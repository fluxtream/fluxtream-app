package org.fluxtream.metadata;

import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.fluxtream.TimeUnit;
import org.fluxtream.TimezoneMap;
import org.fluxtream.domain.metadata.VisitedCity;
import org.fluxtream.utils.TimeUtils;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

/**
 * User: candide
 * Date: 29/05/13
 * Time: 18:19
 */
public class MonthMetadata extends AbstractTimespanMetadata {

    public MonthMetadata(final int year, final int month) {
        final LocalDate firstDayOfMonth = TimeUtils.getBeginningOfMonth(year, month);
        this.startDate = TimeUtils.dateFormatter.print(firstDayOfMonth);
        this.start = firstDayOfMonth.toDateTimeAtStartOfDay().getMillis();

        final LocalDate lastDayOfMonth = TimeUtils.getEndOfMonth(year, month);
        this.endDate = TimeUtils.dateFormatter.print(lastDayOfMonth);
        this.end = lastDayOfMonth.toDateTimeAtStartOfDay().getMillis() + DateTimeConstants.MILLIS_PER_DAY;
    }

    public MonthMetadata(final VisitedCity consensusVisitedCity,
                         VisitedCity previousInferredCity, VisitedCity nextInferredCity,
                         Map<String, TimeZone> consensusTimezones, TimezoneMap timezoneMap,
                         List<VisitedCity> cities, List<VisitedCity> consensusCities,
                         final int year, final int month) {
        super(consensusVisitedCity, previousInferredCity, nextInferredCity, consensusTimezones, timezoneMap, cities, consensusCities);

        // Calculate the calendar date at the beginning and end of this month.  Store as startDate and endDate.
        final LocalDate beginningOfMonth = TimeUtils.getBeginningOfMonth(year, month);
        final LocalDate endOfMonth = TimeUtils.getEndOfMonth(year, month);

        this.startDate = TimeUtils.dateFormatter.print(beginningOfMonth);
        this.endDate = TimeUtils.dateFormatter.print(endOfMonth);

        final TimeZone startTz = consensusTimezones.get(this.startDate);
        final TimeZone endTz = consensusTimezones.get(this.endDate);

        // Finally, calculate start and and using the start and end timezones
        this.start = beginningOfMonth.toDateTimeAtStartOfDay(DateTimeZone.forTimeZone(startTz)).getMillis();
        this.end = endOfMonth.toDateTimeAtStartOfDay(DateTimeZone.forTimeZone(endTz)).getMillis() + DateTimeConstants.MILLIS_PER_DAY;
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

    @Override
    protected TimeUnit getTimespanTimeUnit() {
        return TimeUnit.MONTH;
    }

}
