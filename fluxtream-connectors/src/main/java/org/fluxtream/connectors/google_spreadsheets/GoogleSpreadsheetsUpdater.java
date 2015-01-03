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
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.ApiKey;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
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
@Updater(prettyName = "Google Spreadsheets", value = 1, objectTypes = { GoogleSpreadsheetRowFacet.class }, updateStrategyType = Connector.UpdateStrategyType.INCREMENTAL)
public class GoogleSpreadsheetsUpdater extends AbstractUpdater {

    @Autowired
    GoogleSpreadsheetsHelper helper;

    @Autowired
    JPAFacetDao jpaFacetDao;

    public static class ImportSpecs {
        public String spreadsheetId, worksheetId, dateTimeField, dateTimeFormat, timeZone;
    }

    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ImportSpecs importSpecs = objectMapper.readValue(updateInfo.jsonParams, ImportSpecs.class);
        //TODO: check document doesn't already exist
        GoogleSpreadsheetDocumentFacet documentFacet = new GoogleSpreadsheetDocumentFacet(importSpecs);
        importSpreadsheet(documentFacet);
        jpaFacetDao.persist(documentFacet);
    }

    private void importSpreadsheet(GoogleSpreadsheetDocumentFacet documentFacet) throws UpdateFailedException, IOException, ServiceException {
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
                    if (documentFacet.worksheetId==null||worksheet.getId().equals(documentFacet.worksheetId))
                        importWorksheet(service, worksheet, documentFacet);
                }
            }
        }
    }

    private void importWorksheet(SpreadsheetService service, WorksheetEntry worksheet, GoogleSpreadsheetDocumentFacet documentFacet) throws IOException, ServiceException {
        URL cellFeedUrl = worksheet.getCellFeedUrl();
        CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);

        GoogleSpreadsheetRowFacet currentRow = null;
        int currentRowIndex = -1;
        int dateTimeColIndex = getExcelColumnNumber(documentFacet.dateTimeColumnName);
        DateTimeFormatter formatter = null;
        boolean isEpochTime = Arrays.asList("epochMillis", "epochSeconds").contains(documentFacet.dateTimeFormat);
        if (!isEpochTime)
            formatter = DateTimeFormat.forPattern(documentFacet.dateTimeFormat);
        DateTimeZone dateTimeZone = documentFacet.timeZone!=null?DateTimeZone.forTimeZone(TimeZone.getTimeZone(documentFacet.timeZone)):null;
        for (CellEntry cell : cellFeed.getEntries()) {
            if (currentRowIndex!=cell.getCell().getRow()) {
                currentRowIndex = cell.getCell().getRow();
                currentRow = new GoogleSpreadsheetRowFacet();
                currentRow.document = documentFacet;
                jpaFacetDao.persist(currentRow);
            }
            GoogleSpreadsheetCellFacet cellFacet = new GoogleSpreadsheetCellFacet();
            cellFacet.row = currentRow;
            cellFacet.isNumeric = cell.getCell().getNumericValue()!=null;
            cellFacet.value = cell.getCell().getValue();
            if (cell.getCell().getCol()==dateTimeColIndex) {
                DateTime dateTime;
                if (isEpochTime) {
                    if (documentFacet.dateTimeFormat.equals("epochMillis"))
                        currentRow.start = currentRow.end = Long.valueOf(cell.getCell().getValue());
                    else
                        currentRow.start = currentRow.end = Long.valueOf(cell.getCell().getValue())*1000;
                } else {
                    if (dateTimeZone != null)
                        dateTime = formatter.withZone(dateTimeZone).parseDateTime(cell.getCell().getValue());
                    else
                        dateTime = formatter.parseDateTime(cell.getCell().getValue());
                    currentRow.start = currentRow.end = dateTime.getMillis();
                }
                System.out.println(currentRow.start);
            }
            jpaFacetDao.persist(cellFacet);
            currentRow.cells.add(cellFacet);
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
