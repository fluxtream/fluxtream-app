package com.fluxtream.connectors.fitbit;

import java.util.Date;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.domain.GuestSettings;

public class FitbitLoggedActivityFacetVO extends AbstractTimedFacetVO<FitbitLoggedActivityFacet> {

	int steps;
	int caloriesOut;
	
	@Override
	public void fromFacet(FitbitLoggedActivityFacet facet, TimeInterval timeInterval,
			GuestSettings settings) {
		this.steps = facet.steps;
		caloriesOut = facet.calories;
		description = facet.fullTextDescription;
		startMinute = toMinuteOfDay(new Date(facet.start), timeInterval.timeZone);
		endMinute = toMinuteOfDay(new Date(facet.end), timeInterval.timeZone);
	}

}
