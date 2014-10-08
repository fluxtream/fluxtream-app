package org.fluxtream.connectors.sleep_as_android;


import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.gdata.util.common.base.Pair;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Notification;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.utils.Utils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;

@Component
@Updater(prettyName = "Sleep_As_Android", value = 351, objectTypes={SleepFacet.class})
public class SleepAsAndroidUpdater extends AbstractUpdater {


    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        updateConnectorData(updateInfo);

    }

    @Override
    protected void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        GoogleCredential credentials = getCredentials(updateInfo.apiKey);
        HttpGet get = new HttpGet("https://sleep-cloud.appspot.com/fetchRecords?actigraph=true&labels=true&tags=true");
        get.setHeader("Authorization","Bearer " + credentials.getAccessToken());
        get.getMethod();
        HttpResponse response = env.getHttpClient().execute(get);
        String content;
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            throw new UpdateFailedException("Got status code " + statusCode);
        }
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        content = responseHandler.handleResponse(response);
        try{
            createOrUpdateFacets(updateInfo,content);
        }
        catch (Exception e){
            e.printStackTrace();
            throw new UpdateFailedException(e.getMessage());
        }

    }

    private long createOrUpdateFacets(UpdateInfo updateInfo, String json) throws Exception {
        JSONObject topLevelObject = JSONObject.fromObject(json);
        JSONArray sleepsArray = topLevelObject.getJSONArray("sleeps");
        long oldestFacetTime = Long.MIN_VALUE;
        for (int i = 0; i < sleepsArray.size(); i++){
            SleepFacet newFacet = createOrUpdateFacet(updateInfo, sleepsArray.getJSONObject(i));
            oldestFacetTime = Math.max(oldestFacetTime, newFacet.end);
        }
        return oldestFacetTime;
    }


    private SleepFacet createOrUpdateFacet(final UpdateInfo updateInfo, final JSONObject sleepObject) throws Exception {
        return apiDataService.createOrReadModifyWrite(SleepFacet.class,new ApiDataService.FacetQuery(
                "e.apiKeyId = ? AND e.start = ? AND e.end = ?",
                updateInfo.apiKey.getId(), sleepObject.getLong("fromTime"), sleepObject.getLong("toTime")),
                new ApiDataService.FacetModifier<SleepFacet>() {
                    @Override
                    public SleepFacet createOrModify(SleepFacet facet, Long apiKeyId) {
                        if (facet == null){
                            facet = new SleepFacet(updateInfo.apiKey.getId());
                            facet.api = updateInfo.apiKey.getConnector().value();
                            facet.start = sleepObject.getLong("fromTime");
                            facet.end = sleepObject.getLong("toTime");
                        }
                        facet.cycles = sleepObject.getInt("cycles");
                        facet.ratioDeepSleep = sleepObject.getDouble("deepSleep");
                        facet.rating = sleepObject.getDouble("rating");
                        facet.noiseLevel = sleepObject.getDouble("noiseLevel");
                        if (sleepObject.has("snroingSeconds")) {
                            facet.snoringSeconds = sleepObject.getInt("snoringSeconds");
                        }
                        else {
                            facet.snoringSeconds = null;

                        }

                        facet.sleepTags = new ArrayList<String>();
                        if (sleepObject.has("tags")) {
                            JSONArray tags = sleepObject.getJSONArray("tags");
                            for (int i = 0; i < tags.size(); i++) {
                                facet.sleepTags.add(tags.getString(i));
                            }
                        }

                        facet.actiGraph = new ArrayList<Double>();
                        JSONArray actiGraph = sleepObject.getJSONArray("actigraph");
                        for (int i = 0; i < actiGraph.size(); i++) {
                            facet.actiGraph.add(actiGraph.getDouble(i));
                        }

                        facet.eventLabels = new ArrayList<Pair<String,Long>>();
                        JSONArray labels = sleepObject.getJSONArray("labels");
                        for (int i = 0; i < labels.size(); i++) {
                            JSONObject label = labels.getJSONObject(i);
                            facet.eventLabels.add(new Pair<String,Long>(label.getString("label"), label.getLong("timestamp")));
                        }
                        return facet;
                    }

                },updateInfo.apiKey.getId());


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
            notificationsService.addNamedNotification(apiKey.getGuestId(), Notification.Type.WARNING, connector().statusNotificationName(),
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
