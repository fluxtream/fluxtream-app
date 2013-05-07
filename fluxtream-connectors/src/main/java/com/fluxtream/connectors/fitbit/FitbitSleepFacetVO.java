package com.fluxtream.connectors.fitbit;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractLocalTimeTimedFacetVO;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.models.DurationModel;
import org.joda.time.LocalDateTime;

public class FitbitSleepFacetVO extends AbstractLocalTimeTimedFacetVO<FitbitSleepFacet> {

	public DurationModel minutesAsleep;
	public DurationModel minutesAwake;
	public DurationModel minutesToFallAsleep;

	@Override
	public void fromFacet(FitbitSleepFacet facet, TimeInterval timeInterval, GuestSettings settings) {
		final LocalDateTime riseTime = new LocalDateTime(facet.end);
        final LocalDateTime bedTime = new LocalDateTime(facet.start);
		startMinute = bedTime.getHourOfDay()*60+bedTime.getMinuteOfHour();
		endMinute = riseTime.getHourOfDay()*60+riseTime.getMinuteOfHour();
		minutesAsleep = new DurationModel(facet.minutesAsleep*60);
		minutesAwake = new DurationModel(facet.minutesAwake*60);
		minutesToFallAsleep = new DurationModel(facet.minutesToFallAsleep*60);
        date = facet.date;
	}

}
