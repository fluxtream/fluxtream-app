package org.fluxtream.connectors.zeo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import org.fluxtream.core.ApiData;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.facets.extractors.AbstractFacetExtractor;
import org.fluxtream.core.services.MetadataService;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ZeoSleepStatsFacetExtractor extends AbstractFacetExtractor {

    @Qualifier("metadataServiceImpl")
    @Autowired
	MetadataService metadataService;

	public List<AbstractFacet> extractFacets(final UpdateInfo updateInfo, final ApiData apiData,
			ObjectType objectType) {
		List<AbstractFacet> facets = new ArrayList<AbstractFacet>();

		JSONObject response = JSONObject.fromObject(apiData.json).getJSONObject("response");

		if (response.has("sleepRecord")) {
			JSONObject sleepRecords = response.optJSONObject("sleepRecord");
            extractStatsData(facets, apiData, sleepRecords);
		} else if (response.has("sleepStats")) {
			extractStatsData(facets, apiData, response.getJSONObject("sleepStats"));
		}

		return facets;
	}

	private void extractStatsData(List<AbstractFacet> facets, ApiData apiData,
			JSONObject sleepStats) {
		ZeoSleepStatsFacet facet = new ZeoSleepStatsFacet(apiData.updateInfo.apiKey.getId());
		super.extractCommonFacetData(facet, apiData);
		facet.zq = sleepStats.getInt("zq");
        parseZeoTime(sleepStats, "bedTime", facet);
        parseZeoTime(sleepStats, "riseTime", facet);
		facet.morningFeel = sleepStats.getInt("morningFeel");
		facet.totalZ = sleepStats.getInt("totalZ");
		facet.timeInDeepPercentage = sleepStats.getInt("timeInDeepPercentage");
		facet.timeInLightPercentage = sleepStats .getInt("timeInLightPercentage");
		facet.timeInRemPercentage = sleepStats.getInt("timeInRemPercentage");
		facet.timeInWakePercentage = sleepStats.getInt("timeInWakePercentage");
		facet.awakenings = sleepStats.getInt("awakenings");
		facet.timeToZ = sleepStats.getInt("timeToZ");
		facet.sleepGraph = getSleepGraph(sleepStats);
		facets.add(facet);
	}
	
	private enum Phase {
		UNDEFINED, WAKE, REM, LIGHT, DEEP
    }

	private String getSleepGraph(JSONObject sleepStats) {
		JSONArray sleepGraphArray = sleepStats.getJSONArray("sleepGraph");
		StringBuilder bf = new StringBuilder();
		for (int i=0; i<sleepGraphArray.size(); i++) {
			String phaseString = sleepGraphArray.getString(i);
			Phase phase = Enum.valueOf(Phase.class, phaseString);
			bf.append(String.valueOf(phase.ordinal()));
		}
		return bf.toString();
	}

	private void parseZeoTime(JSONObject stats, String key, ZeoSleepStatsFacet facet) {
		JSONObject o = stats.getJSONObject(key);
		
		int day = o.getInt("day");
		int month = o.getInt("month");
		int year = o.getInt("year");
		int hours = o.getInt("hour");
		int minutes = o.getInt("minute");
		int seconds = o.getInt("second");

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.set(Calendar.MILLISECOND, 0);
        c.set(year, month-1, day, hours, minutes, seconds);
		if (key.equals("bedTime")) {
            facet.startTimeStorage = toTimeStorage(year, month, day, hours, minutes, seconds);
            facet.start = c.getTimeInMillis();
        } else {
            facet.date = (new StringBuilder()).append(year)
                    .append("-").append(pad(month)).append("-").append(pad(day)).toString();
            facet.endTimeStorage = toTimeStorage(year, month, day, hours, minutes, seconds);
            facet.end = c.getTimeInMillis();
        }

	}

}
