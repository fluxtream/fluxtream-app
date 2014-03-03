package org.fluxtream.services;

import java.util.List;
import java.util.TimeZone;
import java.util.TreeSet;
import org.fluxtream.connectors.location.LocationFacet;
import org.fluxtream.domain.metadata.City;
import org.fluxtream.domain.metadata.FoursquareVenue;
import org.fluxtream.domain.metadata.VisitedCity;
import org.fluxtream.domain.metadata.WeatherInfo;
import org.fluxtream.metadata.ArbitraryTimespanMetadata;
import org.fluxtream.metadata.DayMetadata;
import org.fluxtream.metadata.MonthMetadata;
import org.fluxtream.metadata.WeekMetadata;
import net.sf.json.JSONObject;
import org.springframework.transaction.annotation.Transactional;

public interface MetadataService {

	void setTimeZone(long guestId, String date, String timeZone);

    void resetDayMainCity(long guestId, String date);

    void setDayMainCity(long guestId, float latitude, float longitude, String date);

    void setDayMainCity(long guestId, long visitedCityId, String date);

	TimeZone getCurrentTimeZone(long guestId);

	TimeZone getTimeZone(long guestId, long time);

	TimeZone getTimeZone(long guestId, String date);

    ArbitraryTimespanMetadata getArbitraryTimespanMetadata(long guestId, long start, long end);

	DayMetadata getDayMetadata(long guestId, String date);

    WeekMetadata getWeekMetadata(long guestId, int year, int week);

    MonthMetadata getMonthMetadata(long guestId, int year, int month);

    List<DayMetadata> getAllDayMetadata(long guestId);

	LocationFacet getLastLocation(long guestId, long time);

	TimeZone getTimeZone(double latitude, double longitude);

    public City getClosestCity(double latitude, double longitude);

    List<City> getClosestCities(double latitude, double longitude,
                                double dist);

    List<WeatherInfo> getWeatherInfo(double latitude, double longitude,
                                     String date);

    public void rebuildMetadata(String username);

    public void updateLocationMetadata(long guestId, List<LocationFacet> locationResources);

    public FoursquareVenue getFoursquareVenue(String venueId);

    public TreeSet<String> getDatesForWeek(final int year, final int week);

    public TreeSet<String> getDatesForMonth(final int year, final int month);

    public List<VisitedCity> getConsensusCities(final long guestId, final TreeSet<String> dates);

    @Transactional(readOnly=false)
    JSONObject getFoursquareVenueJSON(String venueId);
}
