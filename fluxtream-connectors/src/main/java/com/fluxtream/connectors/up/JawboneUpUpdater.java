package com.fluxtream.connectors.up;

import java.util.ArrayList;
import java.util.List;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.connectors.runkeeper.RunKeeperFitnessActivityFacet;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateFailedException;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.utils.UnexpectedHttpResponseCodeException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.log4j.Logger;
import org.joda.time.LocalDate;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 26/01/14
 * Time: 09:56
 */
@Component
@Updater(prettyName = "Jawbone Up", value = 1999, objectTypes = {LocationFacet.class,
                                                                 JawboneUpMovesFacet.class, JawboneUpSleepFacet.class})
public class JawboneUpUpdater extends AbstractUpdater {

    Logger logger = Logger.getLogger(JawboneUpUpdater.class);

    private static final String MOVES_LAST_SYNC_TIME = "lastSyncTime/moves";
    private static final String SLEEP_LAST_SYNC_TIME = "lastSyncTime/sleep";
    private static final String MOVES_LAST_START_TIME = "lastStartTime/moves";
    private static final String SLEEP_LAST_START_TIME = "lastStartTime/sleep";

    @Autowired
    JPADaoService jpaDaoService;

    @Autowired
    MetadataService metadataService;

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        updateConnectorData(updateInfo);
    }

    @Override
    protected void updateConnectorData(final UpdateInfo updateInfo) throws Exception {
        updateInfo.setContext("accessToken", guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken"));
        long beginningOfTime = getBeginningOfTime();
        Long movesLastSyncTime = beginningOfTime;
        Long sleepLastSyncTime = beginningOfTime;
        final String movesLastSyncTimeAtt = guestService.getApiKeyAttribute(updateInfo.apiKey, MOVES_LAST_SYNC_TIME);
        final String sleepLastSyncTimeAtt = guestService.getApiKeyAttribute(updateInfo.apiKey, SLEEP_LAST_SYNC_TIME);
        if (movesLastSyncTimeAtt !=null)
            movesLastSyncTime = Long.valueOf(movesLastSyncTimeAtt);
        if (sleepLastSyncTimeAtt!=null)
            sleepLastSyncTime = Long.valueOf(sleepLastSyncTimeAtt);
        updateMovesSince(updateInfo, movesLastSyncTime, getLastMoveStartTime(updateInfo));
        updateSleepSince(updateInfo, sleepLastSyncTime, getLastSleepStartTime(updateInfo));
    }

    private long getBeginningOfTime() {
        return ISODateTimeFormat.basicDate().withZoneUTC().parseMillis("20100101");
    }

    private long getLastSleepStartTime(final UpdateInfo updateInfo) {
        final String latestSleep = guestService.getApiKeyAttribute(updateInfo.apiKey, SLEEP_LAST_START_TIME);
        if (latestSleep!=null)
            return Long.valueOf(latestSleep);
        else return getBeginningOfTime();
    }

    private long getLastMoveStartTime(final UpdateInfo updateInfo) {
        final String latestMoves = guestService.getApiKeyAttribute(updateInfo.apiKey, MOVES_LAST_START_TIME);
        if (latestMoves!=null)
            return Long.valueOf(latestMoves);
        else return getBeginningOfTime();
    }

    private void updateMovesSince(final UpdateInfo updateInfo, long lastSyncTime, long lastMovesStartTime) throws Exception {
        final HttpClient client = env.getHttpClient();
        try {
            // get moves since lastSyncTime
            final String movesJson = callJawboneAPI(updateInfo, "https://jawbone.com/nudge/api/v.1.0/users/@me/moves?start_time=" + (lastMovesStartTime / 1000));
            createOrUpdateMovesFacets(updateInfo, movesJson);
            // get moves updated since lastSyncTime
            final long updated_after = lastSyncTime / 1000;
            final String updatedMovesJson = callJawboneAPI(updateInfo, "https://jawbone.com/nudge/api/v.1.0/users/@me/moves?updated_after=" + updated_after);
            guestService.setApiKeyAttribute(updateInfo.apiKey, MOVES_LAST_SYNC_TIME, String.valueOf(System.currentTimeMillis()));
            createOrUpdateMovesFacets(updateInfo, updatedMovesJson);
        }
        finally {
            client.getConnectionManager().shutdown();
        }
    }

    private void updateSleepSince(final UpdateInfo updateInfo, final long lastSyncTime, final long lastSleepStartTime) throws Exception {
        final HttpClient client = env.getHttpClient();
        try {
            // get moves since lastSyncTime
            final String movesJson = callJawboneAPI(updateInfo, "https://jawbone.com/nudge/api/v.1.0/users/@me/sleeps?start_time=" + (lastSleepStartTime / 1000));
            createOrUpdateSleepFacets(updateInfo, movesJson);
            // get moves updated since lastSyncTime
            final String updatedMovesJson = callJawboneAPI(updateInfo, "https://jawbone.com/nudge/api/v.1.0/users/@me/sleeps?updated_after=" + (lastSyncTime /1000));
            guestService.setApiKeyAttribute(updateInfo.apiKey, SLEEP_LAST_SYNC_TIME, String.valueOf(System.currentTimeMillis()));
            createOrUpdateSleepFacets(updateInfo, updatedMovesJson);
        }
        finally {
            client.getConnectionManager().shutdown();
        }
    }

    private void createOrUpdateMovesFacets(final UpdateInfo updateInfo, final String movesJson) throws Exception {
        JSONObject jsonObject = JSONObject.fromObject(movesJson);
        JSONObject data = jsonObject.getJSONObject("data");
        JSONArray items = data.getJSONArray("items");
        for (int i=0; i<items.size(); i++) {
            JSONObject json = items.getJSONObject(i);
            createOrUpdateMovesFacet(json, updateInfo);
        }
    }

    private void createOrUpdateMovesFacet(final JSONObject jsonObject, final UpdateInfo updateInfo) throws Exception {
        final String xid = jsonObject.getString("xid");
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery("e.apiKeyId=? AND e.xid=?",
                                                                                   updateInfo.apiKey.getId(), xid);
        final ApiDataService.FacetModifier<JawboneUpMovesFacet> facetModifier = new ApiDataService.FacetModifier<JawboneUpMovesFacet>() {
            @Override
            public JawboneUpMovesFacet createOrModify(JawboneUpMovesFacet origFacet, final Long apiKeyId) {
                try {
                    JawboneUpMovesFacet facet = origFacet;
                    if (facet==null) {
                        facet = new JawboneUpMovesFacet(updateInfo.apiKey.getId());
                        facet.xid = xid;
                        extractCommonFacetData(facet, updateInfo);
                    }
                    if (jsonObject.has("title"))
                        facet.title = jsonObject.getString("title");
                    facet.time_created = jsonObject.getLong("time_created");
                    facet.time_completed = jsonObject.getLong("time_completed");
                    facet.time_updated = jsonObject.getLong("time_updated");
                    facet.tz = jsonObject.getString("tz");
                    facet.tzs = jsonObject.getJSONArray("tzs").toString();
                    String dateString = jsonObject.getString("date");
                    final LocalDate localDate = ISODateTimeFormat.basicDate().withZoneUTC().parseLocalDate(dateString);
                    facet.date = ISODateTimeFormat.date().withZoneUTC().print(localDate);
                    if (jsonObject.has("snapshot_image"))
                        facet.snapshot_image = jsonObject.getString("snapshot_image");
                    if (jsonObject.has("distance"))
                        facet.distance = jsonObject.getInt("distance");
                    if (jsonObject.has("km"))
                        facet.km = jsonObject.getDouble("km");
                    if (jsonObject.has("steps"))
                        facet.steps = jsonObject.getInt("steps");
                    if (jsonObject.has("active_time"))
                        facet.active_time = jsonObject.getInt("active_time");
                    if (jsonObject.has("longest_active"))
                        facet.longest_active = jsonObject.getInt("longest_active");
                    if (jsonObject.has("inactive_time"))
                        facet.inactive_time = jsonObject.getInt("inactive_time");
                    if (jsonObject.has("longest_idle"))
                        facet.longest_idle = jsonObject.getInt("longest_idle");
                    if (jsonObject.has("calories"))
                        facet.calories = jsonObject.getDouble("calories");
                    if (jsonObject.has("bmr_day"))
                        facet.bmr_day = jsonObject.getDouble("bmr_day");
                    if (jsonObject.has("bg_calories"))
                        facet.bg_calories = jsonObject.getDouble("bg_calories");
                    if (jsonObject.has("wo_calories"))
                        facet.wo_calories = jsonObject.getDouble("wo_calories");
                    if (jsonObject.has("wo_time"))
                        facet.wo_time = jsonObject.getInt("wo_time");
                    if (jsonObject.has("wo_active_time"))
                        facet.wo_active_time = jsonObject.getInt("wo_active_time");
                    if (jsonObject.has("wo_count"))
                        facet.wo_count = jsonObject.getInt("wo_count");
                    if (jsonObject.has("wo_longest"))
                        facet.wo_longest = jsonObject.getInt("wo_longest");

                    if (jsonObject.has("hourly_totals")) {
                        final JSONArray hourlyTotals = jsonObject.getJSONArray("hourly_totals");
                        List<LocationFacet> locationFacets = new ArrayList<LocationFacet>();
                        for (int i=0; i<hourlyTotals.size(); i++) {
                            JSONObject hourlyTotal = hourlyTotals.getJSONObject(i);
                            System.out.println(hourlyTotal.toString());
                        }
                        apiDataService.addGuestLocations(updateInfo.getGuestId(), locationFacets);
                    }

                    return facet;
                } catch (Throwable t) {
                    logger.warn("could not import a Jawbone UP moves record: " + t.getMessage());
                    return origFacet;
                }
            }
        };
        final JawboneUpMovesFacet newFacet = apiDataService.createOrReadModifyWrite(JawboneUpMovesFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
        //if (newFacet!=null) {
        //    List<AbstractFacet> newFacets = new ArrayList<AbstractFacet>();
        //    newFacets.add(newFacet);
        //    bodyTrackStorageService.storeApiData(updateInfo.apiKey.getGuestId(), newFacets);
        //}
    }

    private void createOrUpdateSleepFacets(final UpdateInfo updateInfo, final String movesJson) {

    }

    private String callJawboneAPI(final UpdateInfo updateInfo, final String url) throws Exception {
        final HttpClient client = env.getHttpClient();
        try {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "Bearer " + updateInfo.getContext("accessToken"));
            HttpResponse response = client.execute(get);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String content = responseHandler.handleResponse(response);
                return content;
            } else {
                handleErrors(statusCode, response, "Could not update Jawbone Up Moves Data");
            }
        }
        finally {
            client.getConnectionManager().shutdown();
        }
        throw new RuntimeException("Error calling Jawbone API: this statement should have never been reached");
    }

    private void handleErrors(final int statusCode, final HttpResponse response, final String message) throws Exception {
        // try to extract more information from the response
        try {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String content = responseHandler.handleResponse(response);
            JSONObject errorJson = JSONObject.fromObject(content);
            if (errorJson.has("meta")) {
                JSONObject meta = errorJson.getJSONObject("meta");
                if (meta.has("error_type")) {
                    String details = meta.has("error_detail") ? meta.getString("error_details") : "Unknown Error (no details provided)";
                    throw new UpdateFailedException(message + " - " + details, true);
                }
            }
        } catch (Throwable t) {
            // just ignore any potential problems here
        }
        throw new UnexpectedHttpResponseCodeException(statusCode, message + " - unexpected status code: " + statusCode);
    }
}
