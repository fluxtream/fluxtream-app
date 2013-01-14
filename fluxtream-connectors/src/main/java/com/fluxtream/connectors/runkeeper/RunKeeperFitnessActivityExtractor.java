package com.fluxtream.connectors.runkeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import com.fluxtream.services.MetadataService;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.velocity.util.ArrayListWrapper;
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

    @Override
    public List<AbstractFacet> extractFacets(final ApiData apiData, final ObjectType objectType) throws Exception {
        List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        JSONObject jsonObject = JSONObject.fromObject(apiData.json);

        RunKeeperFitnessActivityFacet facet = new RunKeeperFitnessActivityFacet();

        if (jsonObject.has("path")) {
            final JSONArray path = jsonObject.getJSONArray("path");
            facet.latitudes = new double[path.size()];
            facet.longitudes = new double[path.size()];
            facet.altitudes = new double[path.size()];
            facet.timestamps = new long[path.size()];
            for (int i=0; i<path.size(); i++) {
                JSONObject pathElement = path.getJSONObject(i);
                facet.latitudes[i] = pathElement.getDouble("latitude");
                facet.longitudes[i] = pathElement.getDouble("longitude");
                facet.timestamps[i] = pathElement.getLong("timestamp");
                facet.altitudes[i] = pathElement.getDouble("altitude");
            }
        } else {
            //TODO: abort if we don't have gps data as we are unable to figure out time
            //in this case
        }

        final String start_time = jsonObject.getString("start_time");
        final TimeZone timeZone = metadataService.getTimeZone(facet.latitudes[0], facet.longitudes[0]);
        facet.start = timeFormatter.withZone(DateTimeZone.forTimeZone(timeZone)).parseMillis(start_time);
        final int duration = jsonObject.getInt("duration");
        facet.end = facet.start + duration*1000;

        facet.userID = jsonObject.getString("userID");
        facet.duration = jsonObject.getInt("duration");
        facet.type = jsonObject.getString("type");
        facet.equipment = jsonObject.getString("equipment");
        facet.total_distance = jsonObject.getDouble("total_distance");
        facet.total_climb = jsonObject.getDouble("total_climb");


        if (jsonObject.has("distance")) {
            final JSONArray distanceArray = jsonObject.getJSONArray("distance");
            facet.distance = new double[distanceArray.size()];
            for (int i=0; i<distanceArray.size(); i++)
                facet.distance[i] = distanceArray.getDouble(i);
        }
        if (jsonObject.has("heart_rate")) {
            final JSONArray heart_rateArray = jsonObject.getJSONArray("heart_rate");
            facet.heart_rate = new double[heart_rateArray.size()];
            for (int i=0; i<heart_rateArray.size(); i++)
                facet.heart_rate[i] = heart_rateArray.getDouble(i);
        }
        if (jsonObject.has("calories")) {
            final JSONArray caloriesArray = jsonObject.getJSONArray("calories");
            facet.calories = new double[caloriesArray.size()];
            for (int i=0; i<caloriesArray.size(); i++)
                facet.calories[i] = caloriesArray.getDouble(i);
        }

        facets.add(facet);

        return facets;
    }
}
