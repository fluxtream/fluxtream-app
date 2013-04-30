package com.fluxtream.domain.metadata;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.domain.AbstractLocalTimeFacet;
public class DayMetadataFacet extends AbstractLocalTimeFacet {
	
	public float maxTempC = -10000,
			maxTempF = -10000,
			minTempC = 10000,
			minTempF = 10000;
	
	public String timeZone = "UTC";

    public transient List<VisitedCity> cities;
    public transient int daysInferred = 0;

	/**
	 * we consider only the first timezone and that which
	 * will be set last (/latest) which we will consider
	 * to be that of the evening
	 */
	public String otherTimeZone;

    public DayMetadataFacet() {}

    public DayMetadataFacet(long apiKeyId) { this.apiKeyId = apiKeyId; }

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

	public List<VisitedCity> getOrderedCities() {
		return cities;
	}
		
}
