package org.fluxtream.core.connectors.vos;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.DurationModel;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

public abstract class AbstractTimedFacetVO<T extends AbstractFacet> extends AbstractInstantFacetVO<T> {

    public transient long end;
    public DurationModel duration;
    public String eventStart, eventEnd;

	@Override
	public void extractValues(T facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
		super.extractValues(facet, timeInterval, settings);
        this.eventStart = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forTimeZone(timeInterval.getTimeZone(facet.start))).print(facet.start);
        this.eventEnd = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forTimeZone(timeInterval.getTimeZone(facet.end))).print(facet.end);
	}

    public long end(){
        return ISODateTimeFormat.date().parseDateTime(this.eventEnd).getMillis();
    }

    public void setStart(final long millis) {
        this.eventStart = ISODateTimeFormat.dateTime().withZoneUTC().print(millis);
    }

    public void setEnd(final long millis) {
        this.eventEnd = ISODateTimeFormat.dateTime().withZoneUTC().print(millis);
    }
}
