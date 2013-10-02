package com.fluxtream.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.TimezoneAwareTimeInterval;
import com.fluxtream.TimezoneMap;
import com.fluxtream.domain.metadata.VisitedCity;
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

    private TimeInterval timeInterval;
    public long start, end;

    // startDate and endDate are in the date storage format: yyyy-MM-dd
    public String startDate, endDate;

    public VisitedCity consensusVisitedCity;
    public VisitedCity nextInferredCity;
    public VisitedCity previousInferredCity;

    private List<VisitedCity> cities;

    protected static final DateTimeFormatter formatter = DateTimeFormat
            .forPattern("yyyy-MM-dd");

    public AbstractTimespanMetadata() {}

    // This constructor does not set the time range (start, end, startDate, and endDate) and should only be called
    // by subclasses which are themselves going to set those parameters before return.
    protected AbstractTimespanMetadata(VisitedCity consensusVisitedCity, VisitedCity previousInferredCity, VisitedCity nextInferredCity,
                                       Map<String, TimeZone> consensusTimezones,
                                       TimezoneMap timezoneMap, List<VisitedCity> cities) {
        this.consensusVisitedCity = consensusVisitedCity;
        this.nextInferredCity = nextInferredCity;
        this.previousInferredCity = previousInferredCity;
        this.timeInterval = new TimezoneAwareTimeInterval(start, end, getTimespanTimeUnit(), consensusTimezones, timezoneMap);
        this.cities = cities;
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
        return timeInterval;
    }

    public List<VisitedCity> getCities() {
        return cities;
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
