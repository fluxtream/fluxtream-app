package com.fluxtream.connectors.fitbit;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.AbstractFloatingTimeZoneFacet;
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

		@SuppressWarnings("rawtypes")
		Iterator iterator = sleepRecords.iterator();
		while (iterator.hasNext()) {
			JSONObject record = (JSONObject) iterator.next();

			FitbitSleepFacet facet = new FitbitSleepFacet(apiData.updateInfo.apiKey.getId());

            int duration = record.getInt("duration");
            if (duration==0)
                continue;

			super.extractCommonFacetData(facet, apiData);
			String startTime = record.getString("startTime");
            facet.duration = duration;

			if (record.containsKey("minutesAwake"))
				facet.minutesAwake = record.getInt("minutesAwake");
			if (record.containsKey("minutesAsleep"))
				facet.minutesAsleep = record.getInt("minutesAsleep");
			if (record.containsKey("minutesToFallAsleep"))
				facet.minutesToFallAsleep = record
						.getInt("minutesToFallAsleep");
			Date startDate;
            facet.date = (String) apiData.updateInfo.getContext("date");
            facet.startTimeStorage = startTime;
            final long startTimeMillis = AbstractFloatingTimeZoneFacet.timeStorageFormat.parseMillis(startTime);
            final long endTimeMillis = startTimeMillis + duration;
            facet.endTimeStorage = AbstractFloatingTimeZoneFacet.timeStorageFormat.print(endTimeMillis);

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

    public static void main(final String[] args) {
        String s = "2012-11-07T03:13:00.000";
        final long startTimeMillis = AbstractFloatingTimeZoneFacet.timeStorageFormat.parseMillis(s);
        long endTimeMillis = startTimeMillis + 26820000;
        final String endTimeStorage = AbstractFloatingTimeZoneFacet.timeStorageFormat.print(endTimeMillis);
        System.out.println(startTimeMillis);
        System.out.println(endTimeStorage);
    }
}
