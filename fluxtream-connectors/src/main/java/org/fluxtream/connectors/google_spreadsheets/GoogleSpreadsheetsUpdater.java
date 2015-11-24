package org.fluxtream.connectors.google_spreadsheets;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.*;
import com.google.gdata.util.ServiceException;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.dao.JPAFacetDao;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.SettingsManagingUpdater;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.services.JPADaoService;
import org.fluxtream.core.utils.JPAUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * Created by candide on 29/12/14.
 */
@Component
@Updater(prettyName = "Google Spreadsheets", value = 1,
        settings = GoogleSpreadsheetsSettings.class,
        objectTypes = {GoogleSpreadsheetRowFacet.class}, updateStrategyType = Connector.UpdateStrategyType.INCREMENTAL)
public class GoogleSpreadsheetsUpdater extends AbstractUpdater implements SettingsManagingUpdater {

    @Autowired
    GoogleSpreadsheetsHelper helper;

    @Autowired
    JPAFacetDao jpaFacetDao;

    @Autowired
    JPADaoService jpaDaoService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    GoogleSpreadsheetsDao spreadsheetsDao;

    @Override
    public Object getSettingsInstance(long apiKeyId) {
        List<GoogleSpreadsheetsDocumentFacet> userDocs = jpaDaoService.findWithQuery("SELECT doc FROM " + JPAUtils.getEntityName(GoogleSpreadsheetsDocumentFacet.class) +
                        " doc WHERE doc.apiKeyId=?",
                GoogleSpreadsheetsDocumentFacet.class, apiKeyId);
        GoogleSpreadsheetsSettings googleSpreadsheetsSettings = new GoogleSpreadsheetsSettings(userDocs);
        return googleSpreadsheetsSettings;
    }

