package org.fluxtream.core.connectors.vos;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;
import org.fluxtream.core.domain.GuestSettings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

public abstract class AbstractInstantFacetVO<T extends AbstractFacet> extends
		AbstractFacetVO<T> implements Comparable<AbstractInstantFacetVO<T>> {

    public transient long start;
    public String eventStart;

	@Override
	public void extractValues(T facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
        this.start = facet.start;
        if (facet instanceof AbstractLocalTimeFacet) {
            AbstractLocalTimeFacet ltf = (AbstractLocalTimeFacet) facet;
            this.eventStart = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forTimeZone(timeInterval.getTimeZone(ltf.date))).print(facet.start);
        } else {
            this.eventStart = ISODateTimeFormat.dateTime().withZoneUTC().print(facet.start);
        }
		super.extractValues(facet, timeInterval, settings);
    }
	
	@Override
	public int compareTo(AbstractInstantFacetVO<T> other) {
        DateTime thisStart = ISODateTimeFormat.date().parseDateTime(this.eventStart);
        DateTime otherStart = ISODateTimeFormat.date().parseDateTime(other.eventStart);
		return thisStart.isAfter(otherStart) ? 1 : -1;
	}

}
