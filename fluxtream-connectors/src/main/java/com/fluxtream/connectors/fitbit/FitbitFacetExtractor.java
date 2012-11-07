package com.fluxtream.connectors.fitbit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class FitbitFacetExtractor extends AbstractFacetExtractor {

	Logger logger = Logger.getLogger(FitbitFacetExtractor.class);

	public List<AbstractFacet> extractFacets(ApiData apiData,
			ObjectType objectType) {
		List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
		JSONObject fitbitResponse = JSONObject.fromObject(apiData.json);

		logger.info("guestId=" + apiData.updateInfo.getGuestId() +
				" connector=fitbit action=extractFacets objectType="
						+ objectType.getName());

		if (objectType.getName().equals("activity_summary"))
			extractSummaryActivityInfo(apiData, fitbitResponse, facets);
		else if (objectType.getName().equals("logged_activity"))
			extractLoggedActivities(apiData, fitbitResponse, facets);
        else if (objectType.getName().equals("weight"))
            extractWeightInfo(apiData, fitbitResponse, facets);
        else
			logger.info("guestId=" + apiData.updateInfo.getGuestId() +
					" connector=fitbit action=extractFacets error=no such objectType");

		return facets;
	}

    private void extractWeightInfo(final ApiData apiData, final JSONObject fitbitResponse, final List<AbstractFacet> facets) {
        long guestId = apiData.updateInfo.getGuestId();
        logger.info("guestId=" + guestId +
                    " connector=fitbit action=extractSummaryActivityInfo");

        JSONArray fitbitWeightMeasurements = fitbitResponse.getJSONArray("weight");

        logger.info(
                "guestId=" + guestId +
                " connector=fitbit action=extractWeightInfo");

        for(int i=0; i<fitbitWeightMeasurements.size(); i++) {
            FitbitWeightFacet facet = new FitbitWeightFacet();
            super.extractCommonFacetData(facet, apiData);

            facet.date = (String) apiData.updateInfo.getContext("date");
            facet.startTimeStorage = facet.endTimeStorage = noon(facet.date);
    
            if (fitbitWeightMeasurements.getJSONObject(i).containsKey("bmi"))
                facet.bmi = fitbitWeightMeasurements.getJSONObject(i).getDouble("bmi");
            //if (fitbitWeightMeasurements.getJSONObject(i).containsKey("fat"))
            //    facet.fat = fitbitWeightMeasurements.getJSONObject(i).getDouble("fat");
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
                facet.startTimeStorage = facet.endTimeStorage = toTimeStorage(year, month, day, hours, minutes, seconds);
            }

            facets.add(facet);
        }
    }

    private void extractSummaryActivityInfo(ApiData apiData,
			JSONObject fitbitResponse, List<AbstractFacet> facets) {
		long guestId = apiData.updateInfo.getGuestId();
		logger.info("guestId=" + guestId +
				" connector=fitbit action=extractSummaryActivityInfo");

		FitbitActivityFacet facet = new FitbitActivityFacet();

		JSONObject fitbitSummary = fitbitResponse.getJSONObject("summary");

		super.extractCommonFacetData(facet, apiData);
		logger.info("guestId=" + guestId +
                    " connector=fitbit action=extractSummaryActivityInfo");
		facet.date = (String) apiData.updateInfo.getContext("date");
        facet.startTimeStorage = facet.endTimeStorage = noon(facet.date);

		if (fitbitSummary.containsKey("activeScore"))
			facet.activeScore = fitbitSummary.getInt("activeScore");
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

		facets.add(facet);
	}

	private void extractLoggedActivities(ApiData apiData,
			JSONObject fitbitResponse, List<AbstractFacet> facets) {
		logger.info("guestId=" + apiData.updateInfo.getGuestId() +
				" connector=fitbit action=extractLoggedActivities");

		JSONArray loggedActivities = fitbitResponse.getJSONArray("activities");

		if (loggedActivities == null || loggedActivities.size() == 0)
			return;

		@SuppressWarnings("rawtypes")
		Iterator iterator = loggedActivities.iterator();
		while (iterator.hasNext()) {
			JSONObject loggedActivity = (JSONObject) iterator.next();

			FitbitLoggedActivityFacet facet = new FitbitLoggedActivityFacet();
			super.extractCommonFacetData(facet, apiData);

            facet.date = (String) apiData.updateInfo.getContext("date");

            final String startTime = loggedActivity.getString("startTime");
            facet.startTimeStorage = facet.endTimeStorage = facet.date+"T" + startTime+":00.000";
			if (loggedActivity.containsKey("duration")) {
                final int duration = loggedActivity.getInt("duration");
                facet.duration = duration;
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

			facets.add(facet);
		}

	}

}
