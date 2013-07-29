package com.fluxtream.connectors.google_calendar;

import java.util.Date;
import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.domain.GuestSettings;

/**
 * User: candide
 * Date: 28/07/13
 * Time: 21:39
 */
public class GoogleCalendarEventFacetVO extends AbstractTimedFacetVO<GoogleCalendarEventFacet> {

    public String summary;
    public String description;
    public String attendees;

    @Override
    protected void fromFacet(final GoogleCalendarEventFacet facet, final TimeInterval timeInterval, final GuestSettings settings) throws OutsideTimeBoundariesException {
        startMinute = toMinuteOfDay(new Date(facet.start), timeInterval.getTimeZone(facet.start));
        endMinute = toMinuteOfDay(new Date(facet.end), timeInterval.getTimeZone(facet.end));
        this.description = facet.description;
        this.summary = facet.summary;
        this.attendees = facet.attendees;
    }

}
