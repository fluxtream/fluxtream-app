package com.fluxtream.connectors.fitbit;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class FitbitSleepFacetExtractor extends AbstractFacetExtractor {

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
            facet.duration = duration;

			if (record.containsKey("minutesAwake"))
				facet.minutesAwake = record.getInt("minutesAwake");
			if (record.containsKey("minutesAsleep"))
				facet.minutesAsleep = record.getInt("minutesAsleep");
			if (record.containsKey("minutesToFallAsleep"))
				facet.minutesToFallAsleep = record
						.getInt("minutesToFallAsleep");
			Date startDate;
            facet.date = getDate(startTime);
            facet.startTimeStorage = startTime;
            facet.endTimeStorage = startTime;

			if (record.containsKey("awakeningsCount"))
				facet.awakeningsCount = record.getInt("awakeningsCount");
			if (record.containsKey("timeInBed"))
				facet.timeInBed = record.getInt("timeInBed");

			facets.add(facet);
		}

		return facets;
	}

    private static String getDate(final String timeStr) {
        int i = timeStr.indexOf("T");
        return timeStr.substring(0, i);
    }
}
