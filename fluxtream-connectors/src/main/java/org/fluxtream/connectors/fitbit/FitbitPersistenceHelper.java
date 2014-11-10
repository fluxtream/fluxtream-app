package org.fluxtream.connectors.fitbit;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;
import org.fluxtream.core.facets.extractors.AbstractFacetExtractor;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.utils.TimeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * User: candide
 * Date: 06/11/14
 * Time: 21:49
 */
@Component
public class FitbitPersistenceHelper {

    @Autowired
    ApiDataService apiDataService;

    List<FitbitWeightFacet> createOrUpdateWeightFacets(final UpdateInfo updateInfo, final JSONObject fitbitResponse) throws Exception {
        JSONArray fitbitWeightMeasurements = fitbitResponse.getJSONArray("weight");
        List<FitbitWeightFacet> weightFacets = new ArrayList<FitbitWeightFacet>();
        for(int i=0; i<fitbitWeightMeasurements.size(); i++) {
            final FitbitWeightFacet createdOrUpdatedWeightFacet = createOrUpdateWeightFacet(updateInfo, fitbitWeightMeasurements.getJSONObject(i));
            if (createdOrUpdatedWeightFacet!=null)
                weightFacets.add(createdOrUpdatedWeightFacet);
        }
        return weightFacets;
    }

