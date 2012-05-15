package com.fluxtream.services;

import java.util.List;
import java.util.TimeZone;

import com.fluxtream.connectors.google_latitude.LocationFacet;
import com.fluxtream.domain.metadata.City;
import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.domain.metadata.DayMetadataFacet.TravelType;
import com.fluxtream.domain.metadata.WeatherInfo;

public interface MetadataService {

	void setTimeZone(long guestId, String date, String timeZone);

	TimeZone getCurrentTimeZone(long guestId);

	TimeZone getTimeZone(long guestId, long time);

	TimeZone getTimeZone(long guestId, String date);
	
	void addTimeSpentAtHome(long guestId, long startTime, long endTime);

	City getMainCity(long guestId, DayMetadataFacet context);

	DayMetadataFacet getDayMetadata(long guestId, String date, boolean create);

	void setTraveling(long guestId, String date, TravelType travelType);

	LocationFacet getLastLocation(long guestId, long time);

	LocationFacet getNextLocation(long guestId, long time);

	TimeZone getTimeZone(double latitude, double longitude);

	DayMetadataFacet getLastDayMetadata(long guestId);
	
	void setDayCommentTitle(long guestId, String date, String title);
	
	void setDayCommentBody(long guestId, String date, String body);

    public City getClosestCity(double latitude, double longitude);

    List<City> getClosestCities(double latitude, double longitude,
                                double dist);

    List<WeatherInfo> getWeatherInfo(double latitude, double longitude,
                                     String date, int startMinute, int endMinute);
}
