package org.fluxtream.connectors.google_spreadsheets;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import org.apache.commons.lang.StringUtils;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Notification;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.NotificationsService;
import org.fluxtream.core.services.SettingsService;
import org.fluxtream.core.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
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
    SettingsService settingsService;

    @Autowired
    NotificationsService notificationsService;

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
        TimeZone tz = null;
        if (StringUtils.isNotEmpty(timeZone))
            tz = TimeZone.getTimeZone(timeZone);
        return Response.ok().build();
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSpreadsheets() throws UpdateFailedException {
        try {
            ApiKey apiKey = guestService.getApiKey(AuthHelper.getGuestId(), Connector.getConnector("google_spreadsheets"));
            GoogleCredential credential = getCredentials(apiKey);
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
            GoogleCredential credential = getCredentials(apiKey);
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
            GoogleCredential credential = getCredentials(apiKey);
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

    private GoogleCredential getCredentials(ApiKey apiKey) throws UpdateFailedException {
        HttpTransport httpTransport = new NetHttpTransport();
        JacksonFactory jsonFactory = new JacksonFactory();
        // Get all the attributes for this connector's oauth token from the stored attributes
        String accessToken = guestService.getApiKeyAttribute(apiKey, "accessToken");
        final String refreshToken = guestService.getApiKeyAttribute(apiKey, "refreshToken");
        final String clientId = guestService.getApiKeyAttribute(apiKey, "google.client.id");
        final String clientSecret = guestService.getApiKeyAttribute(apiKey,"google.client.secret");
        final GoogleCredential.Builder builder = new GoogleCredential.Builder();
        builder.setTransport(httpTransport);
        builder.setJsonFactory(jsonFactory);
        builder.setClientSecrets(clientId, clientSecret);
        GoogleCredential credential = builder.build();
        final Long tokenExpires = Long.valueOf(guestService.getApiKeyAttribute(apiKey, "tokenExpires"));
        credential.setExpirationTimeMilliseconds(tokenExpires);
        credential.setAccessToken(accessToken);
        credential.setRefreshToken(refreshToken);

        try {
            if (tokenExpires<System.currentTimeMillis()) {
                boolean tokenRefreshed = false;

                // Don't worry about checking if we are running on a mirrored test instance.
                // Refreshing tokens independently on both the main server and a mirrored instance
                // seems to work just fine.

                // Try to swap the expired access token for a fresh one.
                tokenRefreshed = credential.refreshToken();

                if(tokenRefreshed) {
                    Long newExpireTime = credential.getExpirationTimeMilliseconds();
                    // Update stored expire time
                    guestService.setApiKeyAttribute(apiKey, "accessToken", credential.getAccessToken());
                    guestService.setApiKeyAttribute(apiKey, "tokenExpires", newExpireTime.toString());
                }
            }
        }
        catch (TokenResponseException e) {
            // Notify the user that the tokens need to be manually renewed
            Connector connector = Connector.getConnector("google_spreadsheets");
            notificationsService.addNamedNotification(apiKey.getGuestId(), Notification.Type.WARNING, connector.statusNotificationName(),
                    "Heads Up. We failed in our attempt to automatically refresh your Google authentication tokens.<br>" +
                            "Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
                            "scroll to the Google Calendar connector, and renew your tokens (look for the <i class=\"icon-resize-small icon-large\"></i> icon)");

            // Record permanent update failure since this connector is never
            // going to succeed
            guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, Utils.stackTrace(e), ApiKey.PermanentFailReason.NEEDS_REAUTH);
            throw new UpdateFailedException("refresh token attempt permanently failed due to a bad token refresh response", e, true, ApiKey.PermanentFailReason.NEEDS_REAUTH);
        }
        catch (IOException e) {
            // Notify the user that the tokens need to be manually renewed
            throw new UpdateFailedException("refresh token attempt failed", e, true, ApiKey.PermanentFailReason.NEEDS_REAUTH);
        }

        return credential;
    }

}
