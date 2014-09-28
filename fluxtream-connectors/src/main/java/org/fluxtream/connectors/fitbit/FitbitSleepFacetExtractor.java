package org.fluxtream.connectors.fitbit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.fluxtream.core.ApiData;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;
import org.fluxtream.core.facets.extractors.AbstractFacetExtractor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class FitbitSleepFacetExtractor extends AbstractFacetExtractor {

	public List<AbstractFacet> extractFacets(final UpdateInfo updateInfo, ApiData apiData,
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

            facet.date = (String) apiData.updateInfo.getContext("date");
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

			facets.add(facet);
		}

		return facets;
	}

    public static void main(final String[] args) {
        String s = "2012-11-07T03:13:00.000";
        final long startTimeMillis = AbstractLocalTimeFacet.timeStorageFormat.withZoneUTC().parseMillis(s);
        long endTimeMillis = startTimeMillis+3600000;
        final String endTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.withZoneUTC().print(endTimeMillis);
        System.out.println(startTimeMillis);
        System.out.println(endTimeStorage);
    }
}
