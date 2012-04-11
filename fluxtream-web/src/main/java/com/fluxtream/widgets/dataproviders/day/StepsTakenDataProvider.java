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

@Component("day/stepsTaken")
public class StepsTakenDataProvider extends AbstractWidgetDataProvider {

	@Override
	public void provideData(long guestId, GuestSettings settings, TimeInterval timeInterval,
			JSONObject o) {
		JSONObject stepsTaken = new JSONObject();
		if (!tryFitbit(guestId, timeInterval, stepsTaken)) {
			stepsTaken.accumulate("steps", "?");
		}
		o.accumulate("stepsTaken", stepsTaken);
	}
	
	private boolean tryFitbit(long guestId, TimeInterval timeInterval,
			JSONObject stepsTaken) {
		Connector fitbitConnector = Connector.getConnector("fitbit");
		if (!guestService.hasApiKey(guestId, fitbitConnector))
			return false;
		List<AbstractFacet> apiDataFacets = apiDataService.getApiDataFacets(
				guestId, fitbitConnector,
				ObjectType.getObjectType(fitbitConnector, "activity_summary"),
				timeInterval);
		if (apiDataFacets.size() > 0) {
			FitbitActivityFacet fitbitActivitySummary = (FitbitActivityFacet) apiDataFacets
					.get(0);
			stepsTaken.accumulate("steps", fitbitActivitySummary.steps);
			stepsTaken.accumulate("device", "fitbit");
			return true;
		}
		return false;
	}
	
}
