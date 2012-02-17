package com.fluxtream.connectors.fitbit;

import java.util.Date;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.domain.GuestSettings;

public class FitbitSleepFacetVO extends AbstractTimedFacetVO<FitbitSleepFacet>{

	public int minutesAsleep;
	public int minutesAwake;
	public int minutesToFallAsleep;
	public Date riseTime;
	public Date bedTime;
	
	@Override
	public void fromFacet(FitbitSleepFacet facet, TimeInterval timeInterval, GuestSettings settings) {
		riseTime = new Date(facet.end);
		bedTime = new Date(facet.start);
		startMinute = toMinuteOfDay(bedTime, timeInterval.timeZone);
		endMinute = toMinuteOfDay(riseTime, timeInterval.timeZone);
		minutesAsleep = facet.minutesAsleep;
		minutesAwake = facet.minutesAwake;
		minutesToFallAsleep = facet.minutesToFallAsleep;
	}

}
