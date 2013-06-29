package com.fluxtream.metadata;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.TreeSet;
import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.domain.metadata.VisitedCity;
import com.fluxtream.utils.TimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * User: candide
 * Date: 30/05/13
 * Time: 10:39
 */
public abstract class AbstractTimespanMetadata {

    public float maxTempC = -10000,
            maxTempF = -10000,
            minTempC = 10000,
            minTempF = 10000;

    public long start, end;

    // startDate and endDate are in the date storage format: yyyy-MM-dd
    public String startDate, endDate;

    public VisitedCity consensusVisitedCity;
    public VisitedCity nextInferredCity;
    public VisitedCity previousInferredCity;

    private List<VisitedCity> cities;

    protected static final DateTimeFormatter formatter = DateTimeFormat
            .forPattern("yyyy-MM-dd");

    public List<VisitedCity> getCities() {
        Collections.sort(cities, new Comparator<VisitedCity>() {
            @Override
            public int compare(final VisitedCity o1, final VisitedCity o2) {
                return (int)(o1.start - o2.start);
            }
        });
        return cities;
    }

    public void setCities(final List<VisitedCity> cities) {
        this.cities = cities;
    }

    protected class TimespanTimeInterval implements TimeInterval {

        @Override
        public TimeZone getMainTimeZone() {
            // Return the timezone for the consensus visited city.
            // Note that there are strings in the geo_timezone column
            // of the cities table which cause getTimeZone to throw an exception
            if(consensusVisitedCity!=null && consensusVisitedCity.city!=null) {
                try {
                    return TimeZone.getTimeZone(consensusVisitedCity.city.geo_timezone);
                }
                catch (Exception e) {
                    System.out.println("Failed to parse timezone for " + consensusVisitedCity.city.geo_timezone + ", returning UTC");
                }
            }
            else {
                System.out.println("Invalid consensusVisitedCity, returning UTC");
            }
            return TimeZone.getTimeZone("GMT");
        }

        @Override
        public long getStart() { return start; };

        @Override
        public long getEnd() { return end; }

        @Override
        public TimeUnit getTimeUnit() { return getTimespanTimeUnit(); }

        @Override
        public TimeZone getTimeZone(final long time) throws OutsideTimeBoundariesException {
            if (getTimeUnit()==TimeUnit.DAY)
                return getMainTimeZone();

            // This assumes that the cities array contains an entry for each date in the range, which
            // is a false assumption.  TODO: fix this
            for (VisitedCity city : getCities()) {
                long dayStart = city.getDayStart();
                long dayEnd = city.getDayEnd();
                if (dayStart<=time&&dayEnd>time) {
                    // Note that there are strings in the geo_timezone column
                    // of the cities table which cause getTimeZone to throw an exception
                    try {
                        return TimeZone.getTimeZone(city.city.geo_timezone);
                    }
                    catch (Exception e) {
                        System.out.println("Failed to parse timezone for " + city.city.geo_timezone + ", returning UTC");
                        return TimeZone.getTimeZone("GMT");
                    }
                }
            }
            throw new OutsideTimeBoundariesException();
        }

        @Override
        public TimeZone getTimeZone(final String date) throws OutsideTimeBoundariesException {
            if (getTimeUnit()==TimeUnit.DAY)
                return getMainTimeZone();
            // This assumes that the cities array contains an entry for each date in the range, which
            // is a false assumption.  TODO: fix this
            for (VisitedCity city : getCities()) {
                if (city.date.equals(date)) {
                    // Note that there are strings in the geo_timezone column
                    // of the cities table which cause getTimeZone to throw an exception
                    try {
                        return TimeZone.getTimeZone(city.city.geo_timezone);
                    }
                    catch (Exception e) {
                        System.out.println("Failed to parse timezone for " + city.city.geo_timezone + ", returning UTC");
                        return TimeZone.getTimeZone("GMT");
                    }
                }
            }
            throw new OutsideTimeBoundariesException();
        }
    }

    public AbstractTimespanMetadata() {}

    // This constructor does not set the time range (start, end, startDate, and endDate) and should only be called
    // by subclasses which are themselves going to set those parameters before return.
    protected AbstractTimespanMetadata(List<VisitedCity> cities, VisitedCity consensusVisitedCity, VisitedCity previousInferredCity, VisitedCity nextInferredCity) {
        this.setCities(cities);
        this.consensusVisitedCity = consensusVisitedCity;
        this.nextInferredCity = nextInferredCity;
        this.previousInferredCity = previousInferredCity;
    }

    // Returns the millisecond time of the start of the given date in the given city.
    // This safely dereferences the geo_timezone field, and returns the answer in UTC if
    // the city or the geo_timezone are not able to be successfully parsed.
    public static long getStartTimeForDate(final VisitedCity city, final String forDate) {
        DateTimeZone dateTimeZone = null;

        if(city!=null && city.city!=null) {
            // Note that there are strings in the geo_timezone column
            // of the cities table which cause getTimeZone to throw an exception
            try {
                dateTimeZone = DateTimeZone.forID(city.city.geo_timezone);
            }
            catch (Exception e) {
                System.out.println("Failed to parse timezone for " + city.city.geo_timezone + ", using UTC");
            }
        }
        else {
            System.out.println("Invalid city, using UTC");
        }

       if(dateTimeZone==null) {
            dateTimeZone = DateTimeZone.UTC;
         }

        long forDateTime = formatter.withZone(dateTimeZone).parseDateTime(forDate).getMillis();
        return forDateTime;
    }

    protected abstract TimeUnit getTimespanTimeUnit();

    public TimeInterval getTimeInterval() {
        return new TimespanTimeInterval();
    }


    // Return a list of dates starting with startDate and ending with endDate
    public List<String> getDateList() {
        LocalDate currLocalDate = LocalDate.parse(startDate);
        final LocalDate endLocalDate = LocalDate.parse(endDate);
        List<String> dates = new ArrayList<String>();
        while(!currLocalDate.isAfter(endLocalDate)) {
            final String date = formatter.withZoneUTC().print(currLocalDate);
            dates.add(date);
            currLocalDate = currLocalDate.plusDays(1);
        }
        return dates;
    }

}
