package org.fluxtream.core.domain.metadata;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Index;

import org.fluxtream.core.domain.AbstractEntity;


@Entity(name="WeatherInfo")
//@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@NamedQueries({
		@NamedQuery(name = "weather.byDateAndCity.between", query = "SELECT weather FROM WeatherInfo " +
				"weather WHERE weather.city=? AND " +
				"weather.fdate=?")
})
public class WeatherInfo extends AbstractEntity implements Comparable<WeatherInfo> {

	@Index(name="city_index")
	public String city;
	
	@Index(name="fdate_index")
	public String fdate;
	
	@Index(name="minuteOfDay_index")
	public int minuteOfDay;
	
	public int cloudcover;
	public int humidity;
	public float precipMM;
	public int pressure;
	public int tempC;
	public int tempF;
	
	public int visibility;
	public int weatherCode;
	public String weatherDesc;
    public String weatherIconUrl;
	public String weatherIconUrlDay;
    public String weatherIconUrlNight;
	public String winddir16Point;
	public int winddirDegree;
	public int windspeedKmph;
	public int windspeedMiles;
	
	@Override
	public int compareTo(WeatherInfo wi) {
		return (wi.minuteOfDay > minuteOfDay)?-1:1;
	}
	
}
