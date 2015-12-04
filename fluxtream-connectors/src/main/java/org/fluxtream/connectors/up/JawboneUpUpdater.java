package org.fluxtream.connectors.up;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.log4j.Logger;
import org.fluxtream.core.TimezoneMap;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.location.LocationFacet;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.AuthExpiredException;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ChannelMapping;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.JPADaoService;
import org.fluxtream.core.services.MetadataService;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.utils.HttpUtils;
import org.fluxtream.core.utils.TimespanSegment;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * User: candide
 * Date: 26/01/14
 * Time: 09:56
 */
@Component
@Updater(prettyName = "Jawbone_UP", value = 1999, objectTypes = {LocationFacet.class, JawboneUpMovesFacet.class,
                                                                 JawboneUpSleepFacet.class, JawboneUpMealFacet.class,
                                                                 JawboneUpServingFacet.class, JawboneUpWorkoutFacet.class},
         defaultChannels = {"Jawbone_UP.intensity", "Jawbone_UP.sleep"},
         deviceNickname = "Jawbone_UP",
         deleteOrder= {1, 2, 4, 8, 32, 16}, bodytrackResponder = JawboneUpBodytrackResponder.class)
public class JawboneUpUpdater extends AbstractUpdater {

    Logger logger = Logger.getLogger(JawboneUpUpdater.class);

    static final Map<Integer,String> endpointDict = new java.util.Hashtable<Integer,String>();
    static {
        endpointDict.put(ObjectType.getObjectTypeValue(JawboneUpMovesFacet.class),
                         "https://jawbone.com/nudge/api/v.1.0/users/@me/moves");
        endpointDict.put(ObjectType.getObjectTypeValue(JawboneUpSleepFacet.class),
                         "https://jawbone.com/nudge/api/v.1.0/users/@me/sleeps");
        endpointDict.put(ObjectType.getObjectTypeValue(JawboneUpMealFacet.class),
                         "https://jawbone.com/nudge/api/v.1.0/users/@me/meals");
        endpointDict.put(ObjectType.getObjectTypeValue(JawboneUpWorkoutFacet.class),
                         "https://jawbone.com/nudge/api/v.1.0/users/@me/workouts");
    }

    private static final String MOVES_LAST_SYNC_TIME = "lastSyncTime/moves";
    private static final String SLEEPS_LAST_SYNC_TIME = "lastSyncTime/sleeps";
    private static final String MEALS_LAST_SYNC_TIME = "lastSyncTime/meals";
    private static final String WORKOUTS_LAST_SYNC_TIME = "lastSyncTime/workouts";

    @Autowired
    JPADaoService jpaDaoService;

    @Autowired
    MetadataService metadataService;

    @Autowired
    @Qualifier("AsyncWorker")
    ThreadPoolTaskExecutor executor;

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {

        getUserXid(updateInfo);

        guestService.removeApiKeyAttribute(updateInfo.apiKey.getId(), MOVES_LAST_SYNC_TIME);
        guestService.removeApiKeyAttribute(updateInfo.apiKey.getId(), SLEEPS_LAST_SYNC_TIME);
        guestService.removeApiKeyAttribute(updateInfo.apiKey.getId(), MEALS_LAST_SYNC_TIME);
        guestService.removeApiKeyAttribute(updateInfo.apiKey.getId(), WORKOUTS_LAST_SYNC_TIME);
        updateConnectorData(updateInfo);
    }

