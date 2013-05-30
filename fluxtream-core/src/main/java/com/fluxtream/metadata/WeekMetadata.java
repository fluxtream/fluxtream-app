package com.fluxtream.metadata;

import java.util.List;
import java.util.TimeZone;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.domain.metadata.VisitedCity;
import com.fluxtream.utils.TimeUtils;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

/**
 * User: candide
 * Date: 29/05/13
 * Time: 18:04
 */
public class WeekMetadata extends AbstractTimeUnitMetadata {

    public WeekMetadata(final int year, final int week) {
        final LocalDate beginningOfWeek = TimeUtils.getBeginningOfWeek(year, week);
        this.start = beginningOfWeek.toDateTimeAtStartOfDay().getMillis();
        final LocalDate endOfWeek = beginningOfWeek.plusDays(7);
        this.end = endOfWeek.toDateTimeAtStartOfDay().getMillis() + DateTimeConstants.MILLIS_PER_DAY;
    }

    public WeekMetadata(final List<VisitedCity> cities, final VisitedCity consensusVisitedCity, final int year, final int week) {
        super(cities, consensusVisitedCity);
        final LocalDate beginningOfWeek = TimeUtils.getBeginningOfWeek(year, week);
        final DateTimeZone consensusTimezone = DateTimeZone.forID(consensusVisitedCity.city.geo_timezone);
        this.start = beginningOfWeek.toDateTimeAtStartOfDay(consensusTimezone).getMillis();
        final LocalDate endOfWeek = beginningOfWeek.plusDays(7);
        this.end = endOfWeek.toDateTimeAtStartOfDay(consensusTimezone).getMillis() + DateTimeConstants.MILLIS_PER_DAY;
    }

    public TimeInterval getTimeInterval() {
        // default to UTC
        TimeZone zone = TimeZone.getTimeZone("UTC");
        if (timeZone!=null)
            zone = TimeZone.getTimeZone(timeZone);
        return new TimeInterval(start, end, TimeUnit.WEEK, zone);
    }

}
