package org.fluxtream.connectors.fitbit;

import net.sf.json.JSONObject;
import org.fluxtream.core.ApiData;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;
import org.fluxtream.core.facets.extractors.AbstractFacetExtractor;
import org.fluxtream.core.utils.TimeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * User: candide
 * Date: 28/10/14
 * Time: 18:12
 */
@Component
public class FitbitFoodLogFacetExtractor extends AbstractFacetExtractor {

    FlxLogger logger = FlxLogger.getLogger(FitbitFoodLogFacetExtractor.class);

    @Override
    public List<AbstractFacet> extractFacets(UpdateInfo updateInfo, ApiData apiData, ObjectType objectType) throws Exception {
        List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        JSONObject fitbitResponse = JSONObject.fromObject(apiData.json);

        logger.info("guestId=" + apiData.updateInfo.getGuestId() +
                " connector=fitbit action=extractFacets objectType="
                + objectType.getName());

        if (objectType.getName().equals("food_log_summary"))
            extractFoodLogSummaryInfo(apiData, fitbitResponse, facets);

        return facets;
    }

    private void extractFoodLogSummaryInfo(ApiData apiData, JSONObject fitbitResponse, List<AbstractFacet> facets) {
        long guestId = apiData.updateInfo.getGuestId();
        logger.info("guestId=" + guestId +
                " connector=fitbit action=extractSummaryActivityInfo");

        FitbitFoodLogSummaryFacet facet = new FitbitFoodLogSummaryFacet(apiData.updateInfo.apiKey.getId());

        JSONObject fitbitSummary = fitbitResponse.getJSONObject("summary");

        super.extractCommonFacetData(facet, apiData);
        logger.info("guestId=" + guestId +
                " connector=fitbit action=extractFoodLogSummaryInfo");

        setFacetTimeBounds(apiData, facet);

        if (fitbitSummary.has("calories"))
            facet.calories = fitbitSummary.getInt("calories");
        if (fitbitSummary.has("carbs"))
            facet.carbs = (float) fitbitSummary.getDouble("carbs");
        if (fitbitSummary.has("fat"))
            facet.fat = (float) fitbitSummary.getDouble("fat");
        if (fitbitSummary.has("fiber"))
            facet.fiber = (float) fitbitSummary.getDouble("fiber");
        if (fitbitSummary.has("protein"))
            facet.protein = (float) fitbitSummary.getDouble("protein");
        if (fitbitSummary.has("sodium"))
            facet.sodium = (float) fitbitSummary.getDouble("sodium");
        if (fitbitSummary.has("water"))
            facet.water = (float) fitbitSummary.getDouble("water");

        if (fitbitResponse.has("goals")) {
            JSONObject goals = fitbitResponse.getJSONObject("goals");
            if (goals.has("calories"))
                facet.caloriesGoal = goals.getInt("calories");
            if (goals.has("estimatedCaloriesOut"))
                facet.caloriesOutGoal = goals.getInt("estimatedCaloriesOut");
        }

        facets.add(facet);
    }

    private void setFacetTimeBounds(ApiData apiData, AbstractLocalTimeFacet facet) {
        facet.date = (String) apiData.updateInfo.getContext("date");

        final DateTime dateTime = TimeUtils.dateFormatterUTC.parseDateTime(facet.date);

        // returns the starting midnight for the date
        facet.start = dateTime.getMillis();
        facet.end = dateTime.getMillis()+ DateTimeConstants.MILLIS_PER_DAY-1;

        facet.startTimeStorage = facet.date + "T00:00:00.000";
        facet.endTimeStorage = facet.date + "T23:59:59.999";
    }

}