    @Override
    public void setDefaultChannelStyles(ApiKey apiKey) {
        BodyTrackHelper.ChannelStyle channelStyle = new BodyTrackHelper.ChannelStyle();
        channelStyle.timespanStyles = new BodyTrackHelper.MainTimespanStyle();
        channelStyle.timespanStyles.defaultStyle = new BodyTrackHelper.TimespanStyle();
        channelStyle.timespanStyles.defaultStyle.fillColor = "#fff";
        channelStyle.timespanStyles.defaultStyle.borderColor = "#fff";
        channelStyle.timespanStyles.defaultStyle.borderWidth = 2;
        channelStyle.timespanStyles.defaultStyle.top = 0.0;
        channelStyle.timespanStyles.defaultStyle.bottom = 1.0;
        channelStyle.timespanStyles.values = new HashMap<String, BodyTrackHelper.TimespanStyle>();

        BodyTrackHelper.TimespanStyle stylePart = new BodyTrackHelper.TimespanStyle();
        stylePart.top = .0;
        stylePart.bottom = 0.9;
        stylePart.fillColor = "#1196ef";
        stylePart.borderColor = "#1196ef";
        channelStyle.timespanStyles.values.put("deep",stylePart);

        stylePart = new BodyTrackHelper.TimespanStyle();
        stylePart.top = .0;
        stylePart.bottom = 0.6;
        stylePart.fillColor = "#00d2ff";
        stylePart.borderColor = "#00d2ff";
        channelStyle.timespanStyles.values.put("light",stylePart);

        stylePart = new BodyTrackHelper.TimespanStyle();
        stylePart.top = .0;
        stylePart.bottom = 0.1;
        stylePart.fillColor = "#f87d04";
        stylePart.borderColor = "#f87d04";
        channelStyle.timespanStyles.values.put("wake",stylePart);

        bodyTrackHelper.setBuiltinDefaultStyle(apiKey.getGuestId(), apiKey.getConnector().getName(), "sleep", channelStyle);
    }

