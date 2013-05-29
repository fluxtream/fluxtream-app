package com.fluxtream.metadata;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.domain.metadata.VisitedCity;
import org.joda.time.DateMidnight;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DayMetadata {
	
	public float maxTempC = -10000,
			maxTempF = -10000,
			minTempC = 10000,
			minTempF = 10000;
	
	public String timeZone = "UTC";
    public String date;
    public long start, end;
    public int daysInferred = 0;

    public VisitedCity consensusVisitedCity;
    public List<VisitedCity> cities;


    private static final DateTimeFormatter formatter = DateTimeFormat
            .forPattern("yyyy-MM-dd");

	/**
	 * we consider only the first timezone and that which
	 * will be set last (/latest) which we will consider
	 * to be that of the evening
	 */
	public String otherTimeZone;

    @Deprecated
    public DayMetadata() {}

    public DayMetadata(String forDate) {
        long timeForDate = formatter.withZoneUTC().parseDateTime(forDate).getMillis();
        DateMidnight dateMidnight = new DateMidnight(timeForDate);
        start = dateMidnight.getMillis();
        end = start + DateTimeConstants.MILLIS_PER_DAY;
    }

    public DayMetadata(List<VisitedCity> cities, VisitedCity consensusVisitedCity, String forDate) {
        this.cities = cities;
        this.consensusVisitedCity = consensusVisitedCity;
        long cityTime = formatter.withZone(DateTimeZone.forID(consensusVisitedCity.city.geo_timezone)).parseDateTime(consensusVisitedCity.date).getMillis();
        long forDateTime = formatter.withZone(DateTimeZone.forID(consensusVisitedCity.city.geo_timezone)).parseDateTime(forDate).getMillis();
        daysInferred = Days.daysBetween(new DateMidnight(forDateTime), new DateMidnight(cityTime)).getDays();
        timeZone = consensusVisitedCity.city.geo_timezone;
        start = forDateTime;
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
