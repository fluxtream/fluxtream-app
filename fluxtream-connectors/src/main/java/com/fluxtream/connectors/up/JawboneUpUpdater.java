package com.fluxtream.connectors.up;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import com.fluxtream.TimezoneMap;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateFailedException;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.utils.TimespanSegment;
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
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
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
        updateMovesSince(updateInfo, movesLastSyncTime);
        updateSleepSince(updateInfo, sleepLastSyncTime);
    }

    private long getBeginningOfTime() {
        return ISODateTimeFormat.basicDate().withZoneUTC().parseMillis("20100101");
    }

    private void updateMovesSince(final UpdateInfo updateInfo, long lastSyncTime) throws Exception {
        final HttpClient client = env.getHttpClient();
        try {
            // get moves since lastSyncTime
            String url = getBeginningOfTime()==lastSyncTime
                       ? "https://jawbone.com/nudge/api/v.1.0/users/@me/moves?start_time=" + lastSyncTime/1000
                       : "https://jawbone.com/nudge/api/v.1.0/users/@me/moves?start_time=" + getBeginningOfTime()/1000 + "&updated_after=" + lastSyncTime/1000;
            final String movesJson = callJawboneAPI(updateInfo, url);
            createOrUpdateMovesFacets(updateInfo, movesJson);
        }
        finally {
            client.getConnectionManager().shutdown();
        }
    }

    private void updateSleepSince(final UpdateInfo updateInfo, final long lastSyncTime) throws Exception {
        final HttpClient client = env.getHttpClient();
        try {
            // get moves since lastSyncTime
            String url = getBeginningOfTime()==lastSyncTime
                         ? "https://jawbone.com/nudge/api/v.1.0/users/@me/moves?start_time=" + lastSyncTime/1000
                         : "https://jawbone.com/nudge/api/v.1.0/users/@me/moves?start_time=" + getBeginningOfTime()/1000 + "&updated_after=" + lastSyncTime/1000;
            final String sleepsJson = callJawboneAPI(updateInfo, url);
            createOrUpdateSleepFacets(updateInfo, sleepsJson);
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
                    facet.time_created = jsonObject.getLong("time_created");
                    facet.time_completed = jsonObject.getLong("time_completed");
                    facet.time_updated = jsonObject.getLong("time_updated");

                    if (jsonObject.has("title"))
                        facet.title = jsonObject.getString("title");
                    if (jsonObject.has("snapshot_image"))
                        facet.snapshot_image = jsonObject.getString("snapshot_image");

                    String dateString = jsonObject.getString("date");

                    JSONObject details = jsonObject.getJSONObject("details");
                    facet.tz = details.getString("tz");
                    facet.tzs = details.getJSONArray("tzs").toString();
                    TimeZone defaultTimeZone = TimeZone.getTimeZone(facet.tz);
                    TimezoneMap tzMap = getTimeZoneMap(facet.tzs);

                    System.out.println(facet.tzs);

                    final LocalDate localDate = ISODateTimeFormat.basicDate().withZoneUTC().parseLocalDate(dateString);
                    facet.date = ISODateTimeFormat.date().withZoneUTC().print(localDate);
                    if (details.has("distance"))
                        facet.distance = details.getInt("distance");
                    if (details.has("km"))
                        facet.km = details.getDouble("km");
                    if (details.has("steps"))
                        facet.steps = details.getInt("steps");
                    if (details.has("active_time"))
                        facet.active_time = details.getInt("active_time");
                    if (details.has("longest_active"))
                        facet.longest_active = details.getInt("longest_active");
                    if (details.has("inactive_time"))
                        facet.inactive_time = details.getInt("inactive_time");
                    if (details.has("longest_idle"))
                        facet.longest_idle = details.getInt("longest_idle");
                    if (details.has("calories"))
                        facet.calories = details.getDouble("calories");
                    if (details.has("bmr"))
                        facet.bmr = details.getDouble("bmr");
                    if (details.has("bmr_day"))
                        facet.bmr_day = details.getDouble("bmr_day");
                    if (details.has("bg_calories"))
                        facet.bg_calories = details.getDouble("bg_calories");
                    if (details.has("wo_calories"))
                        facet.wo_calories = details.getDouble("wo_calories");
                    if (details.has("wo_time"))
                        facet.wo_time = details.getInt("wo_time");
                    if (details.has("wo_active_time"))
                        facet.wo_active_time = details.getInt("wo_active_time");
                    if (details.has("wo_count"))
                        facet.wo_count = details.getInt("wo_count");
                    if (details.has("wo_longest"))
                        facet.wo_longest = details.getInt("wo_longest");

                    if (details.has("hourly_totals")) {
                        final JSONObject hourlyTotals = details.getJSONObject("hourly_totals");
                        JSONArray names = hourlyTotals.names();
                        for (int i=0; i<names.size(); i++) {
                            String hour_of_day = names.getString(i);
                            long start = getTimeOfDay(hour_of_day, tzMap, defaultTimeZone);
                            JawboneUpMovesHourlyTotals totals = new JawboneUpMovesHourlyTotals();
                            totals.start = start;
                            JSONObject totalsJson = hourlyTotals.getJSONObject(hour_of_day);
                            if (totalsJson.has("distance"))
                                totals.distance = totalsJson.getInt("distance");
                            if (totalsJson.has("calories"))
                                totals.calories = totalsJson.getDouble("calories");
                            if (totalsJson.has("steps"))
                                totals.steps = totalsJson.getInt("steps");
                            if (totalsJson.has("active_time"))
                                totals.active_time = totalsJson.getInt("active_time");
                            if (totalsJson.has("inactive_time"))
                                totals.inactive_time = totalsJson.getInt("inactive_time");
                            if (totalsJson.has("longest_active_time"))
                                totals.longest_active_time = totalsJson.getInt("longest_active_time");
                            if (totalsJson.has("longest_idle_time"))
                                totals.longest_idle_time = totalsJson.getInt("longest_idle_time");
                            facet.addHourlyTotals(totals);
                        }
                    }

                    return facet;
                } catch (Throwable t) {
                    logger.warn("could not import a Jawbone UP moves record: " + t.getMessage());
                    t.printStackTrace();
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

    private long getTimeOfDay(final String hour_of_day, final TimezoneMap tzMap, final TimeZone defaultTimeZone) {
        final String dateString = hour_of_day.substring(0, 8);
        final int millis_since_midnight_offset = Integer.valueOf(hour_of_day.substring(8))*DateTimeConstants.MILLIS_PER_HOUR;
        final TreeSet<TimespanSegment<DateTimeZone>> spans = tzMap.spans;
        for (TimespanSegment<DateTimeZone> span : spans) {
            long day_gmt = ISODateTimeFormat.basicDate().withZone(span.getValue()).parseDateTime(dateString).getMillis();
            long possibleTime = day_gmt + millis_since_midnight_offset;
            if (span.getStart()<possibleTime&&span.getEnd()>possibleTime)
                return possibleTime;
        }
        long day_gmt = ISODateTimeFormat.basicDate().withZone(DateTimeZone.forTimeZone(defaultTimeZone)).parseDateTime(dateString).getMillis();
        return day_gmt + millis_since_midnight_offset;
    }

    TimezoneMap getTimeZoneMap(final String tzs) {
        TimezoneMap map = new TimezoneMap();
        if (tzs==null) return map;
        JSONArray timezoneArray = JSONArray.fromObject(tzs);
        for (int i=0; i<timezoneArray.size(); i++) {
            JSONArray timezoneInfo = timezoneArray.getJSONArray(i);
            long start = timezoneInfo.getLong(0)*1000;
            // allow some padding for the last element in the map
            long end = start + DateTimeConstants.MILLIS_PER_DAY;
            if (timezoneArray.size()>=i+2)
                end = timezoneArray.getJSONArray(i+1).getLong(0)*1000;
            String ID = timezoneInfo.getString(1);
            map.add(start, end, DateTimeZone.forID(ID));
        }
        //add some padding for the first element in the map
        if (map.spans.size()>0) {
            final TimespanSegment<DateTimeZone> first = map.spans.first();
            first.setStart(first.getStart()-DateTimeConstants.MILLIS_PER_DAY);
        }
        return map;
    }

    private void createOrUpdateSleepFacets(final UpdateInfo updateInfo, final String movesJson) {

    }

    private String callJawboneAPI(final UpdateInfo updateInfo, final String url) throws Exception {
        final HttpClient client = env.getHttpClient();
        final long then = System.currentTimeMillis();
        try {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "Bearer " + updateInfo.getContext("accessToken"));
            HttpResponse response = client.execute(get);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String content = responseHandler.handleResponse(response);
                countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, url);
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
