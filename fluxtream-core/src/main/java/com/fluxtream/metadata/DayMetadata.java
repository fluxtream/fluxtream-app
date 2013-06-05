package com.fluxtream.metadata;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.domain.metadata.VisitedCity;
import org.joda.time.DateMidnight;
import org.joda.time.DateTimeConstants;

public class DayMetadata extends AbstractTimeUnitMetadata {

    public String date;

    public DayMetadata() {}

    public DayMetadata(String forDate) {
        long timeForDate = formatter.withZoneUTC().parseDateTime(forDate).getMillis();
        DateMidnight dateMidnight = new DateMidnight(timeForDate);
        start = dateMidnight.getMillis();
        end = start + DateTimeConstants.MILLIS_PER_DAY;
    }

    public DayMetadata(List<VisitedCity> cities, VisitedCity consensusVisitedCity,
                       VisitedCity previousInferredCity, VisitedCity nextInferredCity,
                       String date) {
        super(cities, consensusVisitedCity, previousInferredCity, nextInferredCity);
        long forDateTime = getTimeForDate(consensusVisitedCity, date);
        end = forDateTime + DateTimeConstants.MILLIS_PER_DAY;
    }

    public TimeInterval getTimeInterval() {
        // default to UTC
        TimeZone zone = TimeZone.getTimeZone("UTC");
        if (timeZone!=null)
            zone = TimeZone.getTimeZone(timeZone);
        return new TimeInterval(start, end, TimeUnit.DAY, zone);
	}
	
	public Calendar getStartCalendar() {
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone(this.timeZone));
		c.setTimeInMillis(start);
		return c;
	}

}
