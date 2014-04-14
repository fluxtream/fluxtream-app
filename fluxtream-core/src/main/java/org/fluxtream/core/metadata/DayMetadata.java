package org.fluxtream.core.metadata;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeMap;
import org.fluxtream.core.TimeUnit;
import org.fluxtream.core.TimezoneMap;
import org.fluxtream.core.domain.metadata.VisitedCity;
import org.fluxtream.core.utils.TimeUtils;
import org.joda.time.DateMidnight;
import org.joda.time.DateTimeConstants;

public class DayMetadata extends AbstractTimespanMetadata {

    public String date;

    public DayMetadata() {}

    @Override
    protected TimeUnit getTimespanTimeUnit() {
        return TimeUnit.DAY;
    }

    public DayMetadata(String forDate) {
        long timeForDate = TimeUtils.dateFormatterUTC.parseDateTime(forDate).getMillis();
        DateMidnight dateMidnight = new DateMidnight(timeForDate);
        start = dateMidnight.getMillis();
        end = start + DateTimeConstants.MILLIS_PER_DAY;
        this.startDate = this.endDate = this.date = forDate;
    }

    public DayMetadata(VisitedCity consensusVisitedCity,
                       VisitedCity previousInferredCity, VisitedCity nextInferredCity,
                       TreeMap<String, TimeZone> consensusTimezones, TimezoneMap timezoneMap,
                       List<VisitedCity> cities, List<VisitedCity> consensusCities,
                       String date) {
        super(consensusVisitedCity, previousInferredCity, nextInferredCity, consensusTimezones, timezoneMap, cities, consensusCities);
        this.start = getStartTimeForDate(consensusVisitedCity, date);
        this.end = start + DateTimeConstants.MILLIS_PER_DAY;
        this.startDate = this.endDate = this.date = date;
    }

	public Calendar getStartCalendar() {
        TimeZone tz = null;

        if(this.consensusVisitedCity!=null && this.consensusVisitedCity.city!=null) {
            // Note that there are strings in the geo_timezone column
            // of the cities table which cause getTimeZone to throw an exception
            try {
                tz=TimeZone.getTimeZone(this.consensusVisitedCity.city.geo_timezone);
            }
            catch (Exception e) {
                System.out.println("Failed to parse timezone for " + consensusVisitedCity.city.geo_timezone + ", using UTC");
            }
        }
        else {
            System.out.println("Invalid consensusVisitedCity, using UTC");
        }

        if(tz==null) {
            tz = TimeZone.getTimeZone("GMT");
        }

		Calendar c = Calendar.getInstance(tz);
		c.setTimeInMillis(start);
		return c;
	}

}
