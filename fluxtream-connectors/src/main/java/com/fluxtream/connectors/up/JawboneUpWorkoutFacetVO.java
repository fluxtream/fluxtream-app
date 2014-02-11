package com.fluxtream.connectors.up;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.connectors.vos.TimeOfDayVO;
import com.fluxtream.domain.GuestSettings;
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

    @Override
    protected void fromFacet(final JawboneUpWorkoutFacet facet, final TimeInterval timeInterval, final GuestSettings settings) throws OutsideTimeBoundariesException {
        this.title = facet.title;
        this.date = facet.date;

        LocalDateTime localStartTime = new LocalDateTime(facet.start, DateTimeZone.forID(facet.tz));
        startTime = new TimeOfDayVO(localStartTime.getHourOfDay()*60+localStartTime.getMinuteOfHour(), true);

        LocalDateTime localEndTime = new LocalDateTime(facet.end, DateTimeZone.forID(facet.tz));
        endTime = new TimeOfDayVO(localEndTime.getHourOfDay()*60+localEndTime.getMinuteOfHour(), true);

        this.details = facet.workoutDetails;
    }

}
