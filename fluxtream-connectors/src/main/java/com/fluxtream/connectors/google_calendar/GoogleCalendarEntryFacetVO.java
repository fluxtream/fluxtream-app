package com.fluxtream.connectors.google_calendar;

import java.util.Date;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.domain.GuestSettings;

public class GoogleCalendarEntryFacetVO extends AbstractTimedFacetVO<GoogleCalendarEntryFacet> {

	String title;
	
	@Override
	public void fromFacet(GoogleCalendarEntryFacet facet, TimeInterval timeInterval, GuestSettings settings)
            throws OutsideTimeBoundariesException {
        //TODO: hack!
        this.type = "google_calendar-entry";
		this.description = facet.title;
		SWhen when = facet.times.get(0);
		startMinute = toMinuteOfDay(new Date(when.startTime), timeInterval.getTimeZone(facet.start));
		endMinute = toMinuteOfDay(new Date(when.endTime), timeInterval.getTimeZone(facet.start));
		this.title = facet.title;
	}

}