    private FitbitWeightFacet createOrUpdateWeightFacet(final UpdateInfo updateInfo, final JSONObject fitbitWeightMeasurement) throws Exception {
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery(
                "e.apiKeyId=? AND e.logId=?",
                updateInfo.apiKey.getId(), fitbitWeightMeasurement.getLong("logId"));

        final ApiDataService.FacetModifier<FitbitWeightFacet> facetModifier = new ApiDataService.FacetModifier<FitbitWeightFacet>() {
            @Override
            public FitbitWeightFacet createOrModify(FitbitWeightFacet facet, Long apiKeyId) throws Exception {
                if (facet == null) {
                    facet = new FitbitWeightFacet(updateInfo.apiKey.getId());
                }
                AbstractUpdater.extractCommonFacetData(facet, updateInfo);

                facet.date = (String) updateInfo.getContext("date");
                facet.startTimeStorage = facet.endTimeStorage = AbstractFacetExtractor.noon(facet.date);

                if (fitbitWeightMeasurement.containsKey("logId"))
                    facet.logId = fitbitWeightMeasurement.getLong("logId");
                if (fitbitWeightMeasurement.containsKey("bmi"))
                    facet.bmi = fitbitWeightMeasurement.getDouble("bmi");
                if (fitbitWeightMeasurement.containsKey("fat"))
                    facet.fat = fitbitWeightMeasurement.getDouble("fat");
                if (fitbitWeightMeasurement.containsKey("weight"))
                    facet.weight = fitbitWeightMeasurement.getDouble("weight");
                if (fitbitWeightMeasurement.containsKey("time")) {
                    String time = fitbitWeightMeasurement.getString("time");
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
                    facet.startTimeStorage = facet.endTimeStorage = AbstractFacetExtractor.toTimeStorage(year, month, day, hours, minutes, seconds);
                }
                return facet;
            }
        };
        return apiDataService.createOrReadModifyWrite(FitbitWeightFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
   }


    FitbitFoodLogSummaryFacet createOrUpdateFoodLogSummaryFacet(final UpdateInfo updateInfo, final JSONObject fitbitResponse) throws Exception {
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery(
                "e.apiKeyId=? AND e.date=?",
                updateInfo.apiKey.getId(), updateInfo.getContext("date"));
        final ApiDataService.FacetModifier<FitbitFoodLogSummaryFacet> facetModifier = new ApiDataService.FacetModifier<FitbitFoodLogSummaryFacet>() {
            @Override
            public FitbitFoodLogSummaryFacet createOrModify(FitbitFoodLogSummaryFacet facet, Long apiKeyId) throws Exception {
                if (facet == null) {
                    facet = new FitbitFoodLogSummaryFacet(updateInfo.apiKey.getId());
                }
                AbstractUpdater.extractCommonFacetData(facet, updateInfo);
                setFacetTimeBounds(updateInfo, facet);

                JSONObject fitbitSummary = fitbitResponse.getJSONObject("summary");

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
                return facet;
            }
        };
        return apiDataService.createOrReadModifyWrite(FitbitFoodLogSummaryFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    void createOrUpdateFoodEntry(final UpdateInfo updateInfo, final JSONObject foodEntry) throws Exception {
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery(
                "e.apiKeyId=? AND e.logId=?",
                updateInfo.apiKey.getId(), foodEntry.getLong("logId"));
        final ApiDataService.FacetModifier<FitbitFoodLogEntryFacet> facetModifier = new ApiDataService.FacetModifier<FitbitFoodLogEntryFacet>() {

            @Override
            public FitbitFoodLogEntryFacet createOrModify(FitbitFoodLogEntryFacet facet, Long apiKeyId) throws Exception {
                if (facet == null) {
                    facet = new FitbitFoodLogEntryFacet(updateInfo.apiKey.getId());
                }

                AbstractUpdater.extractCommonFacetData(facet, updateInfo);
                setFacetTimeBounds(updateInfo, facet);

                facet.isFavorite = foodEntry.getBoolean("isFavorite");
                facet.logId = foodEntry.getLong("logId");
                if (foodEntry.has("loggedFood")) {
                    JSONObject loggedFood = foodEntry.getJSONObject("loggedFood");
                    if (loggedFood.has("accessLevel"))
                        facet.accessLevel = loggedFood.getString("accessLevel");
                    if (loggedFood.has("amount"))
                        facet.amount = (float) loggedFood.getDouble("amount");
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
                if (foodEntry.has("nutritionalValues")) {
                    JSONObject nutritionalValues = foodEntry.getJSONObject("nutritionalValues");
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
                return facet;
            }

        };
        apiDataService.createOrReadModifyWrite(FitbitFoodLogEntryFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    void createOrUpdateLoggedActivities(final UpdateInfo updateInfo, final JSONObject fitbitResponse) throws Exception {
        JSONArray loggedActivities = fitbitResponse.getJSONArray("activities");

        if (loggedActivities == null || loggedActivities.size() == 0)
            return;

        for (int i=0; i<loggedActivities.size(); i++)
            createOrUpdateLoggedActivity(updateInfo, loggedActivities.getJSONObject(i));
    }

    private void createOrUpdateLoggedActivity(final UpdateInfo updateInfo, final JSONObject loggedActivity) throws Exception {
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery(
                "e.apiKeyId=? AND e.logId=?",
                updateInfo.apiKey.getId(), loggedActivity.getLong("logId"));
        final ApiDataService.FacetModifier<FitbitLoggedActivityFacet> facetModifier = new ApiDataService.FacetModifier<FitbitLoggedActivityFacet>() {
            @Override
            public FitbitLoggedActivityFacet createOrModify(FitbitLoggedActivityFacet facet, Long apiKeyId) throws Exception {
                if (facet == null)
                    facet = new FitbitLoggedActivityFacet(updateInfo.apiKey.getId());
                AbstractUpdater.extractCommonFacetData(facet, updateInfo);

                facet.date = (String) updateInfo.getContext("date");

                final String startTime = loggedActivity.getString("startTime");

                facet.startTimeStorage = facet.endTimeStorage = facet.date+"T" + startTime+":00.000";
                final DateTime startDateTime = AbstractLocalTimeFacet.timeStorageFormat.withZoneUTC().parseDateTime(facet.startTimeStorage);
                facet.start = startDateTime.getMillis();
                if (loggedActivity.containsKey("duration")) {
                    final int duration = loggedActivity.getInt("duration");
                    final DateTime endDateTime = startDateTime.plus(duration);
                    facet.endTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.withZoneUTC().print(endDateTime.getMillis());
                    facet.end = endDateTime.getMillis();
                }
                if (loggedActivity.containsKey("activityId"))
                    facet.activityId = loggedActivity.getLong("activityId");
                if (loggedActivity.containsKey("activityParentId"))
                    facet.activityParentId = loggedActivity
                            .getLong("activityParentId");
                if (loggedActivity.containsKey("calories"))
                    facet.calories = loggedActivity.getInt("calories");
                if (loggedActivity.containsKey("description"))
                    facet.fullTextDescription = loggedActivity
                            .getString("description");
                if (loggedActivity.containsKey("distance"))
                    facet.distance = loggedActivity.getInt("distance");
                if (loggedActivity.containsKey("isFavorite"))
                    facet.isFavorite = loggedActivity.getBoolean("isFavorite");
                if (loggedActivity.containsKey("logId"))
                    facet.logId = loggedActivity.getLong("logId");
                if (loggedActivity.containsKey("name"))
                    facet.name = loggedActivity.getString("name");
                if (loggedActivity.containsKey("steps"))
                    facet.steps = loggedActivity.getInt("steps");
                return facet;
            }
        };
        apiDataService.createOrReadModifyWrite(FitbitLoggedActivityFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    FitbitTrackerActivityFacet createOrUpdateActivitySummary(final UpdateInfo updateInfo, final JSONObject fitbitResponse) throws Exception {
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery(
                "e.apiKeyId=? AND e.date=?",
                updateInfo.apiKey.getId(), updateInfo.getContext("date"));
        final ApiDataService.FacetModifier<FitbitTrackerActivityFacet> facetModifier = new ApiDataService.FacetModifier<FitbitTrackerActivityFacet>() {
            @Override
            public FitbitTrackerActivityFacet createOrModify(FitbitTrackerActivityFacet facet, Long apiKeyId) throws Exception {
                if (facet==null)
                    facet = new FitbitTrackerActivityFacet(updateInfo.apiKey.getId());

                JSONObject fitbitSummary = fitbitResponse.getJSONObject("summary");

                AbstractUpdater.extractCommonFacetData(facet, updateInfo);

                facet.date = (String) updateInfo.getContext("date");

                final DateTime dateTime = TimeUtils.dateFormatterUTC.parseDateTime(facet.date);

                // returns the starting midnight for the date
                facet.start = dateTime.getMillis();
                facet.end = dateTime.getMillis()+ DateTimeConstants.MILLIS_PER_DAY-1;

                facet.startTimeStorage = facet.date + "T00:00:00.000";
                facet.endTimeStorage = facet.date + "T23:59:59.999";

                if (fitbitSummary.containsKey("activeScore"))
                    facet.activeScore = fitbitSummary.getInt("activeScore");
                if (fitbitSummary.containsKey("floors"))
                    facet.floors = fitbitSummary.getInt("floors");
                if (fitbitSummary.containsKey("elevation"))
                    facet.elevation = fitbitSummary.getInt("elevation");
                if (fitbitSummary.containsKey("caloriesOut"))
                    facet.caloriesOut = fitbitSummary.getInt("caloriesOut");
                if (fitbitSummary.containsKey("fairlyActiveMinutes"))
                    facet.fairlyActiveMinutes = fitbitSummary
                            .getInt("fairlyActiveMinutes");
                if (fitbitSummary.containsKey("lightlyActiveMinutes"))
                    facet.lightlyActiveMinutes = fitbitSummary
                            .getInt("lightlyActiveMinutes");
                if (fitbitSummary.containsKey("sedentaryMinutes"))
                    facet.sedentaryMinutes = fitbitSummary.getInt("sedentaryMinutes");
                if (fitbitSummary.containsKey("veryActiveMinutes"))
                    facet.veryActiveMinutes = fitbitSummary.getInt("veryActiveMinutes");
                if (fitbitSummary.containsKey("steps"))
                    facet.steps = fitbitSummary.getInt("steps");

                if (fitbitSummary.has("distances")) {
                    JSONArray distancesArray = fitbitSummary.getJSONArray("distances");
                    for (int i=0; i<distancesArray.size(); i++) {
                        JSONObject distanceObject = distancesArray.getJSONObject(i);
                        final String activityType = distanceObject.getString("activity");
                        final double distance = distanceObject.getDouble("distance");
                        if (activityType.equals("tracker"))
                            facet.trackerDistance = distance;
                        else if (activityType.equals("loggedActivities"))
                            facet.loggedActivitiesDistance = distance;
                        else if (activityType.equals("veryActive"))
                            facet.veryActiveDistance = distance;
                        else if (activityType.equals("total"))
                            facet.totalDistance = distance;
                        else if (activityType.equals("moderatelyActive"))
                            facet.moderatelyActiveDistance = distance;
                        else if (activityType.equals("lightlyActive"))
                            facet.lightlyActiveDistance = distance;
                        else if (activityType.equals("sedentary"))
                            facet.sedentaryActiveDistance = distance;
                    }
                }
                return facet;
            }
        };
        return apiDataService.createOrReadModifyWrite(FitbitTrackerActivityFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    void createOrUpdateSleepFacets(final UpdateInfo updateInfo, final JSONObject fitbitResponse) throws Exception {
        JSONArray sleepRecords = fitbitResponse.getJSONArray("sleep");
        Iterator iterator = sleepRecords.iterator();
        while (iterator.hasNext()) {
            JSONObject record = (JSONObject) iterator.next();
            createOrUpdateSleepFacet(updateInfo, record);
        }
    }

    private void createOrUpdateSleepFacet(final UpdateInfo updateInfo, final JSONObject record) throws Exception {
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery(
                "e.apiKeyId=? AND e.logId=?",
                updateInfo.apiKey.getId(), record.getLong("logId"));
        final ApiDataService.FacetModifier<FitbitSleepFacet> facetModifier = new ApiDataService.FacetModifier<FitbitSleepFacet>() {
            @Override
            public FitbitSleepFacet createOrModify(FitbitSleepFacet facet, Long apiKeyId) throws Exception {
                if (facet == null)
                    facet = new FitbitSleepFacet(updateInfo.apiKey.getId());
                int duration = record.getInt("duration");
                if (duration==0)
                    return null;

                AbstractUpdater.extractCommonFacetData(facet, updateInfo);
                String startTime = record.getString("startTime");
                facet.duration = duration;

                if (record.containsKey("minutesAwake"))
                    facet.minutesAwake = record.getInt("minutesAwake");
                if (record.containsKey("minutesAsleep"))
                    facet.minutesAsleep = record.getInt("minutesAsleep");
                if (record.containsKey("minutesToFallAsleep"))
                    facet.minutesToFallAsleep = record
                            .getInt("minutesToFallAsleep");

                facet.date = (String) updateInfo.getContext("date");
                final long startTimeMillis = AbstractLocalTimeFacet.timeStorageFormat.withZoneUTC().parseMillis(startTime);
                facet.start = startTimeMillis;
                facet.end = startTimeMillis + duration;
                facet.startTimeStorage = startTime;
                final long endTimeMillis = startTimeMillis + duration;
                facet.endTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.withZoneUTC().print(endTimeMillis);

                if (record.containsKey("awakeningsCount"))
                    facet.awakeningsCount = record.getInt("awakeningsCount");
                if (record.containsKey("timeInBed"))
                    facet.timeInBed = record.getInt("timeInBed");
                return facet;
            }
        };
        apiDataService.createOrReadModifyWrite(FitbitSleepFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    private void setFacetTimeBounds(final UpdateInfo updateInfo, AbstractLocalTimeFacet facet) {
        facet.date = (String) updateInfo.getContext("date");

        final DateTime dateTime = TimeUtils.dateFormatterUTC.parseDateTime(facet.date);

        // returns the starting midnight for the date
        facet.start = dateTime.getMillis();
        facet.end = dateTime.getMillis()+ DateTimeConstants.MILLIS_PER_DAY-1;

        facet.startTimeStorage = facet.date + "T00:00:00.000";
        facet.endTimeStorage = facet.date + "T23:59:59.999";
    }

}
