package com.fluxtream.domain.metadata;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.domain.AbstractLocalTimeFacet;
import org.joda.time.DateMidnight;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DayMetadataFacet extends AbstractLocalTimeFacet {
	
	public float maxTempC = -10000,
			maxTempF = -10000,
			minTempC = 10000,
			minTempF = 10000;
	
	public String timeZone = "UTC";

    public List<VisitedCity> cities;
    public int daysInferred = 0;

    private static final DateTimeFormatter formatter = DateTimeFormat
            .forPattern("yyyy-MM-dd");

	/**
	 * we consider only the first timezone and that which
	 * will be set last (/latest) which we will consider
	 * to be that of the evening
	 */
	public String otherTimeZone;

    @Deprecated
    public DayMetadataFacet() {}

    public DayMetadataFacet(String forDate) {
        long timeForDate = formatter.withZoneUTC().parseDateTime(forDate).getMillis();
        DateMidnight dateMidnight = new DateMidnight(timeForDate);
        start = dateMidnight.getMillis();
        end = start + DateTimeConstants.MILLIS_PER_DAY;
    }

    public DayMetadataFacet(List<VisitedCity> cities, String forDate) {
        this.cities = cities;
        VisitedCity mainVisitedCity = getMainVisitedCity();
        long cityTime = formatter.withZone(DateTimeZone.forID(mainVisitedCity.city.geo_timezone)).parseDateTime(mainVisitedCity.date).getMillis();
        long forDateTime = formatter.withZone(DateTimeZone.forID(mainVisitedCity.city.geo_timezone)).parseDateTime(forDate).getMillis();
        daysInferred = Days.daysBetween(new DateMidnight(forDateTime), new DateMidnight(cityTime)).getDays();
        timeZone = mainVisitedCity.city.geo_timezone;
        start = forDateTime;
        end = forDateTime + DateTimeConstants.MILLIS_PER_DAY;
    }

    public VisitedCity getMainVisitedCity() {
        for (VisitedCity city : cities) {
            if (city.apiKeyId==0)
                return city;
        }
        return null;
    }

    @Override
    protected void makeFullTextIndexable() {
        //To change body of implemented methods use File | Settings | File Templates.
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
