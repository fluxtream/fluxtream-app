package org.fluxtream.connectors.sleep_as_android;


import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.gdata.util.common.base.Pair;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Notification;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@Component
@Updater(prettyName = "Sleep_As_Android", value = 351, objectTypes={SleepFacet.class}, bodytrackResponder=SleepAsAndroidBodytrackResponder.class,
         defaultChannels = {"Sleep_As_Android.sleep","Sleep_As_Android.actiGraph","Sleep_As_Android.cycles"})
public class SleepAsAndroidUpdater extends AbstractUpdater {

    @Autowired
    BodyTrackHelper bodyTrackHelper;


    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        updateConnectorData(updateInfo);

    }

    @Override
    protected void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        Long latestData = getLatestFacetTime(updateInfo);
        fetchSleeps(updateInfo, latestData, null);
    }

    @Override
    public void setDefaultChannelStyles(ApiKey apiKey) {
        BodyTrackHelper.ChannelStyle channelStyle = new BodyTrackHelper.ChannelStyle();
        channelStyle.timespanStyles = new BodyTrackHelper.MainTimespanStyle();
        channelStyle.timespanStyles.defaultStyle = new BodyTrackHelper.TimespanStyle();
        channelStyle.timespanStyles.defaultStyle.fillColor = "#33b5e5";
        channelStyle.timespanStyles.defaultStyle.borderColor = "#33b5e5";
        channelStyle.timespanStyles.defaultStyle.borderWidth = 0;
        channelStyle.timespanStyles.defaultStyle.top = 1.00;
        channelStyle.timespanStyles.defaultStyle.bottom = 0.67;
        channelStyle.timespanStyles.values = new HashMap();

        BodyTrackHelper.TimespanStyle stylePart = new BodyTrackHelper.TimespanStyle();
        stylePart.top = 0.67;
        stylePart.bottom = 1.00;
        stylePart.fillColor = "#33b5e5";
        stylePart.borderColor = "#33b5e5";
        channelStyle.timespanStyles.values.put("light",stylePart);

        stylePart = new BodyTrackHelper.TimespanStyle();
        stylePart.top = 0.33;
        stylePart.bottom = 0.67;
        stylePart.fillColor = "#0099cc";
        stylePart.borderColor = "#0099cc";
        channelStyle.timespanStyles.values.put("deep",stylePart);

        stylePart = new BodyTrackHelper.TimespanStyle();
        stylePart.top = 0.00;
        stylePart.bottom = 0.33;
        stylePart.fillColor = "#ff8800";
        stylePart.borderColor = "#ff8800";
        channelStyle.timespanStyles.values.put("rem",stylePart);

        bodyTrackHelper.setBuiltinDefaultStyle(apiKey.getGuestId(), "sleep_as_android", "sleep", channelStyle);
    }

    private void fetchSleeps(UpdateInfo updateInfo, Long latestData, String cursor) throws UpdateFailedException, IOException {
        long then = System.currentTimeMillis();
        GoogleCredential credentials = getCredentials(updateInfo.apiKey);


        String url = "https://sleep-cloud.appspot.com/fetchRecords?actigraph=true&labels=true&tags=true&comments=true";
        if (latestData != null)
            url += "&timestamp=" + latestData;
        if (cursor != null)
            url += "&cursor=" + cursor;

        HttpGet get = new HttpGet(url);
        get.setHeader("Authorization","Bearer " + credentials.getAccessToken());
        get.getMethod();
        HttpResponse response = env.getHttpClient().execute(get);
        String content;
        int statusCode = response.getStatusLine().getStatusCode();
        String statusMessage = response.getStatusLine().getReasonPhrase();
        if (statusCode != HttpStatus.SC_OK) {
            throw new UpdateFailedException("Got status code " + statusCode);
        }
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        content = responseHandler.handleResponse(response);
        try{
            Long newLatestTime = createOrUpdateFacets(updateInfo,content);
            if (newLatestTime != null && (latestData == null || newLatestTime > latestData)){
                updateLatestFacetTime(updateInfo, newLatestTime);
            }
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, url);
            JSONObject topLevelObject = JSONObject.fromObject(content);
            String nextCursor = topLevelObject.has("cursor") ? topLevelObject.getString("cursor") : null;
            if (nextCursor != null) {
                fetchSleeps(updateInfo,latestData,cursor);
            }
        }
        catch (Exception e){
            countFailedApiCall(updateInfo.apiKey,updateInfo.objectTypes,then,url, ExceptionUtils.getStackTrace(e),statusCode,statusMessage);
            e.printStackTrace();
            throw new UpdateFailedException(e.getMessage());
        }

    }

    private Long getLatestFacetTime(UpdateInfo updateInfo){
        ApiKey apiKey = updateInfo.apiKey;

        String updateKeyName = "SleepAsAndroid.latestData";
        String latestDataString = guestService.getApiKeyAttribute(apiKey, updateKeyName);
        return latestDataString == null ? null : Long.parseLong(latestDataString);
    }

    private void updateLatestFacetTime(UpdateInfo updateInfo, long timestamp){
        String updateKeyName = "SleepAsAndroid.latestData";
        guestService.setApiKeyAttribute(updateInfo.apiKey, updateKeyName, Long.toString(timestamp));
    }

    private Long createOrUpdateFacets(UpdateInfo updateInfo, String json) throws Exception {
        JSONObject topLevelObject = JSONObject.fromObject(json);
        JSONArray sleepsArray = topLevelObject.getJSONArray("sleeps");
        Long oldestFacetTime = null;
        List<AbstractFacet> newFacets = new LinkedList<AbstractFacet>();
        for (int i = 0; i < sleepsArray.size(); i++){
            SleepFacet newFacet = createOrUpdateFacet(updateInfo, sleepsArray.getJSONObject(i));
            newFacets.add(newFacet);
            oldestFacetTime = oldestFacetTime == null ? newFacet.end : Math.max(oldestFacetTime, newFacet.end);
        }
        bodyTrackStorageService.storeApiData(updateInfo.apiKey,newFacets);
        return oldestFacetTime;
    }


    private SleepFacet createOrUpdateFacet(final UpdateInfo updateInfo, final JSONObject sleepObject) throws Exception {
        return apiDataService.createOrReadModifyWrite(SleepFacet.class,new ApiDataService.FacetQuery(
                "e.apiKeyId = ? AND e.start = ? AND e.end = ?",
                updateInfo.apiKey.getId(), sleepObject.getLong("fromTime"), sleepObject.getLong("toTime")),
                new ApiDataService.FacetModifier<SleepFacet>() {
                    @Override
                    public SleepFacet createOrModify(SleepFacet facet, Long apiKeyId) {
                        if (facet == null) {
                            facet = new SleepFacet(updateInfo.apiKey.getId());
                            facet.api = updateInfo.apiKey.getConnector().value();
                            facet.start = sleepObject.getLong("fromTime");
                            facet.end = sleepObject.getLong("toTime");
                        }
                        if (sleepObject.has("cycles"))
                            facet.cycles = sleepObject.getInt("cycles");
                        else
                            facet.cycles = 0;
                        if (sleepObject.has("deepSleep"))
                            facet.ratioDeepSleep = sleepObject.getDouble("deepSleep");
                        else
                            facet.ratioDeepSleep = 0;
                        facet.rating = sleepObject.getDouble("rating");
                        facet.noiseLevel = sleepObject.getDouble("noiseLevel");
                        if (sleepObject.has("snroingSeconds")) {
                            facet.snoringSeconds = sleepObject.getInt("snoringSeconds");
                        } else {
                            facet.snoringSeconds = 0;

                        }

                        if (sleepObject.has("comment")) {
                            facet.sleepComment = sleepObject.getString("comment");
                        } else {
                            facet.sleepComment = null;
                        }

                        List<String> sleepTags = new LinkedList<String>();
                        if (sleepObject.has("tags")) {
                            JSONArray tags = sleepObject.getJSONArray("tags");
                            for (int i = 0; i < tags.size(); i++) {
                                sleepTags.add(tags.getString(i));
                            }
                        }

                        facet.setSleepTags(sleepTags);

                        Object actiGraphObject = sleepObject.get("actigraph");
                        if (actiGraphObject != null) {
                            JSONArray actigraphArray = new JSONArray();
                            if (actiGraphObject instanceof JSONObject)
                                actigraphArray.add(actiGraphObject);
                            else if (actiGraphObject instanceof JSONArray)
                                actigraphArray = (JSONArray)actiGraphObject;
                            List<Double> actiGraph = new LinkedList<Double>();
                            for (int i = 0; i < actigraphArray.size(); i++) {
                                actiGraph.add(actigraphArray.getDouble(i));
                            }
                            facet.setActiGraph(actiGraph);
                        }

                        if (sleepObject.has("labels")) {
                            List<Pair<String, Long>> eventLabels = new LinkedList<Pair<String, Long>>();
                            JSONArray labels = sleepObject.getJSONArray("labels");
                            for (int i = 0; i < labels.size(); i++) {
                                JSONObject label = labels.getJSONObject(i);
                                eventLabels.add(new Pair<String, Long>(label.getString("label"), label.getLong("timestamp")));
                            }

                            facet.setEventLabels(eventLabels);
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
