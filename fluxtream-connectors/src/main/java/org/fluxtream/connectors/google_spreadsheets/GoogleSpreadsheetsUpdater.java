package org.fluxtream.connectors.google_spreadsheets;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.*;
import com.google.gdata.util.ServiceException;
import org.codehaus.jackson.map.ObjectMapper;
import org.fluxtream.core.auth.AuthHelper;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
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
        GoogleSpreadsheetsDocumentFacet documentFacet = new GoogleSpreadsheetsDocumentFacet(importSpecs);
        documentFacet.columnNames = importSpecs.columnNames;
        documentFacet.collectionLabel = importSpecs.collectionLabel;
        documentFacet.itemLabel = importSpecs.itemLabel;
        extractCommonFacetData(documentFacet, updateInfo);
        jpaFacetDao.persist(documentFacet);
        importSpreadsheet(documentFacet, updateInfo);
    }

    private void importSpreadsheet(GoogleSpreadsheetsDocumentFacet documentFacet, UpdateInfo updateInfo) throws UpdateFailedException, IOException, ServiceException {
        ApiKey apiKey = guestService.getApiKey(AuthHelper.getGuestId(), Connector.getConnector("google_spreadsheets"));
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
                    if (documentFacet.worksheetId == null || worksheet.getId().equals(documentFacet.worksheetId))
                        importWorksheet(service, worksheet, documentFacet, updateInfo);
                }
            }
        }
    }

    private void importWorksheet(SpreadsheetService service, WorksheetEntry worksheet, GoogleSpreadsheetsDocumentFacet documentFacet, UpdateInfo updateInfo) throws IOException, ServiceException {
        URL cellFeedUrl = worksheet.getCellFeedUrl();
        CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);

        GoogleSpreadsheetRowFacet currentRow = null;
        int currentRowIndex = -1;
        int dateTimeColIndex = getExcelColumnNumber(documentFacet.dateTimeColumnName);
        DateTimeFormatter formatter = null;
        boolean isEpochTime = Arrays.asList("epochMillis", "epochSeconds").contains(documentFacet.dateTimeFormat);
        if (!isEpochTime)
            formatter = DateTimeFormat.forPattern(documentFacet.dateTimeFormat);
        DateTimeZone dateTimeZone = documentFacet.timeZone != null ? DateTimeZone.forTimeZone(TimeZone.getTimeZone(documentFacet.timeZone)) : null;
        boolean firstLine = true;
        StringBuilder sb = new StringBuilder(); int colIdx = 0;
        for (CellEntry cell : cellFeed.getEntries()) {
            if (cell.getCell().getRow() < 2) {
                String columnName = cell.getCell().getValue();
                if (colIdx>0) sb.append(", ");
                sb.append(columnName);
                colIdx++;
                continue;
            }
            if (firstLine) {
                jdbcTemplate.update("UPDATE Facet_GoogleSpreadsheetDocument SET columnNames=? WHERE id=?", sb.toString(), documentFacet.getId());
                firstLine = false;
            }
            if (currentRowIndex != cell.getCell().getRow()) {
                currentRowIndex = cell.getCell().getRow();
                currentRow = new GoogleSpreadsheetRowFacet();
                extractCommonFacetData(currentRow, updateInfo);
                jpaFacetDao.persist(currentRow);
                jdbcTemplate.execute("UPDATE Facet_GoogleSpreadsheetRow SET document_id=" + documentFacet.getId() + " WHERE id=" + currentRow.getId());
            }
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
                }
                cellFacet.start = time;
                cellFacet.end = time;
                jdbcTemplate.execute("UPDATE Facet_GoogleSpreadsheetRow SET start=" + time + ", end=" + time + " WHERE id=" + currentRow.getId());
            }
            jpaFacetDao.persist(cellFacet);
            jdbcTemplate.execute("UPDATE Facet_GoogleSpreadsheetCell SET row_id=" + currentRow.getId() + " WHERE id=" + cellFacet.getId());
        }

    }

    public static int getExcelColumnNumber(String column) {
        int result = 0;
        for (int i = 0; i < column.length(); i++) {
            result *= 26;
            result += column.charAt(i) - 'A' + 1;
        }
        return result;
    }

    @Override
    protected void updateConnectorData(UpdateInfo updateInfo) throws Exception {

    }

    @Override
    public void setDefaultChannelStyles(ApiKey apiKey) {

    }
}