    @Override
    protected void updateConnectorData(final UpdateInfo updateInfo) throws Exception {

        getUserXid(updateInfo);

        updateInfo.setContext("accessToken", guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken"));

        updateJawboneUpDataSince(updateInfo, getLastSyncTime(updateInfo, MOVES_LAST_SYNC_TIME), ObjectType.getObjectTypeValue(JawboneUpMovesFacet.class));
        guestService.setApiKeyAttribute(updateInfo.apiKey, MOVES_LAST_SYNC_TIME, String.valueOf(System.currentTimeMillis()));

        updateJawboneUpDataSince(updateInfo, getLastSyncTime(updateInfo, SLEEPS_LAST_SYNC_TIME), ObjectType.getObjectTypeValue(JawboneUpSleepFacet.class));
        guestService.setApiKeyAttribute(updateInfo.apiKey, SLEEPS_LAST_SYNC_TIME, String.valueOf(System.currentTimeMillis()));

        updateJawboneUpDataSince(updateInfo, getLastSyncTime(updateInfo, MEALS_LAST_SYNC_TIME), ObjectType.getObjectTypeValue(JawboneUpMealFacet.class));
        guestService.setApiKeyAttribute(updateInfo.apiKey, MEALS_LAST_SYNC_TIME, String.valueOf(System.currentTimeMillis()));

        updateJawboneUpDataSince(updateInfo, getLastSyncTime(updateInfo, WORKOUTS_LAST_SYNC_TIME), ObjectType.getObjectTypeValue(JawboneUpWorkoutFacet.class));
        guestService.setApiKeyAttribute(updateInfo.apiKey, WORKOUTS_LAST_SYNC_TIME, String.valueOf(System.currentTimeMillis()));

        // not updating moods because the API doesn't allow getting updated_after values...
    }

    /**
     * Retrieve Jawbone's unique identifier for the user, so that we can check that data removal requests
     * have been properly executed
     * @param updateInfo
     * @throws Exception
     */
    private void getUserXid(UpdateInfo updateInfo) throws Exception {
        String user_xid = guestService.getApiKeyAttribute(updateInfo.apiKey, "user_xid");
        if (user_xid == null) {
            String meJson = callJawboneAPI(updateInfo, "https://jawbone.com/nudge/api/v.1.1/users/@me");
            JSONObject jsonObject = JSONObject.fromObject(meJson);
            JSONObject meJsonData = jsonObject.getJSONObject("data");
            user_xid = meJsonData.getString("xid");
            guestService.setApiKeyAttribute(updateInfo.apiKey, "user_xid", user_xid);
        }
    }

    private long getLastSyncTime(final UpdateInfo updateInfo, final String lastSyncTimeAttKey) {
        long lastSyncTime = getBeginningOfTime();
        final String lastSyncTimeAtt = guestService.getApiKeyAttribute(updateInfo.apiKey, lastSyncTimeAttKey);
        if (lastSyncTimeAtt!=null)
            lastSyncTime = Long.valueOf(lastSyncTimeAtt);
        return lastSyncTime;
    }

    private long getBeginningOfTime() {
        return ISODateTimeFormat.basicDate().withZoneUTC().parseMillis("20100101");
    }

    private void updateJawboneUpDataSince(final UpdateInfo updateInfo, long lastSyncTime, int objectTypeValue) throws Exception {
        final HttpClient client = env.getHttpClient();
        try {
            // get moves since lastSyncTime
            String endpoint = endpointDict.get(objectTypeValue);
            String url = getBeginningOfTime()==lastSyncTime
                         ? endpoint + "?start_time=" + getBeginningOfTime()/1000
                         : endpoint + "?start_time=" + getBeginningOfTime()/1000 + "&updated_after=" + lastSyncTime/1000;
            final String upJson = callJawboneAPI(updateInfo, url);
            createOrUpdateFacets(updateInfo, upJson, objectTypeValue);
        }
        finally {
            client.getConnectionManager().shutdown();
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
                    facet.start = facet.time_created*1000;
                    facet.end = facet.time_completed*1000;

                    if (jsonObject.has("title"))
                        facet.title = jsonObject.getString("title");
                    if (jsonObject.has("snapshot_image")) {
                        facet.snapshot_image = jsonObject.getString("snapshot_image");
                        cacheImage(updateInfo, facet.snapshot_image);
                    }

                    String dateString = jsonObject.getString("date");

                    JSONObject details = jsonObject.getJSONObject("details");
                    facet.tz = details.getString("tz");
                    facet.tzs = details.getJSONArray("tzs").toString();
                    TimeZone defaultTimeZone = TimeZone.getTimeZone(facet.tz);
                    TimezoneMap tzMap = getTimeZoneMap(facet.tzs);

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

                    try {
                        String intensityJSON = callJawboneAPI(updateInfo, String.format("https://jawbone.com/nudge/api/moves/%s/snapshot", xid));
                        JSONObject intensity = JSONObject.fromObject(intensityJSON);
                        JSONArray intensityData = intensity.getJSONArray("data");
                        facet.intensityStorage = intensityData.toString();
                    } catch (Throwable t) {
                        logger.warn("could not import Jawbone UP moves intensity records: " + t.getMessage());
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
        if (newFacet!=null) {
            List<AbstractFacet> newFacets = new ArrayList<AbstractFacet>();
            newFacets.add(newFacet);
            bodyTrackStorageService.storeApiData(updateInfo.apiKey, newFacets);
        }
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

    /**
     * Retrieve the snapshot_image and cache it in the user's resources directory
     * @param updateInfo
     * @param uri
     */
    private void cacheImage(final UpdateInfo updateInfo, final String uri) {
        final String devKvsLocation = env.get("btdatastore.db.location");
        executor.execute(new Runnable() {
            public void run() {
                try {
                    String url = "https://jawbone.com" + uri;

                    HttpClient client = env.getHttpClient();
                    HttpGet request = new HttpGet(url);
                    HttpResponse response = null;
                        response = client.execute(request);

                    BufferedInputStream rd = new BufferedInputStream(
                            response.getEntity().getContent());

                    File f = new File(new StringBuilder(devKvsLocation).append(File.separator)
                                              .append(updateInfo.getGuestId())
                                              .append(File.separator)
                                              .append(connector().prettyName())
                                              .append(File.separator)
                                              .append(updateInfo.apiKey.getId())
                                              .append(uri).toString());
                    f.getParentFile().mkdirs();

                    IOUtils.copy(rd, new FileOutputStream(f));
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
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

    private void createOrUpdateFacets(final UpdateInfo updateInfo, final String upJson, int objectTypeValue) throws Exception {
        JSONObject jsonObject = JSONObject.fromObject(upJson);
        JSONObject data = jsonObject.getJSONObject("data");
        JSONArray items = data.getJSONArray("items");
        for (int i=0; i<items.size(); i++) {
            JSONObject json = items.getJSONObject(i);
            if (objectTypeValue==ObjectType.getObjectTypeValue(JawboneUpSleepFacet.class))
                createOrUpdateSleepFacet(json, updateInfo);
            else if (objectTypeValue==ObjectType.getObjectTypeValue(JawboneUpMovesFacet.class))
                createOrUpdateMovesFacet(json, updateInfo);
            else if (objectTypeValue==ObjectType.getObjectTypeValue(JawboneUpMealFacet.class))
                createOrUpdateMealFacet(json, updateInfo);
            else if (objectTypeValue==ObjectType.getObjectTypeValue(JawboneUpWorkoutFacet.class))
                createOrUpdateWorkoutFacet(json, updateInfo);
        }
        if (data.has("links")) {
            JSONObject links = data.getJSONObject("links");
            if (links.has("next")) {
                String next = links.getString("next");
                getNextBatchOfFacets(updateInfo, next, objectTypeValue);
            }
        }
    }

    private void getNextBatchOfFacets(final UpdateInfo updateInfo, String url, final int objectTypeValue) throws Exception {
        url = url.replaceAll("v.None", "v.1.0");
        logger.info(String.format("getting next batch of jawbone facets (objectType value: %s): %s", objectTypeValue, url));
        final String jawboneAPIJson = callJawboneAPI(updateInfo, "https://jawbone.com" + url);
        createOrUpdateFacets(updateInfo, jawboneAPIJson, objectTypeValue);
    }

    private void createOrUpdateSleepFacet(final JSONObject jsonObject, final UpdateInfo updateInfo) throws Exception {

        final String xid = jsonObject.getString("xid");
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery("e.apiKeyId=? AND e.xid=?",
                                                                                   updateInfo.apiKey.getId(), xid);
        final ApiDataService.FacetModifier<JawboneUpSleepFacet> facetModifier = new ApiDataService.FacetModifier<JawboneUpSleepFacet>() {
            @Override
            public JawboneUpSleepFacet createOrModify(JawboneUpSleepFacet origFacet, final Long apiKeyId) {
                try {
                    JawboneUpSleepFacet facet = origFacet;
                    if (facet==null) {
                        facet = new JawboneUpSleepFacet(updateInfo.apiKey.getId());
                        facet.xid = xid;
                        extractCommonFacetData(facet, updateInfo);
                    }
                    facet.time_created = jsonObject.getLong("time_created");
                    facet.time_completed = jsonObject.getLong("time_completed");
                    facet.start = facet.time_created*1000;
                    facet.end = facet.time_completed*1000;

                    JSONObject details = jsonObject.getJSONObject("details");
                    String dateString = jsonObject.getString("date");
                    final LocalDate localDate = ISODateTimeFormat.basicDate().withZoneUTC().parseLocalDate(dateString);
                    facet.date = ISODateTimeFormat.date().withZoneUTC().print(localDate);

                    facet.tz = details.getString("tz");

                    if (jsonObject.has("title"))
                        facet.title = jsonObject.getString("title");
                    if (jsonObject.has("snapshot_image")) {
                        facet.snapshot_image = jsonObject.getString("snapshot_image");
                        cacheImage(updateInfo, facet.snapshot_image);
                    }

                    extractGPSData(facet, jsonObject, updateInfo, xid);

                    if (details.has("smart_alarm_fire"))
                        facet.smart_alarm_fire = details.getLong("smart_alarm_fire");
                    if (details.has("awake_time"))
                        facet.awake_time = details.getLong("awake_time");
                    if (details.has("asleep_time"))
                        facet.asleep_time = details.getLong("asleep_time");
                    if (details.has("awakenings"))
                        facet.awakenings = details.getInt("awakenings");
                    if (details.has("rem"))
                        facet.rem = details.getInt("rem");
                    if (details.has("light"))
                        facet.light = details.getInt("light");
                    if (details.has("deep"))
                        facet.deep = details.getInt("deep");
                    if (details.has("awake"))
                        facet.awake = details.getInt("awake");
                    if (details.has("duration"))
                        facet.duration = details.getInt("duration");
                    if (details.has("quality"))
                        facet.quality = details.getInt("quality");

                    try {
                        String phasesJSON = callJawboneAPI(updateInfo, String.format("https://jawbone.com/nudge/api/sleeps/%s/snapshot", xid));
                        JSONObject phases = JSONObject.fromObject(phasesJSON);
                        JSONArray phasesData = phases.getJSONArray("data");
                        facet.phasesStorage = phasesData.toString();
                    } catch (Throwable t) {
                        logger.warn("could not import Jawbone UP sleep phases records: " + t.getMessage());
                    }

                    return facet;
                } catch (Throwable t) {
                    logger.warn("could not import a Jawbone UP sleep record: " + t.getMessage());
                    t.printStackTrace();
                    return origFacet;
                }
            }
        };
        final JawboneUpSleepFacet newFacet = apiDataService.createOrReadModifyWrite(JawboneUpSleepFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
        if (newFacet!=null) {
            List<AbstractFacet> newFacets = new ArrayList<AbstractFacet>();
            newFacets.add(newFacet);
            bodyTrackStorageService.storeApiData(updateInfo.apiKey, newFacets);
        }
    }

    public static void main(final String[] args) {
        String place_lat_string = "";
        System.out.println("not blank? " + StringUtils.isNotBlank(place_lat_string));
        System.out.println("numeric? " + StringUtils.isNumeric(place_lat_string));
    }

    private void extractGPSData(final JawboneUpGeoFacet facet, final JSONObject jsonObject, final UpdateInfo updateInfo, final String xid) {
        if (jsonObject.has("place_lat")&&
            jsonObject.has("place_lon")&&
            !(jsonObject.getString("place_lat").equals(""))&&
            !(jsonObject.getString("place_lon").equals("")))
        {
            double place_lat, place_lon;
            try {
                place_lat = jsonObject.getDouble("place_lat");
                place_lon = jsonObject.getDouble("place_lon");
            } catch (Throwable t) {
                System.out.println("Couldn't get latitude/longitude from strings: " + jsonObject.getString("place_lat") + "/" + jsonObject.getString("place_lon"));
                return;
            }
            facet.place_lat = place_lat;
            facet.place_lon = place_lon;
            if (jsonObject.has("place_name"))
                facet.place_name = jsonObject.getString("place_name");

            LocationFacet locationFacet = new LocationFacet(updateInfo.apiKey.getId());

            if (jsonObject.has("place_acc")&&StringUtils.isNotEmpty("place_acc")) {
                String placeAccuracy = jsonObject.getString("place_acc");
                try {
                    int place_acc = Integer.valueOf(placeAccuracy);
                    locationFacet.accuracy = jsonObject.getInt("place_acc");
                    facet.place_acc = place_acc;
                } catch(Throwable t) {
                    locationFacet.accuracy = -1;
                    facet.place_acc = -1;
                }
            } else {
                locationFacet.accuracy = -1;
                facet.place_acc = -1;
            }

            locationFacet.latitude = (float) place_lat;
            locationFacet.longitude = (float) place_lon;
            locationFacet.timestampMs = facet.start;
            locationFacet.start = locationFacet.timestampMs;
            locationFacet.end = locationFacet.timestampMs;
            locationFacet.source = LocationFacet.Source.JAWBONE_UP;
            locationFacet.apiKeyId = updateInfo.apiKey.getId();
            locationFacet.api = connector().value();
            locationFacet.uri = xid;
            apiDataService.addGuestLocation(updateInfo.getGuestId(), locationFacet);
        }
    }

    private void createOrUpdateMealFacet(final JSONObject jsonObject, final UpdateInfo updateInfo) throws Exception {

        final String xid = jsonObject.getString("xid");
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery("e.apiKeyId=? AND e.xid=?",
                                                                                   updateInfo.apiKey.getId(), xid);
        final ApiDataService.FacetModifier<JawboneUpMealFacet> facetModifier = new ApiDataService.FacetModifier<JawboneUpMealFacet>() {
            @Override
            public JawboneUpMealFacet createOrModify(JawboneUpMealFacet origFacet, final Long apiKeyId) {
                try {
                    JawboneUpMealFacet facet = origFacet;
                    if (facet==null) {
                        facet = new JawboneUpMealFacet(updateInfo.apiKey.getId());
                        facet.xid = xid;
                        extractCommonFacetData(facet, updateInfo);
                    }
                    facet.time_created = jsonObject.getLong("time_created");
                    facet.time_updated = jsonObject.getLong("time_updated");
                    facet.time_completed = jsonObject.getLong("time_completed");
                    facet.start = facet.time_created*1000;
                    facet.end = facet.time_completed*1000;

                    JSONObject details = jsonObject.getJSONObject("details");
                    String dateString = jsonObject.getString("date");
                    final LocalDate localDate = ISODateTimeFormat.basicDate().withZoneUTC().parseLocalDate(dateString);
                    facet.date = ISODateTimeFormat.date().withZoneUTC().print(localDate);
                    facet.tz = details.getString("tz");

                    facet.mealDetails = details.toString();

                    extractGPSData(facet, jsonObject, updateInfo, xid);

                    // now fetch the servings for this meal...
                    final String servingsJSONStr = callJawboneAPI(updateInfo, "https://jawbone.com/nudge/api/v.1.0/meals/" + xid);
                    JSONObject servingsJSON = JSONObject.fromObject(servingsJSONStr);
                    JSONObject data = servingsJSON.getJSONObject("data");
                    JSONObject itemsObject = data.getJSONObject("items");
                    JSONArray items = itemsObject.getJSONArray("items");
                    List<JawboneUpServingFacet> servingFacets = new ArrayList<JawboneUpServingFacet>();
                    for (int i=0; i<items.size(); i++) {
                        JSONObject servingJSON = items.getJSONObject(i);
                        // pass in start- and end- times of the parent meal facet
                        final JawboneUpServingFacet servingFacet = createOrUpdateServingFacet(servingJSON, updateInfo, facet);
                        servingFacets.add(servingFacet);
                    }
                    facet.setServings(servingFacets);
                    return facet;
                } catch (Throwable t) {
                    logger.warn("could not import a Jawbone UP meal record: " + t.getMessage());
                    t.printStackTrace();
                    return origFacet;
                }
            }
        };
        apiDataService.createOrReadModifyWrite(JawboneUpMealFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    private JawboneUpServingFacet createOrUpdateServingFacet(final JSONObject jsonObject, final UpdateInfo updateInfo, final JawboneUpMealFacet meal) throws Exception {

        final String xid = jsonObject.getString("xid");
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery("e.apiKeyId=? AND e.xid=?",
                                                                                   updateInfo.apiKey.getId(), xid);
        final ApiDataService.FacetModifier<JawboneUpServingFacet> facetModifier = new ApiDataService.FacetModifier<JawboneUpServingFacet>() {
            @Override
            public JawboneUpServingFacet createOrModify(JawboneUpServingFacet origFacet, final Long apiKeyId) {
                try {
                    JawboneUpServingFacet facet = origFacet;
                    if (facet==null) {
                        facet = new JawboneUpServingFacet(updateInfo.apiKey.getId());
                        facet.xid = xid;
                        extractCommonFacetData(facet, updateInfo);
                    }
                    facet.date = meal.date;
                    facet.tz = meal.tz;
                    facet.start = meal.start;
                    facet.end = meal.end;
                    if (jsonObject.has("image")&&StringUtils.isNotEmpty(jsonObject.getString("image"))) {
                        facet.image = jsonObject.getString("image");
                        cacheImage(updateInfo, facet.image);
                    }
                    facet.servingDetails = jsonObject.toString();
                    facet.meal = meal;
                    return facet;
                } catch (Throwable t) {
                    logger.warn("could not import a Jawbone UP meal record: " + t.getMessage());
                    t.printStackTrace();
                    return origFacet;
                }
            }
        };
        final JawboneUpServingFacet newFacet = apiDataService.createOrReadModifyWrite(JawboneUpServingFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
        return newFacet;
    }

    private void createOrUpdateWorkoutFacet(final JSONObject jsonObject, final UpdateInfo updateInfo) throws Exception {

        final String xid = jsonObject.getString("xid");
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery("e.apiKeyId=? AND e.xid=?",
                                                                                   updateInfo.apiKey.getId(), xid);
        final ApiDataService.FacetModifier<JawboneUpWorkoutFacet> facetModifier = new ApiDataService.FacetModifier<JawboneUpWorkoutFacet>() {
            @Override
            public JawboneUpWorkoutFacet createOrModify(JawboneUpWorkoutFacet origFacet, final Long apiKeyId) {
                try {
                    JawboneUpWorkoutFacet facet = origFacet;
                    if (facet==null) {
                        facet = new JawboneUpWorkoutFacet(updateInfo.apiKey.getId());
                        facet.xid = xid;
                        extractCommonFacetData(facet, updateInfo);
                    }
                    facet.time_created = jsonObject.getLong("time_created");
                    facet.time_updated = jsonObject.getLong("time_updated");
                    facet.time_completed = jsonObject.getLong("time_completed");
                    facet.start = facet.time_created*1000;
                    facet.end = facet.time_completed*1000;

                    String dateString = jsonObject.getString("date");
                    final LocalDate localDate = ISODateTimeFormat.basicDate().withZoneUTC().parseLocalDate(dateString);
                    facet.date = ISODateTimeFormat.date().withZoneUTC().print(localDate);

                    JSONObject details = jsonObject.getJSONObject("details");
                    facet.tz = details.getString("tz");
                    facet.sub_type = jsonObject.getInt("sub_type");
                    facet.workoutDetails = details.toString();
                    if (details.has("steps"))
                        facet.steps = details.getInt("steps");
                    if (details.has("time"))
                        facet.duration = details.getInt("time");
                    if (details.has("bg_active_time"))
                        facet.bg_active_time = details.getInt("bg_active_time");
                    if (details.has("meters"))
                        facet.meters = details.getDouble("meters");
                    if (details.has("km"))
                        facet.km = details.getDouble("km");
                    if (details.has("intensity"))
                        facet.intensity = details.getInt("intensity");
                    if (details.has("calories"))
                        facet.calories = details.getDouble("calories");
                    if (details.has("bmr"))
                        facet.bmr = details.getDouble("bmr");
                    if (details.has("bg_calories"))
                        facet.bg_calories = details.getDouble("bg_calories");
                    if (details.has("bmr_calories"))
                        facet.bmr_calories = details.getDouble("bmr_calories");

                    if (jsonObject.has("image")&&!jsonObject.getString("image").equals("")) {
                        facet.image = jsonObject.getString("image");
                        cacheImage(updateInfo, facet.image);
                    }
                    if (jsonObject.has("image")&&!jsonObject.getString("snapshot_image").equals("")) {
                        facet.snapshot_image = jsonObject.getString("snapshot_image");
                        cacheImage(updateInfo, facet.snapshot_image);
                    }
                    if (jsonObject.has("route")&&!jsonObject.getString("route").equals("")) {
                        facet.route = jsonObject.getString("route");
                        cacheImage(updateInfo, facet.route);
                    }

                    extractGPSData(facet, jsonObject, updateInfo, xid);

                    return facet;
                } catch (Throwable t) {
                    logger.warn("could not import a Jawbone UP workout record: " + t.getMessage());
                    t.printStackTrace();
                    return origFacet;
                }
            }
        };
        apiDataService.createOrReadModifyWrite(JawboneUpWorkoutFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    private String callJawboneAPI(final UpdateInfo updateInfo, final String url) throws Exception {
        final HttpClient client = env.getHttpClient();
        final long then = System.currentTimeMillis();
        try {
            long tokenExpires = Long.valueOf(guestService.getApiKeyAttribute(updateInfo.apiKey, "tokenExpires"));
            if (tokenExpires<System.currentTimeMillis())
                refreshToken(updateInfo);
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

    public String refreshToken(ApiKey apiKey) throws IOException, UnexpectedHttpResponseCodeException, UpdateFailedException {
        String refreshToken = guestService.getApiKeyAttribute(apiKey, "refreshToken");
        Map<String,String> parameters = new HashMap<String,String>();
        parameters.put("grant_type", "refresh_token");
        parameters.put("refresh_token", refreshToken);
        parameters.put("client_id", guestService.getApiKeyAttribute(apiKey, "jawboneUp.client.id"));
        parameters.put("client_secret", guestService.getApiKeyAttribute(apiKey, "jawboneUp.client.secret"));
        final String json = HttpUtils.fetch("https://jawbone.com/auth/oauth2/token", parameters);

        JSONObject token = JSONObject.fromObject(json);
        if (!token.has("access_token")) {
            final String message = "Couldn't renew access token (no \"access_token\" field in JSON response)";
            throw new UpdateFailedException(message, new Exception(), true, ApiKey.PermanentFailReason.unknownReason(message));
        }
        final String accessToken = token.getString("access_token");
        // store the new secret
        guestService.setApiKeyAttribute(apiKey,
                "jawboneUp.client.secret", env.get("jawboneUp.client.secret"));
        guestService.setApiKeyAttribute(apiKey,
                "accessToken", accessToken);
        long expiresIn = token.getLong("expires_in");
        guestService.setApiKeyAttribute(apiKey,
                "tokenExpires", String.valueOf(new BigInteger(String.valueOf(System.currentTimeMillis())).
                        add(new BigInteger(String.valueOf(expiresIn*1000)))));
        guestService.setApiKeyAttribute(apiKey,
                "refreshToken", token.getString("refresh_token"));
        return accessToken;
    }

    private void refreshToken(UpdateInfo updateInfo) throws IOException, UnexpectedHttpResponseCodeException, UpdateFailedException {
        String accessToken = refreshToken(updateInfo.apiKey);
        updateInfo.setContext("accessToken", accessToken);
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
                    throw new UpdateFailedException(message + " - " + details, true, ApiKey.PermanentFailReason.unknownReason(details));
                }
            }
        } catch (Throwable t) {
            // just ignore any potential problems here
        }
        if (statusCode==401)
            throw new AuthExpiredException();
        if (statusCode>=400&&statusCode<500) {
            throw new UpdateFailedException("Unexpected response code: " + statusCode, new Exception(), true,
                                            ApiKey.PermanentFailReason.clientError(statusCode, message));
        } else
            throw new UpdateFailedException("Unexpected response code: " + statusCode);
    }
}
