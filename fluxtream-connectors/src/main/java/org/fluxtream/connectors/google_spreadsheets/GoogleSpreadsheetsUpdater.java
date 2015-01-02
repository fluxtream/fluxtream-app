package org.fluxtream.connectors.google_spreadsheets;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.*;
import com.google.gdata.util.ServiceException;
import org.codehaus.jackson.map.ObjectMapper;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.ApiKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Created by candide on 29/12/14.
 */
@Component
@Updater(prettyName = "Google Spreadsheets", value = 1, objectTypes = { GoogleSpreadsheetRowFacet.class }, updateStrategyType = Connector.UpdateStrategyType.INCREMENTAL)
public class GoogleSpreadsheetsUpdater extends AbstractUpdater {

    @Autowired
    GoogleSpreadsheetsHelper helper;

    @PersistenceContext
    EntityManager em;

    public static class ImportSpecs {
        public String spreadsheetId, worksheetId, dateTimeField, dateTimeFormat, timeZone;
    }

    @Override
    @Transactional(readOnly=false)
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ImportSpecs importSpecs = objectMapper.readValue(updateInfo.jsonParams, ImportSpecs.class);
        //TODO: check document doesn't already exist
        GoogleSpreadsheetDocumentFacet documentFacet = new GoogleSpreadsheetDocumentFacet(importSpecs);
        importSpreadsheet(documentFacet);
        em.persist(documentFacet);
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

        // Iterate through each cell, printing its value.
        GoogleSpreadsheetRowFacet currentRow = null;
        int currentRowIndex = -1;
        int dateTimeColIndex = getExcelColumnNumber(documentFacet.dateTimeColumnName);
        for (CellEntry cell : cellFeed.getEntries()) {
            if (currentRowIndex!=cell.getCell().getRow()) {
                currentRow = new GoogleSpreadsheetRowFacet();
                em.persist(currentRow);
            }
            System.out.println(cell.getCell().getRow() + "/" + cell.getCell().getCol() + ": " + cell.getCell().getValue());
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
