package com.fluxtream.connectors.google_calendar;

import java.util.Date;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.domain.GuestSettings;

public class GoogleCalendarEntryFacetVO extends AbstractTimedFacetVO<GoogleCalendarEntryFacet> {

	String title;
	
	@Override
	public void fromFacet(GoogleCalendarEntryFacet facet, TimeInterval timeInterval, GuestSettings settings) {
		this.description = facet.title;
		SWhen when = facet.times.get(0);
		startMinute = toMinuteOfDay(new Date(when.startTime), timeInterval.timeZone);
		endMinute = toMinuteOfDay(new Date(when.endTime), timeInterval.timeZone);
		this.title = facet.title;
	}

}
