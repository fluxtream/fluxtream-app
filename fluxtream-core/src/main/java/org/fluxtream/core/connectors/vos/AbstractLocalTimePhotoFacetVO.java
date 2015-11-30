package org.fluxtream.core.connectors.vos;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;
import org.fluxtream.core.domain.GuestSettings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Created by candide on 23/07/15.
 */
public abstract class AbstractLocalTimePhotoFacetVO<T extends AbstractLocalTimeFacet> extends AbstractPhotoFacetVO<T> {

    public String eventStart;
    public boolean localTime;

    @Override
    public void extractValues(T facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
        super.extractValues(facet, timeInterval, settings);
        this.date = facet.date;
        final DateTimeZone zone = DateTimeZone.forTimeZone(timeInterval.getTimeZone(facet.date));
        if (facet.startTimeStorage!=null) {
            final DateTime startDateTime = ISODateTimeFormat.localDateOptionalTimeParser().parseLocalDateTime(facet.startTimeStorage).toDateTime(zone);
            this.eventStart = ISODateTimeFormat.dateTime().withZone(zone).print(startDateTime);
        } else {
            this.eventStart = ISODateTimeFormat.dateTime().withZone(zone).print(facet.start);
        }
        localTime = true;
    }

    @Override
    public int compareTo(final AbstractInstantFacetVO<T> other) {
        final DateTime thisStart = ISODateTimeFormat.date().parseDateTime(this.eventStart);
        DateTime otherStart = ISODateTimeFormat.date().parseDateTime(other.eventStart);
        return thisStart.isAfter(otherStart) ? 1 : -1;
    }

}
