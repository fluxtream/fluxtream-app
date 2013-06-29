package com.fluxtream.metadata;

import java.util.List;
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
public class MonthMetadata extends AbstractTimespanMetadata {

    public MonthMetadata(final int year, final int month) {
        final LocalDate firstDayOfMonth = TimeUtils.getBeginningOfMonth(year, month);
        this.startDate = formatter.print(firstDayOfMonth);
        this.start = firstDayOfMonth.toDateTimeAtStartOfDay().getMillis();

        final LocalDate lastDayOfMonth = TimeUtils.getEndOfMonth(year, month);
        this.endDate = formatter.print(lastDayOfMonth);
        this.end = lastDayOfMonth.toDateTimeAtStartOfDay().getMillis() + DateTimeConstants.MILLIS_PER_DAY;
    }

    public MonthMetadata(final List<VisitedCity> cities, final VisitedCity consensusVisitedCity,
                         VisitedCity previousInferredCity, VisitedCity nextInferredCity,
                         final int year, final int month) {
        super(cities, consensusVisitedCity, previousInferredCity, nextInferredCity);

        // Calculate the calendar date at the beginning and end of this month.  Store as startDate and endDate.
        final LocalDate beginningOfMonth = TimeUtils.getBeginningOfMonth(year, month);
        final LocalDate endOfMonth = TimeUtils.getEndOfMonth(year, month);

        this.startDate = formatter.print(beginningOfMonth);
        this.endDate = formatter.print(endOfMonth);

        // Calculate the timezone to use for the start and end of the month.  If cities is not emtpy, use the
        // first city for start and the last city for end.  If cities is empty, use
        // previousInferredCity (if any) for start and nextInferredCity (if any).  If either of those is empty,
        // use consensusVisitedCity.  In case all those turn out to be null, start out with defaults in GMT.

        // Even this really isn't right, because the consensus timezone for the first and last dates may not
        // correspond to the contents of the start and end of the cities tables.  TODO: fix this
        DateTimeZone startTz = DateTimeZone.UTC;
        DateTimeZone endTz = DateTimeZone.UTC;

        VisitedCity startCity=null;
        VisitedCity endCity=null;

        // Compute startCity and endCity
        if(cities!=null && cities.size()>0) {
            // We have some cities, use them
            startCity = cities.get(0);
            endCity = cities.get(cities.size()-1);
        }
        else {
            if(previousInferredCity!=null) {
                startCity = previousInferredCity;
            }
            else {
                startCity = consensusVisitedCity;
            }

            if(nextInferredCity!=null) {
                endCity = nextInferredCity;
            }
            else {
                endCity = consensusVisitedCity;
            }
        }


        // Use startCity and endCity to compute
        if(startCity!=null) {
            try {
                startTz = DateTimeZone.forID(startCity.city.geo_timezone);
            }
            catch (Exception e) {
                System.out.println("Failed to parse timezone for " + this.startDate + ": " + startCity.city.geo_timezone);
            }
        }

        if(endCity!=null) {
            try {
                endTz = DateTimeZone.forID(endCity.city.geo_timezone);
            }
            catch (Exception e) {
                System.out.println("Failed to parse timezone for " + this.endDate + ": " + endCity.city.geo_timezone);
            }
        }

        // Finally, calculate start and and using the start and end timezones
        this.start = beginningOfMonth.toDateTimeAtStartOfDay(startTz).getMillis();
        this.end = endOfMonth.toDateTimeAtStartOfDay(endTz).getMillis() + DateTimeConstants.MILLIS_PER_DAY;
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
