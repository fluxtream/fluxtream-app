package org.fluxtream.connectors.google_spreadsheets;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.plexus.util.ExceptionUtils;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.JPADaoService;
import org.fluxtream.core.utils.JPAUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.*;

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

    @Autowired
    JPADaoService daoService;

    @Autowired
    GoogleSpreadsheetsDao spreadsheetsDao;

    @Autowired
    @Qualifier("AsyncWorker")
    ThreadPoolTaskExecutor executor;

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
        public String[] columnNames;
    }

    @POST
    @Path("/")
    public Response addSpreadsheet(@FormParam("spreadsheetId") String spreadsheetId,
                                   @FormParam("worksheetId") String worksheetId,
                                   @FormParam("collectionLabel") String collectionLabel,
                                   @FormParam("itemLabel") String itemLabel,
                                   @FormParam("dateTimeField") String dateTimeField,
                                   @FormParam("dateTimeFormat") String dateTimeFormat,
                                   @FormParam(value="timeZone") String timeZone) throws UpdateFailedException {
        final GoogleSpreadsheetsUpdater.ImportSpecs importSpecs = new GoogleSpreadsheetsUpdater.ImportSpecs();
        List<String> blankLabels = new ArrayList<String>();
        if (StringUtils.isBlank(collectionLabel)) blankLabels.add("collectionLabel");
        if (StringUtils.isBlank(itemLabel)) blankLabels.add("itemLabel");
        if (blankLabels.size()>0) {
            JSONObject errors = new JSONObject();
            JSONArray missing = new JSONArray();
            for (String blankLabel : blankLabels)
                missing.add(blankLabel);
            errors.accumulate("missing", missing);
            return Response.status(400).entity(errors.toString()).build();
        }
        importSpecs.spreadsheetId = spreadsheetId;
        importSpecs.worksheetId = worksheetId.isEmpty()?null:worksheetId;
        importSpecs.itemLabel = itemLabel;
        importSpecs.collectionLabel = collectionLabel;
        importSpecs.dateTimeField = dateTimeField;
        importSpecs.dateTimeFormat = dateTimeFormat;
        importSpecs.timeZone = timeZone.isEmpty()?null:timeZone;
        final Connector connector = Connector.getConnector("google_spreadsheets");
        ApiKey apiKey = guestService.getApiKey(AuthHelper.getGuestId(), connector);

        final UpdateInfo updateInfo = UpdateInfo.initialHistoryUpdateInfo(apiKey,
                7);

        if (spreadsheetsDao.isDupe(updateInfo, importSpecs)) {
            JSONObject errors = new JSONObject();
            errors.accumulate("other", "You have already imported this spreadsheet");
            return Response.status(400).entity(errors.toString()).build();
        }

        executor.execute(new Runnable() {
            public void run() {
                try {
                    final ObjectMapper objectMapper = new ObjectMapper();
                    updateInfo.jsonParams = objectMapper.writeValueAsString(importSpecs);
                    GoogleSpreadsheetsUpdater updater = (GoogleSpreadsheetsUpdater) connectorUpdateService.getUpdater(connector);
                    updater.updateConnectorDataHistory(updateInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
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
                        // get first row items and make them into column names
                        URL cellFeedUrl = worksheet.getCellFeedUrl();
                        CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);
                        worksheetMetadata.columnNames = new String[worksheet.getColCount()];
                        Iterator<CellEntry> iterator = cellFeed.getEntries().iterator();
                        for (CellEntry cell = iterator.next(); iterator.hasNext()&&cell.getCell().getRow()==1; cell = iterator.next()) {
                            worksheetMetadata.columnNames[cell.getCell().getCol()-1] = cell.getCell().getValue();
                        }
                    }
                }
            }
            return Response.ok().entity(worksheetMetadata).build();
        } catch (Exception e) {
            return Response.serverError().entity(ExceptionUtils.getStackTrace(e)).build();
        }
    }

    @DELETE
    @Path("/document/{documentId}")
    public Response removeDocument(@PathParam("documentId") long documentId) {
        Response x = checkDocument(documentId);
        spreadsheetsDao.removeDocument(documentId);
        return x;
    }

    @PUT
    @Path("/document/{documentId}/collectionLabel")
    public Response setCollectionLabel(@PathParam("documentId") long documentId,
                                       @FormParam("label") String collectionLabel) {
        return updateLabel(documentId, "collection", collectionLabel);
    }

    @PUT
    @Path("/document/{documentId}/itemLabel")
    public Response setItemLabel(@PathParam("documentId") long documentId,
                                 @FormParam("label") String itemLabel) {
        return updateLabel(documentId, "item", itemLabel);
    }

    private Response updateLabel(long documentId, String labelName, String itemLabel) {
        Response x = checkDocument(documentId);
        daoService.execute("UPDATE Facet_GoogleSpreadsheetDocument SET " + labelName + "Label=? WHERE id=?", itemLabel, documentId);
        return x;
    }

    private Response checkDocument(long documentId) {
        String entityName = JPAUtils.getEntityName(GoogleSpreadsheetsDocumentFacet.class);
        List<GoogleSpreadsheetsDocumentFacet> documents = daoService.findWithQuery("SELECT doc FROM " + entityName + " doc WHERE doc.id=?",
                GoogleSpreadsheetsDocumentFacet.class, documentId);
        if (documents.size()==0) return Response.status(404).build();
        GoogleSpreadsheetsDocumentFacet document = documents.get(0);
        if (AuthHelper.getGuestId()!=document.guestId)
            return Response.status(403).build();
        return Response.ok().build();
    }

}
