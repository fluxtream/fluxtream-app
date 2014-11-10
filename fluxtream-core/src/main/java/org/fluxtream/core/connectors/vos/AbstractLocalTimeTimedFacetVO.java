package org.fluxtream.core.connectors.vos;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;
import org.fluxtream.core.domain.GuestSettings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public abstract class AbstractLocalTimeTimedFacetVO<T extends AbstractLocalTimeFacet> extends AbstractLocalTimeInstantFacetVO<T> {

    public String eventEnd;

    @Override
    public void extractValues(T facet, TimeInterval timeInterval, GuestSettings settings)
            throws OutsideTimeBoundariesException
    {
        super.extractValues(facet, timeInterval, settings);
        final DateTimeZone zone = DateTimeZone.forTimeZone(timeInterval.getTimeZone(facet.date));
        if (facet.startTimeStorage!=null&&facet.endTimeStorage!=null) {
            final DateTime startDateTime = ISODateTimeFormat.localDateOptionalTimeParser().parseLocalDateTime(facet.startTimeStorage).toDateTime(zone);
            this.eventStart = ISODateTimeFormat.dateTime().withZone(zone).print(startDateTime);
            final DateTime endDateTime = ISODateTimeFormat.localDateOptionalTimeParser().parseLocalDateTime(facet.endTimeStorage).toDateTime(zone);
            this.eventEnd = ISODateTimeFormat.dateTime().withZone(zone).print(endDateTime);
        } else {
            this.eventStart = ISODateTimeFormat.dateTime().withZone(zone).print(facet.start);
            this.eventEnd = ISODateTimeFormat.dateTime().withZone(zone).print(facet.end);
        }
    }
}
