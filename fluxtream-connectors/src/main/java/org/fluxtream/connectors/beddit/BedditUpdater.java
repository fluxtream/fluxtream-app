package org.fluxtream.connectors.beddit;


import com.google.gdata.util.common.base.Pair;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.BasicResponseHandler;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.utils.HttpUtils;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.joda.time.DateTimeZone;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@Component
@Updater(prettyName = "Beddit", value = 352, objectTypes={SleepFacet.class}, bodytrackResponder=BedditBodytrackResponder.class,
        defaultChannels = {"Beddit.sleepStages", "Beddit.snoringEpisodes","Beddit.sleepCycles", "Beddit.heartRate"})
public class BedditUpdater extends AbstractUpdater {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

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

        int statusCode;
        String content = null;
        // support both oauth2- and token -based authorization
        if (guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken")!=null) {
            final String oauthAccessToken = guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken");
            updateInfo.setContext("accessToken", oauthAccessToken);
            try {
                content = callBedditAPI(updateInfo, url);
                statusCode = HttpStatus.SC_OK;
            } catch (UnexpectedHttpResponseCodeException exc) {
                statusCode = exc.getHttpResponseCode();
            }
        } else {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "UserToken " + accessToken);
            HttpClient httpClient = env.getHttpClient();
            if (env.get("development") != null && env.get("development").equals("true"))
                httpClient = HttpUtils.httpClientTrustingAllSSLCerts();
            HttpResponse response = httpClient.execute(get);
            statusCode = response.getStatusLine().getStatusCode();
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            content = responseHandler.handleResponse(response);
        }
        if (statusCode != HttpStatus.SC_OK) {
            throw new UpdateFailedException("Got status code " + statusCode);
        }
        try{
            Double newLatestTime = createOrUpdateFacets(updateInfo,content);
            if (newLatestTime != null && (latestData == null || newLatestTime > latestData)){
                updateLatestFacetTime(updateInfo, newLatestTime);
            }
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, url);
        }
        catch (Exception e){
            countFailedApiCall(updateInfo.apiKey,updateInfo.objectTypes,then,url, ExceptionUtils.getStackTrace(e), statusCode, null);
            e.printStackTrace();
            throw new UpdateFailedException(e.getMessage());
        }


    }

    private String callBedditAPI(final UpdateInfo updateInfo, final String url) throws Exception {
        HttpClient client = env.getHttpClient();
        if (env.get("development") != null && env.get("development").equals("true")) {
//            HttpHost proxy = new HttpHost("127.0.0.1", 8888, "http");
//            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,proxy);
            client = HttpUtils.httpClientTrustingAllSSLCerts();
        }
        final long then = System.currentTimeMillis();
        try {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "UserToken " + updateInfo.getContext("accessToken"));
            HttpResponse response = client.execute(get);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String content = responseHandler.handleResponse(response);
                countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, url);
                return content;
            } else {
                throw new UnexpectedHttpResponseCodeException(statusCode, response.getStatusLine().getReasonPhrase());
            }
        }
        finally {
            client.getConnectionManager().shutdown();
        }
    }

    @Override
    public void setDefaultChannelStyles(ApiKey apiKey) {
        BodyTrackHelper.ChannelStyle channelStyle = new BodyTrackHelper.ChannelStyle();
        channelStyle.timespanStyles = new BodyTrackHelper.MainTimespanStyle();
        channelStyle.timespanStyles.defaultStyle = new BodyTrackHelper.TimespanStyle();
        channelStyle.timespanStyles.defaultStyle.fillColor = "#A3A3A3";
        channelStyle.timespanStyles.defaultStyle.borderColor = "#A3A3A3";
        channelStyle.timespanStyles.defaultStyle.borderWidth = 0;
        channelStyle.timespanStyles.defaultStyle.top = 1.00;
        channelStyle.timespanStyles.defaultStyle.bottom = 0.67;
        channelStyle.timespanStyles.values = new HashMap();

        BodyTrackHelper.TimespanStyle stylePart = new BodyTrackHelper.TimespanStyle();
        stylePart.top = 0.67;
        stylePart.bottom = 1.00;
        stylePart.fillColor = "#A3A3A3";
        stylePart.borderColor = "#A3A3A3";
        channelStyle.timespanStyles.values.put("away",stylePart);

        stylePart = new BodyTrackHelper.TimespanStyle();
        stylePart.top = 0.33;
        stylePart.bottom = 0.67;
        stylePart.fillColor = "#85D3EF";
        stylePart.borderColor = "#85D3EF";
        channelStyle.timespanStyles.values.put("awake",stylePart);

        stylePart = new BodyTrackHelper.TimespanStyle();
        stylePart.top = 0.00;
        stylePart.bottom = 0.33;
        stylePart.fillColor = "#2EA3CE";
        stylePart.borderColor = "#2EA3CE";
        channelStyle.timespanStyles.values.put("asleep",stylePart);

        bodyTrackHelper.setBuiltinDefaultStyle(apiKey.getGuestId(), "beddit", "sleepStages", channelStyle);

        channelStyle = new BodyTrackHelper.ChannelStyle();
        channelStyle.timespanStyles = new BodyTrackHelper.MainTimespanStyle();
        channelStyle.timespanStyles.defaultStyle = new BodyTrackHelper.TimespanStyle();
        channelStyle.timespanStyles.defaultStyle.fillColor = "#33b5e5";
        channelStyle.timespanStyles.defaultStyle.borderColor = "#33b5e5";
        channelStyle.timespanStyles.defaultStyle.borderWidth = 0;
        channelStyle.timespanStyles.defaultStyle.top = 1.00;
        channelStyle.timespanStyles.defaultStyle.bottom = 0.0;
        channelStyle.timespanStyles.values = new HashMap();

        bodyTrackHelper.setBuiltinDefaultStyle(apiKey.getGuestId(), "beddit", "snoringEpisodes", channelStyle);

    }

    private Double createOrUpdateFacets(UpdateInfo updateInfo, String json) throws Exception {
        JSONArray sleepsArray = JSONArray.fromObject(json);
        Double oldestFacetTime = null;
        List<AbstractFacet> newFacets = new LinkedList<AbstractFacet>();
        for (int i = 0; i < sleepsArray.size(); i++){
            SleepFacet newFacet = createOrUpdateFacet(updateInfo, sleepsArray.getJSONObject(i));
            oldestFacetTime = oldestFacetTime == null ? newFacet.updatedTime : Math.max(oldestFacetTime, newFacet.updatedTime);
            newFacets.add(newFacet);
        }
        bodyTrackStorageService.storeApiData(updateInfo.apiKey, newFacets);
        return oldestFacetTime;
    }

    private SleepFacet createOrUpdateFacet(final UpdateInfo updateInfo, final JSONObject sleepObject) throws Exception {
        final DateTimeZone timezone = DateTimeZone.forID(sleepObject.getString("timezone"));

        return apiDataService.createOrReadModifyWrite(SleepFacet.class,new ApiDataService.FacetQuery(
                "e.apiKeyId = ? AND e.date = ?",
                updateInfo.apiKey.getId(), sleepObject.getString("date")),
                new ApiDataService.FacetModifier<SleepFacet>() {
                    @Override
                    public SleepFacet createOrModify(SleepFacet facet, Long apiKeyId) {
                        if (facet == null){
                            facet = new SleepFacet(updateInfo.apiKey.getId());
                            facet.api = updateInfo.apiKey.getConnector().value();
                            facet.date = sleepObject.getString("date");
                        }

                        facet.updatedTime = sleepObject.getDouble("updated");

                        facet.start = (long) sleepObject.getDouble("start_timestamp")*1000;
                        facet.end = (long) sleepObject.getDouble("end_timestamp")*1000;

                        JSONObject propertiesObject = sleepObject.getJSONObject("properties");
                        facet.sleepTimeTarget = propertiesObject.getDouble("sleep_time_target");
                        facet.snoringAmount = propertiesObject.getDouble("total_snoring_episode_duration");
                        if (propertiesObject.has("resting_heart_rate")) {
                            Object o = propertiesObject.get("resting_heart-rate");
                            if (o instanceof Number)
                                facet.restingHeartRate = propertiesObject.getDouble("resting_heart_rate");
                        }
                        facet.respirationRate = propertiesObject.has("average_respiration_rate") ? propertiesObject.getDouble("average_respiration_rate") : null;
                        facet.timeToFallAsleep = propertiesObject.has("sleep_latency") ? propertiesObject.getDouble("sleep_latency") : null;
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

                        JSONArray sleepTags = sleepObject.getJSONArray("tags");
                        List<String> sleepTagsData = new ArrayList<String>();
                        for (int i = 0; i < sleepTags.size(); i++) {
                            sleepTagsData.add(sleepTags.getString(i));
                        }
                        facet.setSleepTags(sleepTagsData);


                        JSONObject timeValueTracksObject = sleepObject.getJSONObject("time_value_tracks");

                        if (timeValueTracksObject.has("sleep_cycles")) {
                            JSONObject sleep_cycles = timeValueTracksObject.getJSONObject("sleep_cycles");
                            if (sleep_cycles!=null) {
                                JSONArray sleepCycles = sleep_cycles.getJSONArray("items");
                                List<Pair<Long, Double>> sleepCycleData = new ArrayList<Pair<Long, Double>>();
                                for (int i = 0; i < sleepCycles.size(); i++) {
                                    sleepCycleData.add(new Pair<Long, Double>((long)sleepCycles.getJSONArray(i).getDouble(0)*1000, sleepCycles.getJSONArray(i).getDouble(1)));
                                }
                                facet.setSleepCycles(sleepCycleData);
                            }
                        }

                        if (timeValueTracksObject.has("heart_rate_curve")) {
                            JSONObject heart_rate_curve = timeValueTracksObject.getJSONObject("heart_rate_curve");
                            if (heart_rate_curve!=null) {
                                JSONArray heartRateCurve = heart_rate_curve.getJSONArray("items");
                                List<Pair<Long,Double>> heartRateCurveData = new ArrayList<Pair<Long,Double>>();
                                for (int i = 0; i < heartRateCurve.size(); i++) {
                                    heartRateCurveData.add(new Pair<Long, Double>((long)heartRateCurve.getJSONArray(i).getDouble(0)*1000, heartRateCurve.getJSONArray(i).getDouble(1)));
                                }
                                facet.setHeartRateCurve(heartRateCurveData);
                            }
                        }

                        if (timeValueTracksObject.has("sleep_stages")) {
                            JSONObject sleep_stages = timeValueTracksObject.getJSONObject("sleep_stages");
                            if (sleep_stages!=null) {
                                JSONArray sleepStages = sleep_stages.getJSONArray("items");
                                List<Pair<Long,Integer>> sleepStagesData = new ArrayList<Pair<Long, Integer>>();
                                for (int i = 0 ; i < sleepStages.size(); i++) {
                                    sleepStagesData.add(new Pair<Long, Integer>((long)sleepStages.getJSONArray(i).getDouble(0)*1000, sleepStages.getJSONArray(i).getInt(1)));
                                }
                                facet.setSleepStages(sleepStagesData);
                            }
                        }

                        if (timeValueTracksObject.has("snoring_episodes")) {
                            JSONObject snoring_episodes = timeValueTracksObject.getJSONObject("snoring_episodes");
                            if (snoring_episodes!=null) {
                                JSONArray snoringEpisodes = snoring_episodes.getJSONArray("items");
                                List<Pair<Long,Double>> snoringEpisodesData = new ArrayList<Pair<Long, Double>>();
                                for (int i = 0; i < snoringEpisodes.size(); i++) {
                                    snoringEpisodesData.add(new Pair<Long,Double>((long)snoringEpisodes.getJSONArray(i).getDouble(0)*1000, snoringEpisodes.getJSONArray(i).getDouble(1)));
                                }
                                facet.setSnoringEpisodes(snoringEpisodesData);
                            }
                        }

                        return facet;
                    }

                },updateInfo.apiKey.getId());


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