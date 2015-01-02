package org.fluxtream.connectors.google_spreadsheets;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import org.codehaus.jackson.map.ObjectMapper;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by candide on 30/12/14.
 */
@Path("/v1/spreadsheets")
@Component("RESTGoogleSpreadsheetsController")
@Scope("request")
public class GoogleSpreadsheetsController {

    @Autowired
    GuestService guestService;

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Autowired
    GoogleSpreadsheetsHelper helper;

    class SpreadsheetModel {
        public SpreadsheetModel(String title, String id) {
            this.title = title;
            this.id = id;
        }
        public String title;
        public String id;
    }

    class WorksheetModel {
        public WorksheetModel(String title, String id, boolean imported) {
            this.title = title;
            this.id = id;
            this.imported = imported;
        }
        public String title;
        public String id;
        public boolean imported;
    }

    class WorksheetMetadata {
        public int rowCount, colCount;
    }

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addSpreadsheet(@FormParam("spreadsheetId") String spreadsheetId,
                                   @FormParam("worksheetId") String worksheetId,
                                   @FormParam("dateTimeField") String dateTimeField,
                                   @FormParam("dateTimeFormat") String dateTimeFormat,
                                   @FormParam(value="timeZone") String timeZone) throws UpdateFailedException {
        ObjectMapper objectMapper = new ObjectMapper();
        GoogleSpreadsheetsUpdater.ImportSpecs importSpecs = new GoogleSpreadsheetsUpdater.ImportSpecs();
        importSpecs.spreadsheetId = spreadsheetId;
        importSpecs.worksheetId = worksheetId.isEmpty()?null:worksheetId;
        importSpecs.dateTimeField = dateTimeField;
        importSpecs.dateTimeFormat = dateTimeFormat;
        importSpecs.timeZone = timeZone.isEmpty()?null:timeZone;
        Connector connector = Connector.getConnector("google_spreadsheets");
        ApiKey apiKey = guestService.getApiKey(AuthHelper.getGuestId(), connector);
        UpdateInfo updateInfo = UpdateInfo.initialHistoryUpdateInfo(apiKey,
                7);
        try {
            updateInfo.jsonParams = objectMapper.writeValueAsString(importSpecs);
            GoogleSpreadsheetsUpdater updater = (GoogleSpreadsheetsUpdater) connectorUpdateService.getUpdater(connector);
            //TODO: accumulate stats and error in the updateInfo's 'context' map and send that back in the response as JSON
            updater.updateConnectorDataHistory(updateInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Response.ok().build();
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSpreadsheets() throws UpdateFailedException {
        try {
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
            if (spreadsheets.isEmpty()) {
                // TODO: There were no spreadsheets, act accordingly.
            }
            Collections.sort(spreadsheets, new Comparator<SpreadsheetEntry>() {
                @Override
                public int compare(SpreadsheetEntry o1, SpreadsheetEntry o2) {
                    return o1.getTitle().getPlainText().compareTo(o2.getTitle().getPlainText());
                }
            });
            List<SpreadsheetModel> models = new ArrayList<SpreadsheetModel>();
            for (SpreadsheetEntry spreadsheet : spreadsheets) {
                SpreadsheetModel model = new SpreadsheetModel(spreadsheet.getTitle().getPlainText(), spreadsheet.getId());
                models.add(model);
            }
            return Response.ok().entity(models).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/worksheets")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorksheets(@QueryParam("spreadsheetId") String spreadsheetId) throws UpdateFailedException {
        try {
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
            List<WorksheetModel> worksheetModels = new ArrayList<WorksheetModel>();
            for (SpreadsheetEntry spreadsheet : spreadsheets) {
                if (spreadsheet.getId().equals(spreadsheetId)) {
                    List<WorksheetEntry> worksheets = spreadsheet.getWorksheets();
                    for (WorksheetEntry worksheet : worksheets) {
                        WorksheetModel worksheetModel = new WorksheetModel(worksheet.getTitle().getPlainText(), worksheet.getId(), false);
                        worksheetModels.add(worksheetModel);
                    }
                }
            }
            return Response.ok().entity(worksheetModels).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/worksheet")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorksheetMetadata(@QueryParam("spreadsheetId") String spreadsheetId,
                                         @QueryParam("worksheetId") String worksheetId) throws UpdateFailedException {
        try {
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
            WorksheetMetadata worksheetMetadata = new WorksheetMetadata();
            for (SpreadsheetEntry spreadsheet : spreadsheets) {
                if (spreadsheet.getId().equals(spreadsheetId)) {
                    List<WorksheetEntry> worksheets = spreadsheet.getWorksheets();
                    for (WorksheetEntry worksheet : worksheets) {
                        if (worksheet.getId().equals(worksheetId)) {
                            worksheetMetadata.rowCount = worksheet.getRowCount();
                            worksheetMetadata.colCount = worksheet.getColCount();
                        }
                    }
                }
            }
            return Response.ok().entity(worksheetMetadata).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

}
