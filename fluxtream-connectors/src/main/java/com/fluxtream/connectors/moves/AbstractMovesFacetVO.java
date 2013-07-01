package com.fluxtream.connectors.moves;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.models.DurationModel;

/**
 * User: candide
 * Date: 20/06/13
 * Time: 17:37
 */
public abstract class AbstractMovesFacetVO<T extends MovesFacet> extends AbstractTimedFacetVO<T> {

    List<MovesActivityVO> activities = new ArrayList<MovesActivityVO>();

    protected void fromFacetBase(final MovesFacet facet, final TimeInterval timeInterval, final GuestSettings settings) throws OutsideTimeBoundariesException {
        // In case this is an endcap facet, meaning it either crosses the leading or trailing midnight of a day,
        // trim the part crossing beyond the leading/trailing midnight so it fits inside the date.

        // Start by computing the timeZone currently in use for the date stored in the facet.
        //
        // It's unfortunate that we have two different time zone systems going on
        // and I don't offhand know how to do what I want only using one or the other,
        // but oh well...
        TimeZone timeZone = timeInterval.getTimeZone(facet.date);
        DateTimeZone dateTimeZone = DateTimeZone.forTimeZone(timeZone);

        // Compute the start and end of that date in milliseconds for comparing
        // and truncating start/end
        LocalDate facetLocalDate = LocalDate.parse(facet.date);
        long dateStart = facetLocalDate.toDateTimeAtStartOfDay(dateTimeZone).getMillis();
        long dateEnd = dateStart + DateTimeConstants.MILLIS_PER_DAY;

        // Check if crosses leading midnight
        if(facet.start<dateStart) {
            this.start = dateStart;
            this.startMinute = 0;
        }
        else {
            this.start = facet.start;
            this.startMinute = toMinuteOfDay(new Date(facet.start), timeZone);
        }

        // Check if crosses trailing midnight
        if(facet.end>=dateEnd) {
            this.end = dateEnd-1;
            this.endMinute = DateTimeConstants.MINUTES_PER_HOUR*DateTimeConstants.HOURS_PER_DAY - 1;
        }
        else {
            this.end = facet.end;
            this.endMinute = toMinuteOfDay(new Date(facet.end), timeZone);
        }

        // Calculate duration from the truncated times
        this.duration = new DurationModel((int)(this.end-this.start)/1000);

        // Also prune the activities
        for (MovesActivity activity : facet.getActivities()) {
            // Only include activities which overlap at least part of this date
            // This comparison looks funny, but it is true if there is any overlap
            // between the time range of the activity and this date
            if(activity.start<dateEnd || activity.end>dateStart) {
                activities.add(new MovesActivityVO(activity, timeZone,
                                                   dateStart, dateEnd, settings));
            }
        }
    }

}
