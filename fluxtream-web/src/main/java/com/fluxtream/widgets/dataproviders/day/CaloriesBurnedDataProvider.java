package com.fluxtream.widgets.dataproviders.day;

import java.util.List;

import net.sf.json.JSONObject;

import org.springframework.stereotype.Component;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.fitbit.FitbitActivityFacet;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.widgets.dataproviders.AbstractWidgetDataProvider;

@Component("day/caloriesBurned")
public class CaloriesBurnedDataProvider extends AbstractWidgetDataProvider {

	@Override
	public JSONObject provideData(long guestId, GuestSettings settings, TimeInterval timeInterval) {
		JSONObject caloriesBurned = new JSONObject();
		if (!tryFitbit(guestId, timeInterval, caloriesBurned)) {
			caloriesBurned.accumulate("kcals", "?");
		}
		return caloriesBurned;
	}
	
	private boolean tryFitbit(long guestId, TimeInterval timeInterval,
			JSONObject caloriesBurned) {
		Connector fitbitConnector = Connector.getConnector("fitbit");
		if (!guestService.hasApiKey(guestId, fitbitConnector))
			return false;
		long then = System.currentTimeMillis();
		List<AbstractFacet> apiDataFacets = apiDataService.getApiDataFacets(
				guestId, fitbitConnector,
				ObjectType.getObjectType(fitbitConnector, "activity_summary"),
				timeInterval);
		long now = System.currentTimeMillis();
		System.out.println("time to do this query: " + (now-then));
		if (apiDataFacets.size() > 0) {
			FitbitActivityFacet fitbitActivitySummary = (FitbitActivityFacet) apiDataFacets
					.get(0);
			caloriesBurned.accumulate("kcals", fitbitActivitySummary.caloriesOut);
			caloriesBurned.accumulate("device", "fitbit");
			return true;
		}
		return false;
	}
}