    public static class ImportSpecs {
        public String spreadsheetId, worksheetId, dateTimeField, dateTimeFormat, timeZone;
        public String columnNames, itemLabel, collectionLabel;
    }

    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ImportSpecs importSpecs = objectMapper.readValue(updateInfo.jsonParams, ImportSpecs.class);
        //TODO: check document doesn't already exist
        if (spreadsheetsDao.isDupe(updateInfo, importSpecs))
            return;
        GoogleSpreadsheetsDocumentFacet documentFacet = new GoogleSpreadsheetsDocumentFacet(importSpecs);
        extractCommonFacetData(documentFacet, updateInfo);
        jpaFacetDao.persist(documentFacet);
        importSpreadsheet(documentFacet, updateInfo);
    }

    private void importSpreadsheet(GoogleSpreadsheetsDocumentFacet documentFacet, UpdateInfo updateInfo) throws UpdateFailedException, IOException, ServiceException {
        ApiKey apiKey = guestService.getApiKey(updateInfo.getGuestId(), Connector.getConnector("google_spreadsheets"));
        GoogleCredential credential = helper.getCredentials(apiKey);
        SpreadsheetService service =
                new SpreadsheetService("Fluxtream");
        service.setProtocolVersion(SpreadsheetService.Versions.V3);
        service.setOAuth2Credentials(credential);
        URL SPREADSHEET_FEED_URL = new URL(
                "https://spreadsheets.google.com/feeds/spreadsheets/private/full");
        SpreadsheetFeed feed = service.getFeed(SPREADSHEET_FEED_URL,
                SpreadsheetFeed.class);
        List<SpreadsheetEntry> spreadsheets = feed.getEntries();
        for (SpreadsheetEntry spreadsheet : spreadsheets) {
            if (spreadsheet.getId().equals(documentFacet.spreadsheetId)) {
                List<WorksheetEntry> worksheets = spreadsheet.getWorksheets();
                for (WorksheetEntry worksheet : worksheets) {
                    if (documentFacet.worksheetId == null || worksheet.getId().equals(documentFacet.worksheetId)) {
                        try {
                            int numberOfRows = importWorksheet(service, worksheet, documentFacet, updateInfo);
                            jdbcTemplate.update("UPDATE Facet_GoogleSpreadsheetDocument SET status=?, numberOfRows=? WHERE id=?",
                                    GoogleSpreadsheetsDocumentFacet.Status.UP.ordinal(),
                                    numberOfRows,
                                    documentFacet.getId());
                        } catch (Throwable t) {
                            jdbcTemplate.update("UPDATE Facet_GoogleSpreadsheetDocument SET status=?, message=?, stackTrace=? WHERE id=?",
                                    GoogleSpreadsheetsDocumentFacet.Status.DOWN.ordinal(),
                                    t.getMessage(),
                                    ExceptionUtils.getStackTrace(t),
                                    documentFacet.getId());
                        }
                    }
                }
            }
        }
    }

    private int importWorksheet(SpreadsheetService service, WorksheetEntry worksheet, GoogleSpreadsheetsDocumentFacet documentFacet, UpdateInfo updateInfo) throws IOException, ServiceException {
        URL cellFeedUrl = worksheet.getCellFeedUrl();
        CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);

        GoogleSpreadsheetRowFacet currentRow = null;
        int currentRowIndex = -1;
        DateTimeFormatter formatter = null;
        boolean isEpochTime = Arrays.asList("epochMillis", "epochSeconds").contains(documentFacet.dateTimeFormat);
        if (!isEpochTime)
            formatter = DateTimeFormat.forPattern(documentFacet.dateTimeFormat);
        DateTimeZone dateTimeZone = documentFacet.timeZone != null ? DateTimeZone.forTimeZone(TimeZone.getTimeZone(documentFacet.timeZone)) : null;
        boolean firstLine = true;
        StringBuilder columnNamesBuilder = new StringBuilder();
        int colIdx = 0;
        for (CellEntry cell : cellFeed.getEntries()) {
            if (cell.getCell().getRow() < 2) {
                String columnName = cell.getCell().getValue();
                if (colIdx > 0) columnNamesBuilder.append(", ");
                columnNamesBuilder.append(columnName);
                colIdx++;
                continue;
            }
            if (firstLine) {
                jdbcTemplate.update("UPDATE Facet_GoogleSpreadsheetDocument SET columnNames=? WHERE id=?", columnNamesBuilder.toString(), documentFacet.getId());
                documentFacet.columnNames = columnNamesBuilder.toString();
                firstLine = false;
            }
            if (currentRowIndex != cell.getCell().getRow()) {
                currentRowIndex = cell.getCell().getRow();
                currentRow = new GoogleSpreadsheetRowFacet();
                extractCommonFacetData(currentRow, updateInfo);
                jpaFacetDao.persist(currentRow);
                jdbcTemplate.execute("UPDATE Facet_GoogleSpreadsheetRow SET document_id=" + documentFacet.getId() + " WHERE id=" + currentRow.getId());
            }
            int dateTimeColIndex = getColumnNumber(documentFacet.dateTimeColumnName, documentFacet.columnNames);
            GoogleSpreadsheetCellFacet cellFacet = new GoogleSpreadsheetCellFacet();
            extractCommonFacetData(cellFacet, updateInfo);
            //TODO: use jdbcTemplate to set the cell's row_id to the current row's id
            cellFacet.isNumeric = cell.getCell().getNumericValue() != null;
            String value = cell.getCell().getValue();
            cellFacet.value = value;
            if (cell.getCell().getCol() == dateTimeColIndex) {
                long time;
                if (isEpochTime) {
                    if (documentFacet.dateTimeFormat.equals("epochMillis")) {
                        time = Long.valueOf(value);
                    } else {
                        time = Long.valueOf(value) * 1000;
                    }
                } else {
                    DateTime dateTime;
                    if (dateTimeZone != null)
                        dateTime = formatter.withZone(dateTimeZone).parseDateTime(value);
                    else
                        dateTime = formatter.parseDateTime(value);
                    time = dateTime.getMillis();
                    if (documentFacet.dateTimeFormat.indexOf(" ")==-1||
                            documentFacet.dateTimeFormat.indexOf("T")==-1) {
                        String date;
                        if (dateTimeZone != null)
                            date = ISODateTimeFormat.date().withZone(dateTimeZone).print(time);
                        else
                            date = ISODateTimeFormat.date().print(time);
                        String sql = "UPDATE Facet_GoogleSpreadsheetRow SET allDayEvent=TRUE, startDate=\""
                                + date + "\", endDate=\"" + date + "\" WHERE id=" + currentRow.getId();
                        jdbcTemplate.execute(sql);
                    }
                }
                cellFacet.start = time;
                cellFacet.end = time;
                jdbcTemplate.execute("UPDATE Facet_GoogleSpreadsheetRow SET start=" + time + ", end=" + time + " WHERE id=" + currentRow.getId());
            }
            jpaFacetDao.persist(cellFacet);
            jdbcTemplate.execute("UPDATE Facet_GoogleSpreadsheetCell SET row_id=" + currentRow.getId() + " WHERE id=" + cellFacet.getId());
        }
        return currentRowIndex;
    }

    public static int getColumnNumber(String column, String columnNames) {
        String[] everyColumnName = StringUtils.split(columnNames, ",");
        int i=1;
        for (String columnName : everyColumnName) {
            if (columnName.trim().equals(column.trim()))
                return i;
            i++;
        }
        return -1;
    }

    @Override
    protected void updateConnectorData(UpdateInfo updateInfo) throws Exception {
    }

    @Override
    public void setDefaultChannelStyles(ApiKey apiKey) {

    }
}
