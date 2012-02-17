package com.fluxtream.connectors.fitbit;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import com.fluxtream.services.MetadataService;

@Component
public class FitbitSleepFacetExtractor extends AbstractFacetExtractor {

	@Autowired
	MetadataService metadataService;

	private final static DateTimeFormatter format = DateTimeFormat
			.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

	public List<AbstractFacet> extractFacets(ApiData apiData,
			ObjectType objectType) {
		List<AbstractFacet> facets = new ArrayList<AbstractFacet>();

		JSONObject fitbitResponse = JSONObject.fromObject(apiData.json);
		JSONArray sleepRecords = fitbitResponse.getJSONArray("sleep");

		if (sleepRecords == null || sleepRecords.size() == 0) {
			FitbitSleepFacet facet = new FitbitSleepFacet();
			super.extractCommonFacetData(facet, apiData);
			facet.isEmpty = true;
			facets.add(facet);
			return facets;
		}

		@SuppressWarnings("rawtypes")
		Iterator iterator = sleepRecords.iterator();
		while (iterator.hasNext()) {
			JSONObject record = (JSONObject) iterator.next();

			FitbitSleepFacet facet = new FitbitSleepFacet();

			super.extractCommonFacetData(facet, apiData);
			String startTime = record.getString("startTime");
			int duration = record.getInt("duration");

			TimeZone timeZone = getGuestTimeZoneForDate(apiData,
					startTime.substring(0, 10));
			storeTime(startTime, duration, facet, timeZone);

			if (record.containsKey("minutesAwake"))
				facet.minutesAwake = record.getInt("minutesAwake");
			if (record.containsKey("minutesAsleep"))
				facet.minutesAsleep = record.getInt("minutesAsleep");
			if (record.containsKey("minutesToFallAsleep"))
				facet.minutesToFallAsleep = record
						.getInt("minutesToFallAsleep");
			Date startDate;
			startDate = new Date(format.withZone(
					DateTimeZone.forTimeZone(timeZone)).parseMillis(startTime));
			facet.start = startDate.getTime();
			facet.end = facet.start + duration;
			if (record.containsKey("awakeningsCount"))
				facet.awakeningsCount = record.getInt("awakeningsCount");
			if (record.containsKey("timeInBed"))
				facet.timeInBed = record.getInt("timeInBed");

			facets.add(facet);
		}

		return facets;
	}

	private void storeTime(String startTime, int duration,
			FitbitSleepFacet facet, TimeZone timeZone) {
		facet.startTimeStorage = startTime;
		MutableDateTime bedTimeUTC = format.withZone(
				DateTimeZone.forTimeZone(timeZone)).parseMutableDateTime(
				startTime);
		bedTimeUTC.add(duration);
		if (startTime.length()>=10)
			facet.date = startTime.substring(0, 10);
		String riseTimeString = format.withZone(
				DateTimeZone.forTimeZone(timeZone)).print(
				bedTimeUTC.getMillis());
		facet.endTimeStorage = riseTimeString;
	}

	private TimeZone getGuestTimeZoneForDate(ApiData apiData, String date) {
		DayMetadataFacet dailyContextualInfo = metadataService
				.getDayMetadata(apiData.updateInfo.getGuestId(), date,
						true);
		return TimeZone.getTimeZone(dailyContextualInfo.timeZone);
	}

}
