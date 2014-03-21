package org.fluxtream.connectors.fitbit;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import org.fluxtream.ApiData;
import org.fluxtream.aspects.FlxLogger;
import org.fluxtream.connectors.ObjectType;
import org.fluxtream.connectors.updaters.UpdateInfo;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.facets.extractors.AbstractFacetExtractor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component
public class FitbitWeightFacetExtractor extends AbstractFacetExtractor {

    FlxLogger logger = FlxLogger.getLogger(FitbitActivityFacetExtractor.class);

    @Override
    public List<AbstractFacet> extractFacets(final UpdateInfo updateInfo, final ApiData apiData,
                                             final ObjectType objectType) throws Exception {
        List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        JSONObject fitbitResponse = JSONObject.fromObject(apiData.json);

        logger.info("guestId=" + apiData.updateInfo.getGuestId() +
                    " connector=fitbit action=extractFacets objectType="
                    + objectType.getName());

        extractWeightInfo(apiData, fitbitResponse, facets);

        return facets;
    }

    private void extractWeightInfo(final ApiData apiData, final JSONObject fitbitResponse, final List<AbstractFacet> facets) {
        long guestId = apiData.updateInfo.getGuestId();

        JSONArray fitbitWeightMeasurements = fitbitResponse.getJSONArray("weight");

        logger.info(
                "guestId=" + guestId +
                " connector=fitbit action=extractWeightInfo");

        for(int i=0; i<fitbitWeightMeasurements.size(); i++) {
            FitbitWeightFacet facet = new FitbitWeightFacet(apiData.updateInfo.apiKey.getId());
            super.extractCommonFacetData(facet, apiData);

            facet.date = (String) apiData.updateInfo.getContext("date");
            facet.startTimeStorage = facet.endTimeStorage = noon(facet.date);

            if (fitbitWeightMeasurements.getJSONObject(i).containsKey("bmi"))
                facet.bmi = fitbitWeightMeasurements.getJSONObject(i).getDouble("bmi");
            if (fitbitWeightMeasurements.getJSONObject(i).containsKey("fat"))
                facet.fat = fitbitWeightMeasurements.getJSONObject(i).getDouble("fat");
            if (fitbitWeightMeasurements.getJSONObject(i).containsKey("weight"))
                facet.weight = fitbitWeightMeasurements.getJSONObject(i).getDouble("weight");
            if (fitbitWeightMeasurements.getJSONObject(i).containsKey("time")) {
                String time = fitbitWeightMeasurements.getJSONObject(i).getString("time");
                String[] timeParts = time.split(":");
                int hours = Integer.valueOf(timeParts[0]);
                int minutes = Integer.valueOf(timeParts[1]);
                int seconds = Integer.valueOf(timeParts[2]);
                String[] dateParts = facet.date.split("-");
                int year = Integer.valueOf(dateParts[0]);
                int month = Integer.valueOf(dateParts[1]);
                int day = Integer.valueOf(dateParts[2]);
                Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                c.set(year, month-1, day, hours, minutes, seconds);
                c.set(Calendar.MILLISECOND, 0);
                facet.start = facet.end = c.getTimeInMillis();
                facet.startTimeStorage = facet.endTimeStorage = toTimeStorage(year, month, day, hours, minutes, seconds);
            }

            facets.add(facet);
        }
    }
}
