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
 * Time: 18:04
 */
public class WeekMetadata extends AbstractTimespanMetadata {

    public WeekMetadata(final int year, final int week) {
        final LocalDate beginningOfWeek = TimeUtils.getBeginningOfWeek(year, week);
        this.startDate = TimeUtils.dateFormatter.print(beginningOfWeek);
        this.start = beginningOfWeek.toDateTimeAtStartOfDay().getMillis();

        // endDate is the last day of the same week as startDate, so add 6 not 7
        final LocalDate endOfWeek = beginningOfWeek.plusDays(6);
        this.endDate = TimeUtils.dateFormatter.print(endOfWeek);
        this.end = endOfWeek.toDateTimeAtStartOfDay().getMillis() + DateTimeConstants.MILLIS_PER_DAY;
    }

    public WeekMetadata(final VisitedCity consensusVisitedCity,
                        VisitedCity previousInferredCity, VisitedCity nextInferredCity,
                        Map<String, TimeZone> consensusTimezones, TimezoneMap timezoneMap,
                        List<VisitedCity> cities,List<VisitedCity> consensusCities,
                        final int year, final int week) {
        super(consensusVisitedCity, previousInferredCity, nextInferredCity, consensusTimezones, timezoneMap, cities, consensusCities);

        // Calculate the calendar date at the beginning and end of this week.  Store as startDate and endDate.
        final LocalDate beginningOfWeek = TimeUtils.getBeginningOfWeek(year, week);
        this.startDate = TimeUtils.dateFormatter.print(beginningOfWeek);

        // endDate is the last day of the same week as startDate, so add 6 not 7
        final LocalDate endOfWeek = beginningOfWeek.plusDays(6);
        this.endDate = TimeUtils.dateFormatter.print(endOfWeek);

        DateTimeZone startTz = DateTimeZone.forTimeZone(consensusTimezones.get(this.startDate));
        DateTimeZone endTz = DateTimeZone.forTimeZone(consensusTimezones.get(this.endDate));

        this.start = beginningOfWeek.toDateTimeAtStartOfDay(startTz).getMillis();
        this.end = endOfWeek.toDateTimeAtStartOfDay(endTz).getMillis() + DateTimeConstants.MILLIS_PER_DAY;
    }

    @Override
    protected TimeUnit getTimespanTimeUnit() {
        return TimeUnit.WEEK;
    }

}
