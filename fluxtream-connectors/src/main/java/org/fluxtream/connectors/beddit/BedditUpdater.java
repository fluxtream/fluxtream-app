package org.fluxtream.connectors.beddit;


import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
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
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Notification;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.utils.Utils;
import org.joda.time.DateTimeZone;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Updater(prettyName = "Beddit", value = 352, objectTypes={SleepFacet.class})
public class BedditUpdater extends AbstractUpdater {

    //note: all timestamps but the updated timestamp are in local time, use the provided timezone ID to deal with this


    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        updateConnectorData(updateInfo);

    }

    @Override
    protected void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        long userId = Long.parseLong(guestService.getApiKeyAttribute(updateInfo.apiKey, "userid"));
        String accessToken = guestService.getApiKeyAttribute(updateInfo.apiKey, "access_token");

        long then = System.currentTimeMillis();
        Double latestData = getLatestFacetTime(updateInfo);


        String url = "https://cloudapi.beddit.com/api/v1/user/" + userId + "/sleep";
        if (latestData != null){
            url +=  "?updated_after=" + latestData;
        }

        HttpGet get = new HttpGet(url);
        get.setHeader("Authorization","UserToken " + accessToken);
        HttpResponse response = env.getHttpClient().execute(get);
        int statusCode = response.getStatusLine().getStatusCode();
        String statusMessage = response.getStatusLine().getReasonPhrase();
        if (statusCode != HttpStatus.SC_OK) {
            throw new UpdateFailedException("Got status code " + statusCode + " - " + statusMessage);
        }
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String content = responseHandler.handleResponse(response);
        try{
            Double newLatestTime = createOrUpdateFacets(updateInfo,content);
            if (newLatestTime != null && (latestData == null || newLatestTime > latestData)){
                updateLatestFacetTime(updateInfo, newLatestTime);
            }
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, url);
        }
        catch (Exception e){
            countFailedApiCall(updateInfo.apiKey,updateInfo.objectTypes,then,url, ExceptionUtils.getStackTrace(e),statusCode,statusMessage);
            e.printStackTrace();
            throw new UpdateFailedException(e.getMessage());
        }


    }

    private Double createOrUpdateFacets(UpdateInfo updateInfo, String json) throws Exception {
        JSONArray sleepsArray = JSONArray.fromObject(json);
        Double oldestFacetTime = null;
        for (int i = 0; i < sleepsArray.size(); i++){
            SleepFacet newFacet = createOrUpdateFacet(updateInfo, sleepsArray.getJSONObject(i));
            oldestFacetTime = oldestFacetTime == null ? newFacet.updatedTime : Math.max(oldestFacetTime, newFacet.updatedTime);
        }
        return oldestFacetTime;
    }

    private SleepFacet createOrUpdateFacet(final UpdateInfo updateInfo, final JSONObject sleepObject) throws Exception {
        final DateTimeZone timezone = DateTimeZone.forID(sleepObject.getString("timezone"));
        final long facetStart = getUTCMillis(sleepObject.getDouble("start_timestamp"),timezone);
        final long facetEnd =  getUTCMillis(sleepObject.getDouble("end_timestamp"),timezone);

        return apiDataService.createOrReadModifyWrite(SleepFacet.class,new ApiDataService.FacetQuery(
                "e.apiKeyId = ? AND e.start = ? AND e.end = ?",
                updateInfo.apiKey.getId(), facetStart, facetEnd),
                new ApiDataService.FacetModifier<SleepFacet>() {
                    @Override
                    public SleepFacet createOrModify(SleepFacet facet, Long apiKeyId) {
                        if (facet == null){
                            facet = new SleepFacet(updateInfo.apiKey.getId());
                            facet.api = updateInfo.apiKey.getConnector().value();
                            facet.start = facetStart;
                            facet.end = facetEnd;
                        }

                        facet.updatedTime = sleepObject.getDouble("updated");

                        JSONObject propertiesObject = sleepObject.getJSONObject("properties");
                        facet.sleepTimeTarget = propertiesObject.getDouble("sleep_time_target");
                        facet.snoringAmount = propertiesObject.getDouble("total_snoring_episode_duration");
                        facet.restingHeartRate = propertiesObject.getDouble("resting_heart_rate");
                        facet.respirationRate = propertiesObject.getDouble("average_respiration_rate");
                        facet.timeToFallAsleep = propertiesObject.getDouble("sleep_latency");
                        facet.awayCount = (int) propertiesObject.getDouble("away_episode_count");
                        facet.totalAwayTime = propertiesObject.getDouble("stage_duration_A");
                        facet.totalSleepTime = propertiesObject.getDouble("stage_duration_S");
                        facet.totalWakeTime = propertiesObject.getDouble("stage_duration_W");
                        facet.totalTimeNoSignal = propertiesObject.getDouble("stage_duration_G");

                        facet.scoreBedExits = propertiesObject.getDouble("score_bed_exits");
                        facet.scoreSleepAmount = propertiesObject.getDouble("score_amount_of_sleep");
                        facet.scoreSnoring = propertiesObject.getDouble("score_snoring");
                        facet.scoreFallAsleepTime = propertiesObject.getDouble("score_sleep_latency");
                        facet.scoreSleepEfficiency = propertiesObject.getDouble("score_sleep_efficiency");
                        facet.scoreAwakenings = propertiesObject.getDouble("score_awakenings");

                        return facet;
                    }

                },updateInfo.apiKey.getId());


    }

    private long getUTCMillis(double localSeconds, DateTimeZone timeZone){
        return timeZone.convertLocalToUTC((long) (localSeconds * 1000),true);
    }


    private Double getLatestFacetTime(UpdateInfo updateInfo){
        ApiKey apiKey = updateInfo.apiKey;

        String updateKeyName = "SleepAsAndroid.latestData";
        String latestDataString = guestService.getApiKeyAttribute(apiKey, updateKeyName);
        return latestDataString == null ? null : Double.parseDouble(latestDataString);
    }

    private void updateLatestFacetTime(UpdateInfo updateInfo, double timestamp){
        String updateKeyName = "SleepAsAndroid.latestData";
        guestService.setApiKeyAttribute(updateInfo.apiKey, updateKeyName, Double.toString(timestamp));
    }
}