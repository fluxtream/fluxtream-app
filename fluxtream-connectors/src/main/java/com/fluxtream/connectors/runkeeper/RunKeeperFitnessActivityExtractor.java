package com.fluxtream.connectors.runkeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.google_latitude.LocationFacet;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.MetadataService;
import com.google.api.client.util.Key;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.velocity.util.ArrayListWrapper;
import org.hibernate.annotations.Index;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component
public class RunKeeperFitnessActivityExtractor extends AbstractFacetExtractor {

    final DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss");

    @Autowired
    MetadataService metadataService;

    @Autowired
    ApiDataService apiDataService;

    @Override
    public List<AbstractFacet> extractFacets(final ApiData apiData, final ObjectType objectType) throws Exception {
        List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        JSONObject jsonObject = JSONObject.fromObject(apiData.json);

        RunKeeperFitnessActivityFacet facet = new RunKeeperFitnessActivityFacet();

        super.extractCommonFacetData(facet, apiData);

        String uri = jsonObject.getString("uri") + "|" + jsonObject.getString("userID");

        boolean startTimeSet = false;
        if (jsonObject.has("path")) {
            final JSONArray path = jsonObject.getJSONArray("path");
            for (int i=0; i<path.size(); i++) {
                JSONObject pathElement = path.getJSONObject(i);
                LocationFacet locationFacet = new LocationFacet();
                locationFacet.latitude = (float) pathElement.getDouble("latitude");
                locationFacet.longitude = (float) pathElement.getDouble("longitude");
                if (!startTimeSet) {
                    // we need to know the user's location in order to figure out
                    // his timezone
                    final String start_time = jsonObject.getString("start_time");
                    final TimeZone timeZone = metadataService.getTimeZone(locationFacet.latitude, locationFacet.longitude);
                    facet.start = timeFormatter.withZone(DateTimeZone.forTimeZone(timeZone)).parseMillis(start_time);
                    facet.timeZone = timeZone.getID();
                    final int duration = jsonObject.getInt("duration");
                    facet.end = facet.start + duration*1000;
                    facet.duration = duration;
                    startTimeSet = true;
                }
                locationFacet.altitude = (int) pathElement.getDouble("altitude");
                locationFacet.timestampMs = facet.start + pathElement.getLong("timestamp");
                locationFacet.source = LocationFacet.Source.RUNKEEPER;
                locationFacet.uri = uri;

                apiDataService.addGuestLocation(updateInfo.getGuestId(), locationFacet);
            }
        } else {
            //TODO: abort elegantly if we don't have gps data as we are unable to figure out time
            //in this case
            return facets;
        }

        facet.userID = jsonObject.getString("userID");
        facet.duration = jsonObject.getInt("duration");
        facet.type = jsonObject.getString("type");
        facet.equipment = jsonObject.getString("equipment");
        facet.total_distance = jsonObject.getDouble("total_distance");
        facet.is_live = jsonObject.getBoolean("is_live");
        facet.comments = jsonObject.getString("comments");
        if (jsonObject.has("total_climb"))
            facet.total_climb = jsonObject.getDouble("total_climb");


        if (jsonObject.has("heart_rate")) {
            final JSONArray heart_rateArray = jsonObject.getJSONArray("heart_rate");
            HeartRateMeasure heartRateMeasure = new HeartRateMeasure();
            facet.heart_rate = new ArrayList<HeartRateMeasure>(heart_rateArray.size());
            for (int i=0; i<heart_rateArray.size(); i++) {
                final JSONObject heartRateTuple = heart_rateArray.getJSONObject(i);
                heartRateMeasure.timestamp = heartRateTuple.getDouble("timestamp");
                heartRateMeasure.heartRate = heartRateTuple.getDouble("heart_rate");
                facet.heart_rate.add(heartRateMeasure);
            }
        }
        if (jsonObject.has("calories")) {
            // ignore calories for now
        }

        facets.add(facet);

        return facets;
    }
}
