package com.fluxtream.metadata;

import java.util.List;
import java.util.TimeZone;
import com.fluxtream.SimpleTimeInterval;
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
public class WeekMetadata extends AbstractTimespanMetadata {

    public WeekMetadata(final int year, final int week) {
        final LocalDate beginningOfWeek = TimeUtils.getBeginningOfWeek(year, week);
        this.start = beginningOfWeek.toDateTimeAtStartOfDay().getMillis();
        final LocalDate endOfWeek = beginningOfWeek.plusDays(7);
        this.end = endOfWeek.toDateTimeAtStartOfDay().getMillis() + DateTimeConstants.MILLIS_PER_DAY;
    }

    public WeekMetadata(final List<VisitedCity> cities, final VisitedCity consensusVisitedCity,
                        VisitedCity previousInferredCity, VisitedCity nextInferredCity,
                        final int year, final int week) {
        super(cities, consensusVisitedCity, previousInferredCity, nextInferredCity);
        this.start = getStartOfWeek(consensusVisitedCity.city.geo_timezone, year, week);
        this.end = getEndOfWeek(consensusVisitedCity.city.geo_timezone, year, week);
    }

    public static long getEndOfWeek(final String timeZone, final int year, final int week) {
        final LocalDate beginningOfWeek = TimeUtils.getBeginningOfWeek(year, week);
        final LocalDate endOfWeek = beginningOfWeek.plusDays(7);
        final DateTimeZone consensusTimezone = DateTimeZone.forID(timeZone);
        long t = endOfWeek.toDateTimeAtStartOfDay(consensusTimezone).getMillis() + DateTimeConstants.MILLIS_PER_DAY;
        return t;
    }

    public static long getStartOfWeek(final String timeZone, final int year, final int week) {
        final LocalDate beginningOfWeek = TimeUtils.getBeginningOfWeek(year, week);
        final DateTimeZone consensusTimezone = DateTimeZone.forID(timeZone);
        long t = beginningOfWeek.toDateTimeAtStartOfDay(consensusTimezone).getMillis();
        return t;
    }

    public TimeInterval getTimeInterval() {
        // default to UTC
        TimeZone zone = TimeZone.getTimeZone("UTC");
        if (timeZone!=null)
            zone = TimeZone.getTimeZone(timeZone);
        return new SimpleTimeInterval(start, end, TimeUnit.WEEK, zone);
    }

}
