package com.fluxtream.connectors.moves;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.AbstractLocalTimeFacet;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import com.fluxtream.services.ApiDataService;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 17/06/13
 * Time: 20:38
 */
@Component
public class MovesFacetExtractor extends AbstractFacetExtractor {

    public static DateTimeFormatter timeStorageFormat = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss'Z'");

    @Autowired
    ApiDataService apiDataService;

    @Override
    public List<AbstractFacet> extractFacets(final ApiData apiData, final ObjectType objectType) throws Exception {
        JSONArray jsonArray = JSONArray.fromObject(apiData.json);
        List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        for (int i=0; i<jsonArray.size(); i++) {
            JSONObject dayFacetData = jsonArray.getJSONObject(i);
            String date = dayFacetData.getString("date");
            JSONArray segments = dayFacetData.getJSONArray("segments");
            for (int j=0; j<segments.size(); j++) {
                JSONObject segment = segments.getJSONObject(j);
                if (segment.getString("type").equals("move")) {
                    facets.add(extractMove(date, segment));
                } else if (segment.getString("type").equals("place")) {
                    facets.add(extractPlace(date, segment));
                }
            }
        }
        return facets;
    }

    private MovesPlaceFacet extractPlace(final String date, final JSONObject segment) {
        MovesPlaceFacet facet = new MovesPlaceFacet();
        extractMoveData(date, segment, facet);
        extractPlaceData(segment, facet);
        return facet;
    }

    private void extractPlaceData(final JSONObject segment, final MovesPlaceFacet facet) {
        JSONObject placeData = segment.getJSONObject("place");
        facet.placeId = placeData.getLong("id");
        facet.name = placeData.getString("name");
        facet.type = placeData.getString("type");
        if (facet.type.equals("foursquare")){
            facet.foursquareId = placeData.getString("foursquareId");
        }
        JSONObject locationData = placeData.getJSONObject("location");
        facet.latitude = (float) locationData.getDouble("lat");
        facet.longitude = (float) locationData.getDouble("lon");
    }

    private MovesMoveFacet extractMove(final String date, final JSONObject segment) {
        MovesMoveFacet facet = new MovesMoveFacet();
        extractMoveData(date, segment, facet);
        return facet;
    }

    private void extractMoveData(final String date, final JSONObject segment, final MovesMoveFacet facet) {
        facet.date = date;
        final DateTime startTime = timeStorageFormat.withZoneUTC().parseDateTime(segment.getString("startTime"));
        final DateTime endTime = timeStorageFormat.withZoneUTC().parseDateTime(segment.getString("endTime"));
        facet.startTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.print(startTime);
        facet.endTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.print(endTime);
        facet.start = startTime.getMillis();
        facet.end = endTime.getMillis();
        extractActivities(segment, facet);
    }

    private void extractActivities(final JSONObject segment, final MovesMoveFacet facet) {
        final JSONArray activities = segment.getJSONArray("activities");
        if (activities.size()>0)
            facet.activities = new ArrayList<Activity>();
        for (int i=0; i<activities.size(); i++) {
            JSONObject activityData = activities.getJSONObject(i);
            Activity activity = new Activity();
            activity.activityId = UUID.randomUUID().toString();
            activity.activity = activityData.getString("activity");
            final DateTime startTime = timeStorageFormat.withZoneUTC().parseDateTime(activityData.getString("startTime"));
            final DateTime endTime = timeStorageFormat.withZoneUTC().parseDateTime(activityData.getString("endTime"));
            activity.startTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.print(startTime);
            activity.endTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.print(endTime);
            activity.start = startTime.getMillis();
            activity.end = endTime.getMillis();
            activity.steps = activityData.getInt("steps");
            activity.distance = activityData.getInt("distance");
            extractTrackPoints(activity.activityId, activityData);
        }
    }

    private void extractTrackPoints(final String activityId, final JSONObject activityData) {
        final JSONArray trackPoints = activityData.getJSONArray("trackPoints");
        List<LocationFacet> locationFacets = new ArrayList<LocationFacet>();
        for (int i=0; i<trackPoints.size(); i++) {
            JSONObject trackPoint = trackPoints.getJSONObject(i);
            LocationFacet locationFacet = new LocationFacet(updateInfo.apiKey.getId());
            locationFacet.isLocalTime = true;
            locationFacet.latitude = (float) trackPoint.getDouble("lat");
            locationFacet.longitude = (float) trackPoint.getDouble("lon");
            final DateTime time = timeStorageFormat.withZoneUTC().parseDateTime(activityData.getString("time"));
            locationFacet.timestampMs = time.getMillis();
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
