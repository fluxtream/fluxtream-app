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
 * Time: 18:19
 */
public class MonthMetadata extends AbstractTimeUnitMetadata {

    public MonthMetadata(final int year, final int month) {
        final LocalDate firstDayOfMonth = TimeUtils.getBeginningOfMonth(year, month);
        this.start = firstDayOfMonth.toDateTimeAtStartOfDay().getMillis();
        final LocalDate lastDayOfMonth = TimeUtils.getEndOfMonth(year, month);
        this.end = lastDayOfMonth.toDateTimeAtStartOfDay().getMillis() + DateTimeConstants.MILLIS_PER_DAY;
    }

    public MonthMetadata(final List<VisitedCity> cities, final VisitedCity consensusVisitedCity, final int year, final int month) {
        super(cities, consensusVisitedCity);
        this.start = getStartOfMonth(consensusVisitedCity.city.geo_timezone, year, month);
        this.end = getEndOfMonth(consensusVisitedCity.city.geo_timezone, year, month);
    }

    public static long getEndOfMonth(String timeZone, final int year, final int month) {
        final DateTimeZone consensusTimezone = DateTimeZone.forID(timeZone);
        final LocalDate lastDayOfMonth = TimeUtils.getEndOfMonth(year, month);
        long t = lastDayOfMonth.toDateTimeAtStartOfDay(consensusTimezone).getMillis() + DateTimeConstants.MILLIS_PER_DAY;
        return t;
    }

    public static long getStartOfMonth(String timeZone, final int year, final int month) {
        final LocalDate firstDayOfMonth = TimeUtils.getBeginningOfMonth(year, month);
        final DateTimeZone consensusTimezone = DateTimeZone.forID(timeZone);
        final long t = firstDayOfMonth.toDateTimeAtStartOfDay(consensusTimezone).getMillis();
        return t;
    }

    public TimeInterval getTimeInterval() {
        // default to UTC
        TimeZone zone = TimeZone.getTimeZone("UTC");
        if (timeZone!=null)
            zone = TimeZone.getTimeZone(timeZone);
        return new TimeInterval(start, end, TimeUnit.MONTH, zone);
    }

}
