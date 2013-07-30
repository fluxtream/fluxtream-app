package com.fluxtream.metadata;

import java.util.Collections;
import java.util.Comparator;
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
 * Time: 18:04
 */
public class WeekMetadata extends AbstractTimespanMetadata {

    public WeekMetadata(final int year, final int week) {
        final LocalDate beginningOfWeek = TimeUtils.getBeginningOfWeek(year, week);
        this.startDate = formatter.print(beginningOfWeek);
        this.start = beginningOfWeek.toDateTimeAtStartOfDay().getMillis();

        // endDate is the last day of the same week as startDate, so add 6 not 7
        final LocalDate endOfWeek = beginningOfWeek.plusDays(6);
        this.endDate = formatter.print(endOfWeek);
        this.end = endOfWeek.toDateTimeAtStartOfDay().getMillis() + DateTimeConstants.MILLIS_PER_DAY;
    }

    public WeekMetadata(final int year, final int week,
                        final List<VisitedCity> cities, final VisitedCity consensusVisitedCity,
                        VisitedCity previousInferredCity, VisitedCity nextInferredCity) {
        super(cities, consensusVisitedCity, previousInferredCity, nextInferredCity);
        Collections.sort(cities, new Comparator<VisitedCity>() {
            @Override
            public int compare(final VisitedCity o1, final VisitedCity o2) {
                return (int)(o1.start - o2.start);
            }
        });

        // Calculate the calendar date at the beginning and end of this week.  Store as startDate and endDate.
        final LocalDate beginningOfWeek = TimeUtils.getBeginningOfWeek(year, week);
        this.startDate = formatter.print(beginningOfWeek);

        // endDate is the last day of the same week as startDate, so add 6 not 7
        final LocalDate endOfWeek = beginningOfWeek.plusDays(6);
        this.endDate = formatter.print(endOfWeek);

        // Calculate the timezone to use for the start and end of the week.  If cities is not emtpy, use the
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
                System.out.println("Failed to parse timezone for " + beginningOfWeek + ": " + startCity.city.geo_timezone);
            }
        }

        if(endCity!=null) {
            try {
                endTz = DateTimeZone.forID(endCity.city.geo_timezone);
            }
            catch (Exception e) {
                System.out.println("Failed to parse timezone for " + endOfWeek + ": " + endCity.city.geo_timezone);
            }
        }

        // Finally, calculate start and and using the start and end timezones
        this.start = beginningOfWeek.toDateTimeAtStartOfDay(startTz).getMillis();
        this.end = endOfWeek.toDateTimeAtStartOfDay(endTz).getMillis() + DateTimeConstants.MILLIS_PER_DAY;

        // The above was added by Anne on 6/29/13.  The original version was below, but it assumed that
        // the cities array contains entries corresponding exactly to the first and last day of the week.
        // That assumption does not appear to be true.
        // this.start = cities.get(0).getDayStart();
        // this.end = cities.get(cities.size()-1).getDayEnd();

    }

    @Override
    protected TimeUnit getTimespanTimeUnit() {
        return TimeUnit.WEEK;
    }

}
