package com.fluxtream.connectors.fitbit;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractLocalTimeTimedFacetVO;
import com.fluxtream.domain.GuestSettings;
import org.joda.time.LocalDateTime;

public class FitbitLoggedActivityFacetVO extends AbstractLocalTimeTimedFacetVO<FitbitLoggedActivityFacet> {

	int steps;
	int caloriesOut;
	
	@Override
	public void fromFacet(FitbitLoggedActivityFacet facet, TimeInterval timeInterval,
			GuestSettings settings) {
		this.steps = facet.steps;
		caloriesOut = facet.calories;
		description = facet.fullTextDescription;
        LocalDateTime startTime = timeStorageFormat.parseLocalDateTime(facet.startTimeStorage);
        LocalDateTime endTime = timeStorageFormat.parseLocalDateTime(facet.endTimeStorage);
		startMinute = startTime.getHourOfDay()*60+startTime.getMinuteOfHour();
		endMinute = endTime.getHourOfDay()*60+endTime.getMinuteOfHour();
	}

}
