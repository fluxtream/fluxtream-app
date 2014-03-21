package org.fluxtream.connectors.up;

import org.fluxtream.OutsideTimeBoundariesException;
import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.vos.AbstractFacetVO;
import org.fluxtream.connectors.vos.TimeOfDayVO;
import org.fluxtream.domain.GuestSettings;
import org.codehaus.jackson.annotate.JsonRawValue;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

/**
 * User: candide
 * Date: 11/02/14
 * Time: 15:34
 */
public class JawboneUpWorkoutFacetVO extends AbstractFacetVO<JawboneUpWorkoutFacet> {

    String title;

    @JsonRawValue
    String details;

    public TimeOfDayVO startTime;
    public TimeOfDayVO endTime;

    public long start, end;
    public int startMinute, endMinute;

    @Override
    protected void fromFacet(final JawboneUpWorkoutFacet facet, final TimeInterval timeInterval, final GuestSettings settings) throws OutsideTimeBoundariesException {
        this.title = facet.title;
        this.date = facet.date;
        this.start = facet.start;
        this.end = facet.end;

        LocalDateTime localStartTime = new LocalDateTime(facet.start, DateTimeZone.forID(facet.tz));
        startMinute = localStartTime.getHourOfDay() * 60 + localStartTime.getMinuteOfHour();
        startTime = new TimeOfDayVO(startMinute, true);

        LocalDateTime localEndTime = new LocalDateTime(facet.end, DateTimeZone.forID(facet.tz));
        endMinute = localEndTime.getHourOfDay() * 60 + localEndTime.getMinuteOfHour();
        endTime = new TimeOfDayVO(endMinute, true);

        this.details = facet.workoutDetails;
    }

}
