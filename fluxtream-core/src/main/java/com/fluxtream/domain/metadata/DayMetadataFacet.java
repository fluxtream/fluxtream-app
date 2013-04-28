package com.fluxtream.domain.metadata;

import java.util.Calendar;
import java.util.NavigableSet;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;
import javax.persistence.Lob;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.domain.AbstractLocalTimeFacet;
import org.hibernate.annotations.Index;
public class DayMetadataFacet extends AbstractLocalTimeFacet {
	
	public class VisitedCity implements Comparable<VisitedCity> {

		public String name, country, state;
		public int count, population;
		
		@Override
		public int compareTo(VisitedCity other) {
			if (count>other.count) return 1;
			else if (count<other.count) return -1;
			else if (population>other.population) return 1;
			else return -1;
		}
		
	}

	@Lob
	public String cities;
	
	public float maxTempC = -10000,
			maxTempF = -10000,
			minTempC = 10000,
			minTempF = 10000;
	
	public String timeZone;

	/**
	 * we consider only the first timezone and that which
	 * will be set last (/latest) which we will consider
	 * to be that of the evening
	 */
	public String otherTimeZone;
	
	public enum TravelType {
		UNKNOWN, NONE, BUSINESS, LEISURE, BUSINESS_AND_LEISURE
	}
	
	public enum InTransitType {
		UNKNOWN, YES, NO
	}
	
	@Index(name="inTransit_index")
	public InTransitType inTransit = InTransitType.UNKNOWN;
	
	@Index(name="travelType_index")
	public TravelType travelType = TravelType.UNKNOWN;

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

	public NavigableSet<VisitedCity> getOrderedCities() {
		TreeSet<VisitedCity> cities = new TreeSet<VisitedCity>();
		if (this.cities==null) return cities;
		StringTokenizer st = new StringTokenizer(this.cities, "|");
		while(st.hasMoreTokens()) {
			String cityString = st.nextToken();
			VisitedCity city = new VisitedCity();
			String[] parts = cityString.split("/");
			city.name = parts[0];
			if (parts.length==5) {
				city.state = parts[1];
				city.country = parts[2];
				city.population = Integer.valueOf(parts[3]);
				city.count = Integer.valueOf(parts[4]);
			} else if (parts.length==4) {
				city.country = parts[1];
				city.population = Integer.valueOf(parts[2]);
				city.count = Integer.valueOf(parts[3]);
			}
			cities.add(city);
		}
		return cities;
	}
		
}
