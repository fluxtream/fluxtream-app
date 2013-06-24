package com.fluxtream.connectors.moves;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.AbstractLocalTimeFacet;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.utils.HttpUtils;
import com.fluxtream.utils.JPAUtils;
import com.fluxtream.utils.Utils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * User: candide
 * Date: 17/06/13
 * Time: 16:50
 */
@Component
@Updater(prettyName = "Moves", value = 144, objectTypes = {LocationFacet.class, MovesMoveFacet.class, MovesPlaceFacet.class})
public class MovesUpdater extends AbstractUpdater {

    final static String host = "https://api.moves-app.com/api/v1";
    public static DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyyMMdd");
    public static DateTimeFormatter timeStorageFormat = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss'Z'");

    @Autowired
    MovesController controller;

    @Autowired
    MetadataService metadataService;

    @Autowired
    JPADaoService jpaDaoService;

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        long then = System.currentTimeMillis();
        String accessToken = controller.getAccessToken(updateInfo.apiKey);
        String query = host + "/user/profile?access_token=" + accessToken;
        String firstDate = null;
        try {
            final String fetched = HttpUtils.fetch(query);
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, query);
            JSONObject json = JSONObject.fromObject(fetched);
            if (!json.has("profile"))
                throw new Exception("no profile");
            final JSONObject profile = json.getJSONObject("profile");
            if (!profile.has("firstDate"))
                throw new Exception("no firstDate in profile");
            firstDate = profile.getString("firstDate");
        } catch (Exception e) {
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, query, Utils.stackTrace(e));
        }

        List<String> dates = getDatesSince(firstDate);
        updateConnectorDataForDates(updateInfo, dates);
    }

    private void updateConnectorDataForDates(final UpdateInfo updateInfo, final List<String> dates) throws Exception {
        for (String date : dates) {
            final String fetched = fetchStorylineForDate(updateInfo, date, true);
            if (fetched!=null) {
                final List<AbstractFacet> abstractFacets = extractFacets(updateInfo, fetched);
                for (AbstractFacet facet : abstractFacets) {
                    apiDataService.persistFacet(facet);
                }
                guestService.setApiKeyAttribute(updateInfo.apiKey, "lastDate", date);
            }
        }
    }

    private String fetchStorylineForDate(final UpdateInfo updateInfo, final String date, final boolean withTrackpoints) throws Exception {
        long then = System.currentTimeMillis();
        String fetched = null;
        try {
            String accessToken = controller.getAccessToken(updateInfo.apiKey);
            String fetchUrl = String.format(host + "/user/storyline/daily/%s?trackPoints=%s&access_token=%s",
                                            date, withTrackpoints, accessToken);
            fetched = HttpUtils.fetch(fetchUrl);
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, fetchUrl);
        } catch (Exception e) {
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, date, Utils.stackTrace(e));
        }
        return fetched;
    }

    private static List<String> getDatesSince(String fromDate) {
        List<String> dates = new ArrayList<String>();
        DateTime then = dateFormat.withZoneUTC().parseDateTime(fromDate);
        String today = dateFormat.withZoneUTC().print(System.currentTimeMillis());
        DateTime todaysTime = dateFormat.withZoneUTC().parseDateTime(today);
        if (then.isAfter(todaysTime))
            throw new IllegalArgumentException("fromDate is after today");
        while (!today.equals(fromDate)) {
            dates.add(fromDate);
            then = dateFormat.withZoneUTC().parseDateTime(fromDate);
            String date = dateFormat.withZoneUTC().print(then.plusDays(1));
            fromDate = date;
        }
        dates.add(today);
        return dates;
    }

    @Override
    protected void updateConnectorData(final UpdateInfo updateInfo) throws Exception {
        //// if lastDate is today, it means that we probably already have partial data, so
        //// let's be parcimonious
        //String lastDate = guestService.getApiKeyAttribute(updateInfo.apiKey, "lastDate");
        //TimeZone guestTimezone = metadataService.getCurrentTimeZone(updateInfo.getGuestId());
        //String today = dateFormat.withZone(DateTimeZone.forTimeZone(guestTimezone)).print(System.currentTimeMillis());
        //final List<String> datesSince = getDatesSince(lastDate);
        //if (today.equals(lastDate)) {
        //    fixUpExistingData(Arrays.asList(today), updateInfo, true);
        //} else {
        //    // for all other days since lastDate, we do the same thing as for a history update,
        //    // -> first we delete potentially existing data for those dates
        //    deleteDataForDates(updateInfo, datesSince);
        //    updateConnectorDataForDates(updateInfo, datesSince);
        //}
        //// finally, we cleanup existing data for 7 days before that (reflect manual fixup of the data)
        //fixUpExistingData(getDatesBefore(lastDate, 7), updateInfo, false);
    }

    private void deleteDataForDates(final UpdateInfo updateInfo, final List<String> dates) {
        for (String date : dates) {
            jpaDaoService.execute("DELETE facet FROM " + JPAUtils.getEntityName(MovesMoveFacet.class) +
                " facet WHERE facet.apiKeyId=" + updateInfo.apiKey.getId() + " AND facet.date='" + date + "'");
            jpaDaoService.execute("DELETE facet FROM " + JPAUtils.getEntityName(MovesPlaceFacet.class) +
                " facet WHERE facet.apiKeyId=" + updateInfo.apiKey.getId() + " AND facet.date='" + date + "'");
        }
    }

    private List<String> getDatesBefore(String date, int nDays) {
        DateTime initialDate = dateFormat.withZoneUTC().parseDateTime(date);
        List<String> dates = new ArrayList<String>();
        for (int i=0; i<nDays; i++) {
            initialDate = initialDate.minusDays(1);
            String nextDate = dateFormat.withZoneUTC().print(initialDate);
            dates.add(nextDate);
        }
        return dates;
    }

    private void fixUpExistingData(List<String> dates, UpdateInfo updateInfo, boolean withTrackpoints) throws Exception {
        Connector connector = Connector.getConnector("moves");
        ObjectType moveOT = ObjectType.getObjectType(connector, "move");
        ObjectType placeOT = ObjectType.getObjectType(connector, "place");
        for (String date : dates) {
            String fetched = fetchStorylineForDate(updateInfo, date, withTrackpoints);
            final JSONArray segments = getSegments(fetched);
            List<MovesMoveFacet> moves = apiDataService.getApiDataFacets(updateInfo.apiKey, moveOT, Arrays.asList(date), MovesMoveFacet.class);
            List<MovesPlaceFacet> places = apiDataService.getApiDataFacets(updateInfo.apiKey, placeOT, Arrays.asList(date), MovesPlaceFacet.class);
            fixUpExistingData(updateInfo, segments, moves, places, date);
        }
    }

    private void fixUpExistingData(final UpdateInfo updateInfo,
                                   final JSONArray segments,
                                   final List<MovesMoveFacet> moves,
                                   final List<MovesPlaceFacet> places,
                                   final String date) {
        for (int j=0; j<segments.size(); j++) {
            JSONObject segment = segments.getJSONObject(j);
            if (segment.getString("type").equals("move")) {
                final long start = timeStorageFormat.withZoneUTC().parseDateTime(segment.getString("startTime")).getMillis();
                for (MovesMoveFacet move : moves) {
                    if (start==move.start){
                        tidyUpActivities(updateInfo, segment, move);
                        break;
                    }
                }
                final MovesMoveFacet movesMoveFacet = extractMove(date, segment, updateInfo);
                apiDataService.persistFacet(movesMoveFacet);
            } else if (segment.getString("type").equals("place")) {
                final long start = timeStorageFormat.withZoneUTC().parseDateTime(segment.getString("startTime")).getMillis();
                for (MovesPlaceFacet place :places) {
                    if (start==place.start){
                        tidyUpPlaceFacet(segment, place);
                        tidyUpActivities(updateInfo, segment, place);
                        break;
                    }
                }
                final MovesPlaceFacet movesPlaceFacet = extractPlace(date, segment, updateInfo);
                apiDataService.persistFacet(movesPlaceFacet);
            }
        }
    }

    @Transactional(readOnly=false)
    private void tidyUpActivities(final UpdateInfo updateInfo, final JSONObject segment, final MovesFacet facet) {
        final List<MovesActivity> movesActivities = facet.getActivities();
        if (!segment.has("activities"))
            return;
        final JSONArray activities = segment.getJSONArray("activities");
        boolean needsUpdate = false;
        if (movesActivities.size()<activities.size()) {
            // identify missing activities and add them to the facet's activities
            addMissingActivities(updateInfo, movesActivities, activities, facet);
            needsUpdate = true;
        } else if (movesActivities.size()>activities.size()) {
            // find out which activities we need to remove
            removeActivities(movesActivities, activities, facet);
            needsUpdate = true;
        }
        // finally, update activities that need it
        needsUpdate|=updateActivities(updateInfo, movesActivities, activities);
        if (needsUpdate)
            jpaDaoService.persist(facet);
    }

    private void removeActivities(final List<MovesActivity> movesActivities, final JSONArray activities, final MovesFacet facet) {
        withMovesActivities:for (int i=0; i<movesActivities.size(); i++) {
            final MovesActivity movesActivity = movesActivities.get(i);
            for (int j=0; i<activities.size(); i++) {
                JSONObject activityData = activities.getJSONObject(j);
                final long start = timeStorageFormat.withZoneUTC().parseDateTime(activityData.getString("startTime")).getMillis();
                if (movesActivity.start==start) {
                    continue withMovesActivities;
                }
            }
            facet.removeActivity(movesActivity);
        }
    }

    private boolean updateActivities(final UpdateInfo updateInfo, final List<MovesActivity> movesActivities, final JSONArray activities) {
        boolean needsUpdate = false;
        for (int i=0; i<activities.size(); i++) {
            JSONObject activityData = activities.getJSONObject(i);
            for (int j=0; j<movesActivities.size(); j++) {
                final long start = timeStorageFormat.withZoneUTC().parseDateTime(activityData.getString("startTime")).getMillis();
                final MovesActivity movesActivity = movesActivities.get(j);
                if (movesActivity.start==start) {
                    needsUpdate|=updateActivity(updateInfo, movesActivity, activityData);
                }

            }
        }
        return needsUpdate;
    }

    private boolean updateActivity(final UpdateInfo updateInfo,
                                   final MovesActivity movesActivity,
                                   final JSONObject activityData) {
        boolean needsUpdate = false;
        final long end = timeStorageFormat.withZoneUTC().parseDateTime(activityData.getString("endTime")).getMillis();
        if (movesActivity.end!=end) {
            needsUpdate = true;
            movesActivity.end = end;
            movesActivity.endTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.print(end);
            movesActivity.end = end;
        }

        final String activity = activityData.getString("activity");
        if (!movesActivity.activity.equals(activity)) {
            needsUpdate = true;
            movesActivity.activity = activity;
        }

        if ((activityData.has("steps")&&movesActivity.steps==null)||
            (activityData.has("steps")&&movesActivity.steps!=activityData.getInt("steps"))) {
            needsUpdate = true;
            movesActivity.steps = activityData.getInt("steps");
        }
        if (activityData.getInt("distance")!=movesActivity.distance) {
            needsUpdate = true;
            movesActivity.distance = activityData.getInt("distance");
        }
        if (activityData.has("trackPoints")) {
            if (movesActivity.activityURI!=null) {
                // note: we don't needs to set needsUpdate to true here as the location data is
                // not stored with the facet per se, it is stored separately in the LocationFacets table
                final long stored = jpaDaoService.executeCount("SELECT count(e) FROM " +
                                                          JPAUtils.getEntityName(LocationFacet.class) +
                                                          " facet WHERE facet.source=" + LocationFacet.Source.MOVES +
                                                          " AND facet.uri='" + movesActivity.activityURI + "'");
                final JSONArray trackPoints = activityData.getJSONArray("trackPoints");
                if (stored!=trackPoints.size()) {
                    jpaDaoService.execute("DELETE facet FROM " +
                                          JPAUtils.getEntityName(LocationFacet.class) +
                                          " facet WHERE facet.source=" + LocationFacet.Source.MOVES +
                                          " AND facet.uri='" + movesActivity.activityURI + "'");
                    extractTrackPoints(movesActivity.activityURI, activityData, updateInfo);
                }
            } else {
                needsUpdate = true; // adding an activityURI means an update is needed
                movesActivity.activityURI = UUID.randomUUID().toString();
                extractTrackPoints(movesActivity.activityURI, activityData, updateInfo);
            }
        }
        return needsUpdate;
    }

    private void addMissingActivities(final UpdateInfo updateInfo,
                                      final List<MovesActivity> movesActivities,
                                      final JSONArray activities,
                                      final MovesFacet facet) {
        withApiActivities:for (int i=0; i<activities.size(); i++) {
            JSONObject activityData = activities.getJSONObject(i);
            for (int j=0; j<movesActivities.size(); j++) {
                final long start = timeStorageFormat.withZoneUTC().parseDateTime(activityData.getString("startTime")).getMillis();
                if (movesActivities.get(j).start==start) {
                    continue withApiActivities;
                }
            }
            final MovesActivity activity = extractActivity(updateInfo, activityData);
            facet.addActivity(activity);
        }
    }

    @Transactional(readOnly=false)
    private void tidyUpPlaceFacet(final JSONObject segment, final MovesPlaceFacet place) {
        JSONObject placeData = segment.getJSONObject("place");
        boolean needsUpdating = false;
        if (placeData.has("id")&&place.placeId==null) {
            System.out.println("now the place has an id");
            needsUpdating = true;
            place.placeId = placeData.getLong("id");
        }
        // update the place type
        String previousPlaceType = place.type;
        if (!placeData.getString("type").equals(place.type)) {
            System.out.println("place type has changed");
            needsUpdating = true;
            place.type = placeData.getString("type");
        }
        // if the place wasn't identified previously, store its fourquare info now
        if (!previousPlaceType.equals("foursquare")&&
            place.type.equals("foursquare")){
            System.out.println("storing foursquare info");
            place.name = placeData.getString("name");
            place.foursquareId = placeData.getString("foursquareId");
        }
        JSONObject locationData = placeData.getJSONObject("location");
        float lat = (float) locationData.getDouble("lat");
        float lon = (float) locationData.getDouble("lon");
        if (lat!=place.latitude||lon!=place.longitude) {
            System.out.println("lat/lon have changed");
            needsUpdating = true;
            place.latitude = lat;
            place.longitude = lon;
        }
        if (needsUpdating)
            jpaDaoService.persist(place);
    }

    private JSONArray getSegments(final String json) {
        JSONArray jsonArray = JSONArray.fromObject(json);
        JSONObject dayFacetData = jsonArray.getJSONObject(0);
        JSONArray segments = dayFacetData.getJSONArray("segments");
        return segments;
    }

    private List<AbstractFacet> extractFacets(UpdateInfo updateInfo, String json) throws Exception {
        List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        JSONArray jsonArray = JSONArray.fromObject(json);
        for (int i=0; i<jsonArray.size(); i++) {
            JSONObject dayFacetData = jsonArray.getJSONObject(i);
            String date = dayFacetData.getString("date");
            JSONArray segments = dayFacetData.getJSONArray("segments");
            for (int j=0; j<segments.size(); j++) {
                JSONObject segment = segments.getJSONObject(j);
                if (segment.getString("type").equals("move")) {
                    facets.add(extractMove(date, segment, updateInfo));
                } else if (segment.getString("type").equals("place")) {
                    facets.add(extractPlace(date, segment, updateInfo));
                }
            }
        }
        return facets;
    }

    private MovesPlaceFacet extractPlace(final String date, final JSONObject segment, UpdateInfo updateInfo) {
        MovesPlaceFacet facet = new MovesPlaceFacet(updateInfo.apiKey.getId());
        extractMoveData(date, segment, facet, updateInfo);
        extractPlaceData(segment, facet);
        return facet;
    }

    private void extractPlaceData(final JSONObject segment, final MovesPlaceFacet facet) {
        JSONObject placeData = segment.getJSONObject("place");
        if (placeData.has("id"))
            facet.placeId = placeData.getLong("id");
        facet.type = placeData.getString("type");
        if (facet.type.equals("foursquare")){
            facet.name = placeData.getString("name");
            facet.foursquareId = placeData.getString("foursquareId");
        }
        JSONObject locationData = placeData.getJSONObject("location");
        facet.latitude = (float) locationData.getDouble("lat");
        facet.longitude = (float) locationData.getDouble("lon");
    }

    private MovesMoveFacet extractMove(final String date, final JSONObject segment, final UpdateInfo updateInfo) {
        MovesMoveFacet facet = new MovesMoveFacet(updateInfo.apiKey.getId());
        extractMoveData(date, segment, facet, updateInfo);
        return facet;
    }

    private void extractMoveData(final String date, final JSONObject segment, final MovesFacet facet, UpdateInfo updateInfo) {
        facet.date = date;
        final DateTime startTime = timeStorageFormat.withZoneUTC().parseDateTime(segment.getString("startTime"));
        final DateTime endTime = timeStorageFormat.withZoneUTC().parseDateTime(segment.getString("endTime"));
        facet.startTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.print(startTime);
        facet.endTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.print(endTime);
        facet.start = startTime.getMillis();
        facet.end = endTime.getMillis();
        extractActivities(segment, facet, updateInfo);
    }

    private void extractActivities(final JSONObject segment, final MovesFacet facet, UpdateInfo updateInfo) {
        if (!segment.has("activities"))
            return;
        final JSONArray activities = segment.getJSONArray("activities");
        for (int i=0; i<activities.size(); i++) {
            JSONObject activityData = activities.getJSONObject(i);
            MovesActivity activity = extractActivity(updateInfo, activityData);
            facet.addActivity(activity);
        }
    }

    private MovesActivity extractActivity(final UpdateInfo updateInfo, final JSONObject activityData) {
        MovesActivity activity = new MovesActivity();
        activity.activityURI = UUID.randomUUID().toString();
        activity.activity = activityData.getString("activity");
        final DateTime startTime = timeStorageFormat.withZoneUTC().parseDateTime(activityData.getString("startTime"));
        final DateTime endTime = timeStorageFormat.withZoneUTC().parseDateTime(activityData.getString("endTime"));
        activity.startTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.print(startTime);
        activity.endTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.print(endTime);
        activity.start = startTime.getMillis();
        activity.end = endTime.getMillis();
        if (activityData.has("steps"))
            activity.steps = activityData.getInt("steps");
        activity.distance = activityData.getInt("distance");
        extractTrackPoints(activity.activityURI, activityData, updateInfo);
        return activity;
    }

    private void extractTrackPoints(final String activityId, final JSONObject activityData, UpdateInfo updateInfo) {
        final JSONArray trackPoints = activityData.getJSONArray("trackPoints");
        List<LocationFacet> locationFacets = new ArrayList<LocationFacet>();
        // timeZone is computed based on first location for each batch of trackPoints
        TimeZone timeZone = null;
        Connector connector = Connector.getConnector("moves");
        for (int i=0; i<trackPoints.size(); i++) {
            JSONObject trackPoint = trackPoints.getJSONObject(i);
            LocationFacet locationFacet = new LocationFacet(updateInfo.apiKey.getId());
            locationFacet.latitude = (float) trackPoint.getDouble("lat");
            locationFacet.longitude = (float) trackPoint.getDouble("lon");
            if (timeZone==null)
                timeZone = metadataService.getTimeZone(locationFacet.latitude, locationFacet.longitude);
            final DateTime time = timeStorageFormat.withZone(DateTimeZone.forTimeZone(timeZone)).parseDateTime(trackPoint.getString("time"));
            locationFacet.timestampMs = time.getMillis();
            locationFacet.api = connector.value();
            locationFacet.start = locationFacet.timestampMs;
            locationFacet.end = locationFacet.timestampMs;
            locationFacet.source = LocationFacet.Source.MOVES;
            locationFacet.apiKeyId = updateInfo.apiKey.getId();
            locationFacet.uri = activityId;
            locationFacets.add(locationFacet);
        }
        apiDataService.addGuestLocations(updateInfo.getGuestId(), locationFacets);
    }

}
