package com.fluxtream.metadata;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import com.fluxtream.TimeUnit;
import com.fluxtream.domain.metadata.VisitedCity;
import org.joda.time.DateMidnight;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

public class DayMetadata extends AbstractTimespanMetadata {

    public String date;

    public DayMetadata() {}

    @Override
    protected TimeUnit getTimespanTimeUnit() {
        return TimeUnit.DAY;
    }

    public DayMetadata(String forDate) {
        long timeForDate = formatter.withZoneUTC().parseDateTime(forDate).getMillis();
        DateMidnight dateMidnight = new DateMidnight(timeForDate);
        start = dateMidnight.getMillis();
        end = start + DateTimeConstants.MILLIS_PER_DAY;
        this.startDate = this.endDate = this.date = forDate;
    }

    public DayMetadata(List<VisitedCity> cities, VisitedCity consensusVisitedCity,
                       VisitedCity previousInferredCity, VisitedCity nextInferredCity,
                       String date) {
        super(cities, consensusVisitedCity, previousInferredCity, nextInferredCity);
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
