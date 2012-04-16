package com.fluxtream.dashboard.dataproviders.day;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONObject;

import org.springframework.stereotype.Component;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.fitbit.FitbitSleepFacet;
import com.fluxtream.connectors.zeo.ZeoSleepStatsFacet;
import com.fluxtream.dashboard.dataproviders.AbstractWidgetDataProvider;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.GuestSettings;

@Component("day/hoursSlept")
public class HoursSleptDataProvider extends AbstractWidgetDataProvider {

	@Override
	public JSONObject provideData(long guestId, GuestSettings settings,
			TimeInterval timeInterval) {
		JSONObject hoursSlept = new JSONObject();
		if (!(tryZeo(guestId, timeInterval, hoursSlept) ||
			tryFitbit(guestId, timeInterval, hoursSlept))){
			hoursSlept.accumulate("hours", "?");
			hoursSlept.accumulate("minutes", "?");
		}
		return hoursSlept;
	}
	
	private List<AbstractFacet> getFitbitSleepFacets(long guestId, TimeInterval timeInterval) {
		Connector fitbitConnector = Connector.getConnector("fitbit");
		if (!guestService.hasApiKey(guestId, fitbitConnector))
			return new ArrayList<AbstractFacet>();
		List<AbstractFacet> apiDataFacets = apiDataService.getApiDataFacets(
				guestId, fitbitConnector,
				ObjectType.getObjectType(fitbitConnector, "sleep"),
				timeInterval);
		return apiDataFacets;
	}
	
	private List<AbstractFacet> getZeoSleepFacets(long guestId, TimeInterval timeInterval) {
		Connector zeoConnector = Connector.getConnector("zeo");
		if (!guestService.hasApiKey(guestId, zeoConnector))
			return new ArrayList<AbstractFacet>();
		List<AbstractFacet> apiDataFacets = apiDataService.getApiDataFacets(
				guestId, zeoConnector, null, timeInterval);
		return apiDataFacets;
	}

	private boolean tryFitbit(long guestId, TimeInterval timeInterval,
			JSONObject hoursSlept) {
		List<AbstractFacet> apiDataFacets = getFitbitSleepFacets(guestId, timeInterval);
		if (apiDataFacets.size() > 0) {
			FitbitSleepFacet fitbitSleep = (FitbitSleepFacet) apiDataFacets
					.get(0);
			int minutes = fitbitSleep.minutesAsleep;
			int hours = minutes / 60;
			minutes = minutes % 60;
			hoursSlept.accumulate("hours", hours);
			hoursSlept.accumulate("minutes", minutes);
			hoursSlept.accumulate("device", "fitbit");
			return true;
		}
		return false;
	}

	private boolean tryZeo(long guestId, TimeInterval timeInterval,
			JSONObject hoursSlept) {
		List<AbstractFacet> apiDataFacets = getZeoSleepFacets(guestId, timeInterval);
		if (apiDataFacets.size() > 0) {
			ZeoSleepStatsFacet zeoSleep = (ZeoSleepStatsFacet) apiDataFacets
					.get(0);
			int minutes = zeoSleep.totalZ;
			int hours = minutes / 60;
			minutes = minutes % 60;
			hoursSlept.accumulate("hours", hours);
			hoursSlept.accumulate("minutes", minutes);
			hoursSlept.accumulate("device", "zeo");
			return true;
		}
		return false;
	}

}
