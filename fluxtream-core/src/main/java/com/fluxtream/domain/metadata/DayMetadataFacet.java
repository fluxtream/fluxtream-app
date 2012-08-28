package com.fluxtream.domain.metadata;

import java.util.Calendar;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Index;
import org.hibernate.search.annotations.Indexed;

import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.domain.AbstractFacet;

@Entity(name="ContextualInfo")
@NamedQueries ( {
	@NamedQuery( name="context.delete.all",
		query="DELETE FROM ContextualInfo context WHERE context.guestId=?"),
	@NamedQuery( name="context.day.when",
		query="SELECT context FROM ContextualInfo context WHERE context.guestId=? AND context.start<? and context.end>? ORDER BY context.start DESC"),
	@NamedQuery( name="context.day.last",
		query="SELECT context FROM ContextualInfo context WHERE context.guestId=? ORDER BY context.start DESC"),
	@NamedQuery( name="context.day.oldest",
		query="SELECT context FROM ContextualInfo context WHERE context.guestId=? ORDER BY context.start ASC"),
	@NamedQuery( name="context.day.next",
		query="SELECT context FROM ContextualInfo context WHERE context.guestId=? and context.start>? ORDER BY context.start ASC"),
	@NamedQuery( name="context.byDate",
		query="SELECT context FROM ContextualInfo context WHERE context.guestId=? and context.date=?"),
    @NamedQuery( name="context.all",
        query="SELECT context FROM ContextualInfo context WHERE context.guestId=? ORDER BY context.start ASC")
})
@Indexed
public class DayMetadataFacet extends AbstractFacet {
	
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
	
	public String title;
	
	public String date;
	
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
	
	public TimeInterval getTimeInterval() {
		return new TimeInterval(start, end, TimeUnit.DAY, TimeZone.getTimeZone(timeZone));
	}
	
	public Calendar getStartCalendar() {
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone(this.timeZone));
		c.setTimeInMillis(start);
		return c;
	}
	
	public Calendar getEndCalendar() {
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone(this.timeZone));
		c.setTimeInMillis(end);
		return c;
	}
	
	@Override
	protected void makeFullTextIndexable() {
		StringBuffer sb = new StringBuffer();
		
		Set<String> uniqueCities = new HashSet<String>();
		Set<String> uniqueStates = new HashSet<String>();
		Set<String> uniqueCountries = new HashSet<String>();
		
		if (cities!=null) {
			StringTokenizer st = new StringTokenizer(cities, "|");
			while (st.hasMoreTokens()) {
				String cityLabel = st.nextToken();
				String[] parts = cityLabel.split("/");
				if (parts.length==4) {
					uniqueCities.add(parts[0]);
					uniqueCountries.add(parts[1]);
				} else if (parts.length==5) {
					uniqueCities.add(parts[0]);
					uniqueStates.add(parts[1]);
					uniqueCountries.add(parts[2]);
				}
			}
		}
		for (String s : uniqueCities)
			sb.append(" ").append(s);
		for (String s : uniqueStates)
			sb.append(" ").append(s);
		for (String s : uniqueCountries)
			sb.append(" ").append(s);

        if (comment!=null)
    		sb.append(" ").append(comment);
        if (title!=null)
    		sb.append(" ").append(title);
				
		this.fullTextDescription = sb.toString().trim();
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
