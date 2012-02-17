package com.fluxtream.connectors.fitbit;

import java.util.ArrayList;
import java.util.List;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractFacetVOCollection;
import com.fluxtream.domain.GuestSettings;

@SuppressWarnings("rawtypes")
public class FitbitFacetVOCollection extends AbstractFacetVOCollection {

	FitbitActivityFacetVO activity_summary;
	List<FitbitSleepFacetVO> sleep;
	List<FitbitLoggedActivityFacetVO> logged_activity;
	
	@Override
	public void extractFacets(List facets, TimeInterval timeInterval, GuestSettings settings) {
		for (Object facet : facets) {
			if (facet instanceof FitbitLoggedActivityFacet)
				addLoggedActivity((FitbitLoggedActivityFacet)facet, timeInterval, settings);
			else if (facet instanceof FitbitSleepFacet)
				addSleepMeasure((FitbitSleepFacet)facet, timeInterval, settings);
			else if (facet instanceof FitbitActivityFacet)
				addActivityData((FitbitActivityFacet)facet, timeInterval, settings);
		}
	}

	private void addActivityData(FitbitActivityFacet facet, TimeInterval timeInterval, GuestSettings settings) {
		this.activity_summary = new FitbitActivityFacetVO();
		this.activity_summary.fromFacet(facet, timeInterval, settings);
	}

	private void addSleepMeasure(FitbitSleepFacet facet, TimeInterval timeInterval, GuestSettings settings) {
		if (sleep==null) sleep = new ArrayList<FitbitSleepFacetVO>();
		FitbitSleepFacetVO jsonFacet = new FitbitSleepFacetVO();
		jsonFacet.extractValues(facet, timeInterval, settings);
		sleep.add(jsonFacet);
	}

	private void addLoggedActivity(FitbitLoggedActivityFacet facet, TimeInterval timeInterval, GuestSettings settings) {
		if (logged_activity==null) logged_activity = new ArrayList<FitbitLoggedActivityFacetVO>();
		FitbitLoggedActivityFacetVO jsonFacet = new FitbitLoggedActivityFacetVO();
		jsonFacet.extractValues(facet, timeInterval, settings);
		logged_activity.add(jsonFacet);
	}

}
