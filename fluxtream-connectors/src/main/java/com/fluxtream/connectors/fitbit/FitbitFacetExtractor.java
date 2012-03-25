package com.fluxtream.connectors.fitbit;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import com.fluxtream.services.MetadataService;

@Component
public class FitbitFacetExtractor extends AbstractFacetExtractor {

	Logger logger = Logger.getLogger(FitbitFacetExtractor.class);

	@Autowired
	MetadataService metadataService;

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
		else
			logger.info("guestId=" + apiData.updateInfo.getGuestId() +
					" connector=fitbit action=extractFacets error=no such objectType");

		return facets;
	}
	
	public void updateWeekFacets() {
		
	}
	
	public void updateMonthFacets() {
		
	}

	private void extractSummaryActivityInfo(ApiData apiData,
			JSONObject fitbitResponse, List<AbstractFacet> facets) {
		long guestId = apiData.updateInfo.getGuestId();
		logger.info("guestId=" + guestId +
				" connector=fitbit action=extractSummaryActivityInfo");

		FitbitActivityFacet facet = new FitbitActivityFacet();

		JSONObject fitbitSummary = fitbitResponse.getJSONObject("summary");

		super.extractCommonFacetData(facet, apiData);
		TimeZone timeZone = metadataService.getTimeZone(guestId, apiData.start);
		logger.info(
				"guestId=" + guestId +
				" connector=fitbit action=extractSummaryActivityInfo time="
						+ apiData.start + " timeZone="
						+ timeZone.getDisplayName() + " date="
						+ apiData.getDate(timeZone));
		facet.date = apiData.getDate(timeZone);

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

			facet.date = apiData.getDate(metadataService.getTimeZone(
					apiData.updateInfo.getGuestId(), apiData.start));

			Date startDate = getStartTime(
					loggedActivity.getString("startTime"), 0);
			facet.start = startDate.getTime();
			if (loggedActivity.containsKey("duration"))
				facet.end = facet.start + loggedActivity.getInt("duration");
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

	Date getStartTime(String s, long t) {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.setTimeInMillis(t);
		String[] splits = s.split(":");
		int hour = Integer.valueOf(splits[0]);
		int minutes = Integer.valueOf(splits[1]);
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minutes);
		return calendar.getTime();
	}

}
