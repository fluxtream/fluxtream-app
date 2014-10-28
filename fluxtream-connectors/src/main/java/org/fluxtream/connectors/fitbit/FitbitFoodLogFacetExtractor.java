package org.fluxtream.connectors.fitbit;

import net.sf.json.JSONArray;
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
import java.util.Iterator;
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

        if (objectType.getName().equals("activity_summary"))
            extractFoodLogSummaryInfo(apiData, fitbitResponse, facets);
        else if (objectType.getName().equals("logged_activity"))
            extractFoodLogEntries(apiData, fitbitResponse, facets);
        else
            logger.info("guestId=" + apiData.updateInfo.getGuestId() +
                    " connector=fitbit action=extractFacets error=no such objectType");

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

    private void extractFoodLogEntries(ApiData apiData, JSONObject fitbitResponse, List<AbstractFacet> facets) {
        logger.info("guestId=" + apiData.updateInfo.getGuestId() +
                " connector=fitbit action=extractFoodLogEntries");

        JSONArray foodEntries = fitbitResponse.getJSONArray("foods");

        if (foodEntries == null || foodEntries.size() == 0)
            return;

        Iterator iterator = foodEntries.iterator();
        while (iterator.hasNext()) {
            JSONObject foodEntry = (JSONObject) iterator.next();

            FitbitFoodLogEntryFacet facet = new FitbitFoodLogEntryFacet(apiData.updateInfo.apiKey.getId());
            super.extractCommonFacetData(facet, apiData);

            setFacetTimeBounds(apiData, facet);

            facet.isFavorite = foodEntry.getBoolean("isFavorite");
            facet.logId = foodEntry.getLong("logId");
            if (foodEntry.has("loggedFood")) {
                JSONObject loggedFood = foodEntry.getJSONObject("loggedFood");
                if (loggedFood.has("accessLevel"))
                    facet.accessLevel = loggedFood.getString("accessLevel");
                if (loggedFood.has("amount"))
                    facet.amount = (float) loggedFood.getDouble("accessLevel");
                if (loggedFood.has("brand"))
                    facet.brand = loggedFood.getString("brand");
                if (loggedFood.has("calories"))
                    facet.calories = loggedFood.getInt("calories");
                if (loggedFood.has("foodId"))
                    facet.foodId = loggedFood.getLong("foodId");
                if (loggedFood.has("mealTypeId"))
                    facet.mealTypeId = loggedFood.getInt("mealTypeId");
                if (loggedFood.has("locale"))
                    facet.locale = loggedFood.getString("locale");
                if (loggedFood.has("name"))
                    facet.name = loggedFood.getString("name");
                if (loggedFood.has("unit")) {
                    JSONObject unit = loggedFood.getJSONObject("unit");
                    facet.unitId = unit.getInt("id");
                    facet.unitName = unit.getString("name");
                    facet.unitPlural = unit.getString("plural");
                }
            }
            if (foodEntry.has("nutritionalValues ")) {
                JSONObject nutritionalValues = foodEntry.getJSONObject("nutrionalValues");
                if (nutritionalValues.has("calories"))
                    facet.NV_Calories = nutritionalValues.getInt("calories");
                if (nutritionalValues.has("carbs"))
                    facet.NV_Carbs = nutritionalValues.getInt("carbs");
                if (nutritionalValues.has("fat"))
                    facet.NV_Fat = nutritionalValues.getInt("fat");
                if (nutritionalValues.has("fiber"))
                    facet.NV_Fiber = nutritionalValues.getInt("fiber");
                if (nutritionalValues.has("protein"))
                    facet.NV_Protein = nutritionalValues.getInt("protein");
                if (nutritionalValues.has("sodium"))
                    facet.NV_Sodium = nutritionalValues.getInt("sodium");
            }
        }

    }

}
