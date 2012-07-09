package com.fluxtream.connectors.fitbit;

import java.util.Date;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.models.DurationModel;

public class FitbitSleepFacetVO extends AbstractTimedFacetVO<FitbitSleepFacet>{

	public DurationModel minutesAsleep;
	public DurationModel minutesAwake;
	public DurationModel minutesToFallAsleep;
	public Date riseTime;
	public Date bedTime;
	
	@Override
	public void fromFacet(FitbitSleepFacet facet, TimeInterval timeInterval, GuestSettings settings) {
		riseTime = new Date(facet.end);
		bedTime = new Date(facet.start);
		startMinute = toMinuteOfDay(bedTime, timeInterval.timeZone);
		endMinute = toMinuteOfDay(riseTime, timeInterval.timeZone);
		minutesAsleep = new DurationModel(facet.minutesAsleep*60);
		minutesAwake = new DurationModel(facet.minutesAwake*60);
		minutesToFallAsleep = new DurationModel(facet.minutesToFallAsleep*60);
	}

}
