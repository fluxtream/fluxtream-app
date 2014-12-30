package org.fluxtream.connectors.google_spreadsheets;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.util.ServiceException;
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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Created by candide on 30/12/14.
 */
@Path("/spreadsheets")
@Component("RESTGoogleSpreadsheetsController")
@Scope("request")
public class GoogleSpreadsheetsController {

    @Autowired
    GuestService guestService;

    @Autowired
    SettingsService settingsService;

    @Autowired
    NotificationsService notificationsService;

    @GET
    @Path("/")
    @Produces("text/plain")
    public void test() throws UpdateFailedException {
        // https://docs.google.com/a/fluxtream.com/spreadsheets/d/1-qKa-Cs4XR-jQyTjLy_AeWkio7JYYzzYWCBi_6xzGdU/edit?usp=sharing
        try {
            ApiKey apiKey = guestService.getApiKey(AuthHelper.getGuestId(), Connector.getConnector("google_spreadsheets"));
//            HttpTransport httpTransport = new NetHttpTransport();
//            JacksonFactory jsonFactory = new JacksonFactory();
            GoogleCredential credential = getCredentials(apiKey);
//            Drive service = new Drive(httpTransport, jsonFactory, credential);
//            File file = service.files().get("1-qKa-Cs4XR-jQyTjLy_AeWkio7JYYzzYWCBi_6xzGdU").execute();
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
            for (SpreadsheetEntry spreadsheet : spreadsheets) {
                System.out.println(spreadsheet.getTitle().getPlainText());
//                Link spreadsheetLink = spreadsheet.getSpreadsheetLink();
//                System.out.println(spreadsheetLink.getTitleLang());
            }
        } catch (IOException e) {
            System.out.println("An error occured: " + e);
        } catch (ServiceException e) {
            e.printStackTrace();
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
