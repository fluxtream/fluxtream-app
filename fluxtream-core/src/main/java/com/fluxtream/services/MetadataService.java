package com.fluxtream.services;

import java.util.List;
import java.util.TimeZone;

import com.fluxtream.connectors.google_latitude.LocationFacet;
import com.fluxtream.domain.metadata.City;
import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.domain.metadata.DayMetadataFacet.TravelType;
import com.fluxtream.domain.metadata.WeatherInfo;

public interface MetadataService {

	public void setTimeZone(long guestId, String date, String timeZone);

	public TimeZone getCurrentTimeZone(long guestId);

	public TimeZone getTimeZone(long guestId, long time);

	public TimeZone getTimeZone(long guestId, String date);
	
	public void addTimeSpentAtHome(long guestId, long startTime, long endTime);

	public City getMainCity(long guestId, DayMetadataFacet context);

	public DayMetadataFacet getDayMetadata(long guestId, String date, boolean create);

	public void addGuestLocation(long guestId, long time, float latitude, float longitude);

	public void setTraveling(long guestId, String date, TravelType travelType);

	public LocationFacet getLastLocation(long guestId, long time);

	public LocationFacet getNextLocation(long guestId, long time);

	public City getClosestCity(double latitude, double longitude);

	public List<City> getClosestCities(double latitude, double longitude,
			double dist);

	public TimeZone getTimeZone(double latitude, double longitude);

	public List<WeatherInfo> getWeatherInfo(double latitude, double longitude,
			String date, int startMinute, int endMinute);
	
	public DayMetadataFacet getLastDayMetadata(long guestId);
	
	public void setDayCommentTitle(long guestId, String date, String title);
	
	public void setDayCommentBody(long guestId, String date, String body);

}
