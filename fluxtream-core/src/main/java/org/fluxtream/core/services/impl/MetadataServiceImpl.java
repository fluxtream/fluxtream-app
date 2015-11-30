package org.fluxtream.core.services.impl;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.TimezoneMap;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.location.LocationFacet;
import org.fluxtream.core.connectors.vos.AbstractFacetVO;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.metadata.City;
import org.fluxtream.core.domain.metadata.FoursquareVenue;
import org.fluxtream.core.domain.metadata.VisitedCity;
import org.fluxtream.core.domain.metadata.WeatherInfo;
import org.fluxtream.core.metadata.ArbitraryTimespanMetadata;
import org.fluxtream.core.metadata.DayMetadata;
import org.fluxtream.core.metadata.MonthMetadata;
import org.fluxtream.core.metadata.WeekMetadata;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.MetadataService;
import org.fluxtream.core.services.NotificationsService;
import org.fluxtream.core.thirdparty.helpers.WWOHelper;
import org.fluxtream.core.utils.HttpUtils;
import org.fluxtream.core.utils.JPAUtils;
import org.fluxtream.core.utils.TimeUtils;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Component
@Transactional(readOnly=true)
public class MetadataServiceImpl implements MetadataService {

    FlxLogger logger = FlxLogger.getLogger(MetadataServiceImpl.class);

    // threshold range in meters to consider successive location points to be in the same city
    // without checking
    private static final float CITY_RANGE = 1000.f;

	@Autowired
	Configuration env;

	@PersistenceContext
	EntityManager em;

	@Autowired
	GuestService guestService;

	@Autowired
	NotificationsService notificationsService;

    @Autowired
    WWOHelper wwoHelper;

    @Override
	public TimeZone getTimeZone(double latitude, double longitude) {
		City closestCity = getClosestCity(latitude, longitude);
		TimeZone timeZone = TimeZone.getTimeZone(closestCity.geo_timezone);
		return timeZone;
	}

	@Override
	public TimeZone getCurrentTimeZone(long guestId) {
		LocationFacet lastLocation = getLastLocation(guestId, System.currentTimeMillis());
		if (lastLocation != null) {
			TimeZone timeZone = getTimeZone(lastLocation.latitude,
					lastLocation.longitude);
			return timeZone;
		}
		return null;
	}

	@Override
	@Transactional(readOnly = false)
	public void setTimeZone(long guestId, String date, String timeZone) {
        logger.warn("component=metadata action=setTimeZone message=attempt to set timezone");
	}

    @Override
    @Transactional(readOnly = false)
    public void resetDayMainCity(final long guestId, final String date) {
        final List<VisitedCity> visitedCities = getVisitedCitiesForDate(guestId, date);
        for (VisitedCity visitedCity : visitedCities) {
            if (visitedCity.locationSource== LocationFacet.Source.USER)
                em.remove(visitedCity);
        }
        em.flush();
    }

    @Override
    public void setDayMainCity(final long guestId, final float latitude, final float longitude, final String date) {
        final City closestCity = getClosestCity(latitude, longitude);
        setDayMainCity(guestId, date, closestCity);
    }

    @Override
    public void setDayMainCity(final long guestId, final long visitedCityId, final String date) {
        final VisitedCity visitedCity = em.find(VisitedCity.class, visitedCityId);
        setDayMainCity(guestId, date, visitedCity.city);
    }

    private void setDayMainCity(final long guestId, final String date, final City closestCity) {
        clearMainCities(guestId, Arrays.asList(date));
        final DateTime dateTime = TimeUtils.dateFormatter.withZone(DateTimeZone.forID(closestCity.geo_timezone)).parseDateTime(date);
        setMainCity(guestId, closestCity, dateTime.getMillis(), dateTime.getMillis() + DateTimeConstants.MILLIS_PER_DAY - 1, date);
    }

    private void clearMainCities(final long guestId, final Collection<String> dates) {
        TypedQuery<VisitedCity> query = em.createQuery("SELECT facet FROM " +
                JPAUtils.getEntityName(VisitedCity.class) +
                " facet WHERE facet.guestId=:guestId AND facet.locationSource=:source  AND facet.date IN :dates" +
                " ORDER BY facet.start", VisitedCity.class);
        query.setParameter("guestId", guestId);
        query.setParameter("source", LocationFacet.Source.USER);
        query.setParameter("dates", dates);
        final List<VisitedCity> resultList = query.getResultList();
        for (VisitedCity city : resultList) {
            em.remove(city);
        }
    }

    @Transactional(readOnly=false)
    private void setMainCity(final long guestId, final City city, final long start, final long end, final String timePeriod) {

        resetDayMainCity(guestId, timePeriod);

        VisitedCity visitedCity = new VisitedCity();
        visitedCity.guestId = guestId;
        visitedCity.date = timePeriod;
        visitedCity.locationSource = LocationFacet.Source.USER;
        visitedCity.city = city;
        visitedCity.start = start;
        visitedCity.end = end;
        visitedCity.count = 1;
        visitedCity.startTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.withZone(DateTimeZone.forID(city.geo_timezone)).print(start);
        visitedCity.endTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.withZone(DateTimeZone.forID(city.geo_timezone)).print(end);
        em.persist(visitedCity);
    }

    @Override
    public ArbitraryTimespanMetadata getArbitraryTimespanMetadata(final long guestId, final long start, final long end) {
        final TreeSet<String> dates = getDatesBetween(start, end);
        List<VisitedCity> cities = getVisitedCitiesForDates(guestId, dates);
        VisitedCity previousInferredCity = null, nextInferredCity = null;
        if (cities.size()==0) {
            previousInferredCity = searchCityBeforeDate(guestId, dates.first());
            nextInferredCity = searchCityAfterDate(guestId, dates.last());
            if (previousInferredCity==null&&nextInferredCity==null) {
                ArbitraryTimespanMetadata info = new ArbitraryTimespanMetadata(start, end);
                return info;
            }
        }
        final VisitedCity consensusVisitedCity = getConsensusVisitedCity(cities, previousInferredCity, nextInferredCity);
        final List<DayMetadata> dayMetadataForDates = getDayMetadataForDates(guestId, dates);
        final TreeMap<String, TimeZone> consensusTimezoneMap = getConsensusTimezoneMap(dayMetadataForDates);
        final List<VisitedCity> consensusCities = extractConsensusCities(dayMetadataForDates);
        TimezoneMap timezoneMap = TimezoneMap.fromConsensusTimezoneMap(consensusTimezoneMap);
        ArbitraryTimespanMetadata info = new ArbitraryTimespanMetadata(consensusVisitedCity, previousInferredCity, nextInferredCity, consensusTimezoneMap, timezoneMap, cities, consensusCities, start, end);
        return info;
    }

    TreeMap<String,TimeZone> getConsensusTimezoneMap(List<DayMetadata> metadata) {
        TreeMap<String, TimeZone> tzMap = new TreeMap<String, TimeZone>();
        for (DayMetadata dayMetadata : metadata)
            tzMap.put(dayMetadata.date, TimeZone.getTimeZone(dayMetadata.consensusVisitedCity.city.geo_timezone));
        return tzMap;
    }

    List<DayMetadata> getDayMetadataForDates(final long guestId, final TreeSet<String> dates) {
        List<DayMetadata> metadata = new ArrayList<DayMetadata>();
        for (String date : dates) {
            final DayMetadata dayMetadata = getDayMetadata(guestId, date);
            metadata.add(dayMetadata);
        }
        return metadata;
    }

    List<VisitedCity> extractConsensusCities(List<DayMetadata> metadata) {
        List<VisitedCity> visitedCities = new ArrayList<VisitedCity>();
        for (DayMetadata dayMetadata : metadata) {
            visitedCities.add(dayMetadata.consensusVisitedCity);
        }
        return visitedCities;
    }

    TreeSet<String> getDatesBetween(long start, final long end) {
        TreeSet<String> dates = new TreeSet<String>();
        String startDate = TimeUtils.dateFormatter.print(start-DateTimeConstants.MILLIS_PER_DAY/2);
        String endDate = TimeUtils.dateFormatter.print(end + DateTimeConstants.MILLIS_PER_DAY/2);
        dates.add(startDate);
        for(;!startDate.equals(endDate);start+=DateTimeConstants.MILLIS_PER_DAY) {
            startDate = TimeUtils.dateFormatter.print(start);
            dates.add(startDate);
        }
        return dates;
    }

    @Override
	public DayMetadata getDayMetadata(long guestId, String date) {
        // get visited cities for a specific date . If we don't have any data for that date,
        // retrieve cities for the first date for which we do have data
        List<VisitedCity> cities = getVisitedCitiesForDate(guestId, date);
        VisitedCity previousInferredCity = null, nextInferredCity = null;
        if (cities.size()==0) {
            previousInferredCity = searchCityBeforeDate(guestId, date);
            nextInferredCity = searchCityAfterDate(guestId, date);
            if (previousInferredCity==null&&nextInferredCity==null) {
                DayMetadata info = new DayMetadata(date);
                return info;
            }
        }
        final VisitedCity consensusVisitedCity = getConsensusVisitedCity(cities, previousInferredCity, nextInferredCity);
        TreeMap<String, TimeZone> consensusTimezoneMap = new TreeMap<String, TimeZone>();
        consensusTimezoneMap.put(date, TimeZone.getTimeZone(consensusVisitedCity.city.geo_timezone));
        TimezoneMap timezoneMap = TimezoneMap.fromConsensusTimezoneMap(consensusTimezoneMap);
        List<VisitedCity> consensusVisitedCities = Arrays.asList(consensusVisitedCity);
        DayMetadata info = new DayMetadata(consensusVisitedCity, previousInferredCity, nextInferredCity, consensusTimezoneMap, timezoneMap, cities, consensusVisitedCities, date);
        return info;
    }

    private int daysBetween(String date, VisitedCity vcity) {
        final DateTime wantedDate = TimeUtils.dateFormatter.withZone(DateTimeZone.forID(vcity.city.geo_timezone)).parseDateTime(date);
        final DateTime availableDate = TimeUtils.dateFormatter.withZone(DateTimeZone.forID(vcity.city.geo_timezone)).parseDateTime(vcity.date);
        final int days = Days.daysBetween(wantedDate, availableDate).getDays();
        return days;
    }

    @Override
    public WeekMetadata getWeekMetadata(final long guestId, final int year, final int week) {
        TreeSet<String> dates = getDatesForWeek(year, week);
        List<VisitedCity> cities = getVisitedCitiesForDates(guestId, dates);
        VisitedCity previousInferredCity = null, nextInferredCity = null;
        if (cities.size()==0) {
            previousInferredCity = searchCityBeforeDate(guestId, dates.first());
            nextInferredCity = searchCityAfterDate(guestId, dates.last());
            if (previousInferredCity==null&&nextInferredCity==null) {
                WeekMetadata info = new WeekMetadata(year, week);
                return info;
            }
        }
        final VisitedCity consensusVisitedCity = getConsensusVisitedCity(cities, previousInferredCity, nextInferredCity);
        final List<DayMetadata> dayMetadataForDates = getDayMetadataForDates(guestId, dates);
        final TreeMap<String, TimeZone> consensusTimezoneMap = getConsensusTimezoneMap(dayMetadataForDates);
        final List<VisitedCity> consensusCities = extractConsensusCities(dayMetadataForDates);
        TimezoneMap timezoneMap = TimezoneMap.fromConsensusTimezoneMap(consensusTimezoneMap);
        WeekMetadata info = new WeekMetadata(consensusVisitedCity, previousInferredCity, nextInferredCity, consensusTimezoneMap, timezoneMap, cities, consensusCities, year, week);
        return info;
    }

    public List<VisitedCity> getConsensusCities(final long guestId, final TreeSet<String> dates) {
        List<VisitedCity> consensusCities = new ArrayList<VisitedCity>();
        Collections.sort(consensusCities,
            new Comparator<VisitedCity>(){
                @Override
                public int compare(final VisitedCity o1, final VisitedCity o2) {
                    return o1.date.compareTo(o2.date);
                }
            });
        for (String date : dates) {
            final DayMetadata dayMetadata = getDayMetadata(guestId, date);
            final VisitedCity consensusVisitedCity = dayMetadata.consensusVisitedCity;
            // Explicitely set the date on this visitedCity to enable time boundaries checking
            VisitedCity copy = new VisitedCity(consensusVisitedCity);
            copy.setDate(date);
            copy.start = copy.getDayStart();
            copy.end = copy.getDayEnd();
            consensusCities.add(copy);
        }
        return consensusCities;
    }

    @Override
    public MonthMetadata getMonthMetadata(final long guestId, final int year, final int month) {
        TreeSet<String> dates = getDatesForMonth(year, month);
        List<VisitedCity> cities = getVisitedCitiesForDates(guestId, dates);
        VisitedCity previousInferredCity = null, nextInferredCity = null;
        if (cities.size()==0) {
            previousInferredCity = searchCityBeforeDate(guestId, dates.first());
            nextInferredCity = searchCityAfterDate(guestId, dates.last());
            if (previousInferredCity==null && nextInferredCity==null) {
                MonthMetadata info = new MonthMetadata(year, month);
                return info;
            }
        }
        final VisitedCity consensusVisitedCity = getConsensusVisitedCity(cities, previousInferredCity, nextInferredCity);
        final List<DayMetadata> dayMetadataForDates = getDayMetadataForDates(guestId, dates);
        final TreeMap<String, TimeZone> consensusTimezoneMap = getConsensusTimezoneMap(dayMetadataForDates);
        final List<VisitedCity> consensusCities = extractConsensusCities(dayMetadataForDates);
        TimezoneMap timezoneMap = TimezoneMap.fromConsensusTimezoneMap(consensusTimezoneMap);
        MonthMetadata info = new MonthMetadata(consensusVisitedCity, previousInferredCity, nextInferredCity, consensusTimezoneMap, timezoneMap, cities, consensusCities, year, month);
        return info;
    }

    public TreeSet<String> getDatesForWeek(final int year, final int week) {
        LocalDate weekDay = TimeUtils.getBeginningOfWeek(year, week);
        final LocalDate nextWeekStart = weekDay.plusWeeks(1);
        TreeSet<String> dates = new TreeSet<String>();
        while(weekDay.isBefore(nextWeekStart)) {
            final String date = TimeUtils.dateFormatterUTC.print(weekDay);
            dates.add(date);
            weekDay = weekDay.plusDays(1);
        }
        return dates;
    }

    public TreeSet<String> getDatesForMonth(final int year, final int month) {
        LocalDate dayOfMonth = TimeUtils.getBeginningOfMonth(year, month);
        final LocalDate nextMonthStart = dayOfMonth.plusMonths(1);
        TreeSet<String> dates = new TreeSet<String>();
        while(dayOfMonth.isBefore(nextMonthStart)) {
            final String date = TimeUtils.dateFormatterUTC.print(dayOfMonth);
            dates.add(date);
            dayOfMonth = dayOfMonth.plusDays(1);
        }
        return dates;
    }

    public List<VisitedCity> getVisitedCitiesForDate(final long guestId, final String date) {
        TypedQuery<VisitedCity> query = em.createQuery("SELECT facet FROM " + JPAUtils.getEntityName(VisitedCity.class) + " facet WHERE facet.guestId=? AND facet.date=?" + " ORDER BY facet.start", VisitedCity.class);
        query.setParameter(1, guestId);
        query.setParameter(2, date);
        final List<VisitedCity> cities = query.getResultList();
        return cities;
    }

    public List<VisitedCity> getVisitedCitiesForDates(final long guestId, final TreeSet<String> dates) {
        TypedQuery<VisitedCity> query = em.createQuery("SELECT facet FROM " + JPAUtils.getEntityName(VisitedCity.class) + " facet WHERE facet.guestId=:guestId AND facet.date IN :dates" + " ORDER BY facet.start", VisitedCity.class);
        query.setParameter("guestId", guestId);
        query.setParameter("dates", dates);
        List<VisitedCity> cities = query.getResultList();
        return cities;
    }

    /**
     * Get only the consensus city. Return user's preference if it has been set, otherwise return
     * the city where the user has spent the most time.
     *
     * @param cities
     * @return
     */
    private VisitedCity getConsensusVisitedCity(final List<VisitedCity> cities, final VisitedCity previousInferredCity, final VisitedCity nextInferredCity) {

        for (VisitedCity city : cities)
            if (city.locationSource== LocationFacet.Source.USER)
                return city;

        if (previousInferredCity!=null&&nextInferredCity!=null) {
            if (Math.abs(previousInferredCity.daysInferred)>Math.abs(nextInferredCity.daysInferred))
                return nextInferredCity;
            else
                return previousInferredCity;
        } else if (previousInferredCity!=null)
            return previousInferredCity;
        else if (nextInferredCity!=null)
            return nextInferredCity;

        List<VisitedCity> cityList = new ArrayList<VisitedCity>(cities);
        Collections.sort(cityList, new Comparator<VisitedCity>() {
            @Override
            public int compare(final VisitedCity a, final VisitedCity b) {
                int timeSpentInA = (int) (a.end - a.start + 1); //add one if start and end are equal
                int timeSpentInB = (int) (b.end - b.start + 1);
                return timeSpentInB - timeSpentInA;
            }
        });

        if (cityList.size()>0)
            return cityList.get(0);
        return null;
    }

    private String findClosestKnownDateForTime(final long guestId, final long time) {
        VisitedCity existingCity = searchCityBefore(guestId, time);
        if (existingCity==null)
            existingCity = searchCityAfter(guestId, time);
        if (existingCity!=null) {
            return existingCity.date;
        }
        return TimeUtils.dateFormatterUTC.print(time);
    }

    private VisitedCity searchCityBefore(final long guestId, final long instant) {
        return searchCity("SELECT facet FROM " + JPAUtils.getEntityName(VisitedCity.class) + " facet WHERE facet.guestId=? AND facet.start<? ORDER BY facet.start", guestId, instant);
    }

    private VisitedCity searchCityAfter(final long guestId, final long instant) {
        return searchCity("SELECT facet FROM " + JPAUtils.getEntityName(VisitedCity.class) + " facet WHERE facet.guestId=? AND facet.start>? ORDER BY facet.start", guestId, instant);
    }

    private VisitedCity searchCity(final String queryString, final long guestId, final long instant) {
        final TypedQuery<VisitedCity> query = em.createQuery(queryString, VisitedCity.class);
        return getVisitedCity(guestId, instant, query);
    }

    private VisitedCity searchCity(final String queryString, final long guestId, final String date) {
        final TypedQuery<VisitedCity> query = em.createQuery(queryString, VisitedCity.class);
        return getVisitedCity(guestId, date, query);
    }

    private VisitedCity getVisitedCity(final long guestId, final String date, final TypedQuery<VisitedCity> query) {
        query.setMaxResults(1);
        query.setParameter(1, guestId);
        query.setParameter(2, date);
        final List<VisitedCity> cities = query.getResultList();
        if (cities.size()>0)
            return cities.get(0);
        return null;
    }

    private VisitedCity getVisitedCity(final long guestId, final long instant, final TypedQuery<VisitedCity> query) {
        query.setMaxResults(1);
        query.setParameter(1, guestId);
        query.setParameter(2, instant);
        final List<VisitedCity> cities = query.getResultList();
        if (cities.size()>0)
            return cities.get(0);
        return null;
    }

    private VisitedCity searchCityBeforeDate(final long guestId, final String date) {
        VisitedCity visitedCity = searchCity("SELECT facet FROM " + JPAUtils.getEntityName(VisitedCity.class) + " facet WHERE facet.guestId=? AND facet.date<? ORDER BY facet.start DESC", guestId, date);
        if (visitedCity!=null) {
            final List<VisitedCity> visitedCitiesForDate = getVisitedCitiesForDate(guestId, visitedCity.date);
            visitedCity = getConsensusVisitedCity(visitedCitiesForDate, null, null);
            visitedCity.daysInferred = daysBetween(date, visitedCity);
        }
        return visitedCity;
    }

    private VisitedCity searchCityAfterDate(final long guestId, final String date) {
        VisitedCity visitedCity = searchCity("SELECT facet FROM " + JPAUtils.getEntityName(VisitedCity.class) + " facet WHERE facet.guestId=? AND facet.date>? ORDER BY facet.start", guestId, date);
        if (visitedCity!=null) {
            final List<VisitedCity> visitedCitiesForDate = getVisitedCitiesForDate(guestId, visitedCity.date);
            visitedCity = getConsensusVisitedCity(visitedCitiesForDate, null, null);
            visitedCity.daysInferred = daysBetween(date, visitedCity);
        }
        return visitedCity;
    }

    @Override
    public List<DayMetadata> getAllDayMetadata(final long guestId) {
        return JPAUtils.find(em, DayMetadata.class,"context.all",guestId);
    }

	@Override
	public LocationFacet getLastLocation(long guestId, long time) {
		LocationFacet lastSeen = JPAUtils.findUnique(em, LocationFacet.class,
                "location.lastSeen", guestId, time);
		return lastSeen;
	}

	@Override
	public TimeZone getTimeZone(long guestId, String date) {
        final DayMetadata dayMetadata = getDayMetadata(guestId, date);
        return dayMetadata.getTimeInterval().getMainTimeZone();
	}

    @Override
	public TimeZone getTimeZone(long guestId, long time) {
        String date = findClosestKnownDateForTime(guestId, time);
        return getTimeZone(guestId, date);
	}

    @Override
    public City getClosestCity(double latitude, double longitude) {

        List<City> cities = new ArrayList<City>();
        for (int dist = 10, i = 1; cities.size() == 0;)
            cities = getClosestCities(latitude, longitude,
                                      Double.valueOf(dist ^ i++));

        return cities.get(0);
    }

    @Override
    public List<City> getClosestCities(double latitude, double longitude,
                                       double dist) {

        double lon1 = longitude - dist
                                  / Math.abs(Math.cos(Math.toRadians(latitude)) * 69d);
        double lon2 = longitude + dist
                                  / Math.abs(Math.cos(Math.toRadians(latitude)) * 69d);
        double lat1 = latitude - (dist / 69.d);
        double lat2 = latitude + (dist / 69.d);

        Query query = em
                .createNativeQuery(
                        "SELECT cities1000.geo_id, cities1000.geo_name, "
                        + "cities1000.geo_timezone, cities1000.geo_latitude, "
                        + "cities1000.geo_longitude, cities1000.geo_country_code, "
                        + "cities1000.geo_admin1_code, cities1000.population, "
                        + "3956 * 2 * ASIN(SQRT(POWER(SIN((:mylat - geo_latitude) * pi()/180 / 2), 2) + COS(:mylat * pi()/180) *COS(geo_latitude * pi()/180) * POWER(SIN((:mylon -geo_longitude) * pi()/180 / 2), 2))) as distance "
                        + "FROM cities1000 "
                        + "WHERE geo_longitude between :lon1 and :lon2 "
                        + "and geo_latitude between :lat1 and :lat2 "
                        + "HAVING distance < :dist ORDER BY distance limit 1;",
                        City.class);

        query.setParameter("mylat", latitude);
        query.setParameter("mylon", longitude);
        query.setParameter("lon1", lon1);
        query.setParameter("lon2", lon2);
        query.setParameter("lat1", lat1);
        query.setParameter("lat2", lat2);
        query.setParameter("dist", dist);

        @SuppressWarnings("unchecked")
        List<City> resultList = query.getResultList();
        return resultList;
    }

    @Override
    public List<WeatherInfo> getWeatherInfo(double latitude, double longitude,
                                            String date) {
        City closestCity = getClosestCity(latitude, longitude);
        List<WeatherInfo> weather = JPAUtils.find(em, WeatherInfo.class, "weather.byDateAndCity.between", closestCity.geo_name, date);

        if (weather != null && weather.size() > 0) {
            addIcons(weather);
            return weather;
        }
        else {
            try {
                fetchWeatherInfo(latitude, longitude, closestCity.geo_name,
                                 date);
            } catch (Exception e) {
                logger.warn("action=fetchWeather error date="+ date+", lat=" + latitude + ", lon=" +longitude+", city="+closestCity.geo_name);
            }
            weather = JPAUtils.find(em, WeatherInfo.class,
                                    "weather.byDateAndCity.between", closestCity.geo_name,
                                    date);
            addIcons(weather);
        }
        return weather;
    }

    @Override
    public void rebuildMetadata(final String username) {
        Guest guest = null;
        // Accept guest ID as well as username
        try {
            guest = guestService.getGuest(username);
            if(guest==null) {
                // Try to treat arg as guestId
                Long guestId = Long.valueOf(username);
                if(guestId!=null) {
                    guest = guestService.getGuestById(guestId);
                }
            }
        }
        catch (Exception e) {
            // Might get exception if username doesn't exist and non-numeric
            guest=null;
        }
        // Check if we succeeded, return.  This isn't really right because we don't get
        // error reporting, but would take to long to fix error reporting right now.
        // TODO: fix error reporting
        if(guest==null) {
            return;
        }

        String entityName = JPAUtils.getEntityName(LocationFacet.class);
        final Query nativeQuery = em.createNativeQuery(String.format("SELECT DISTINCT apiKeyId FROM %s WHERE guestId=%s", entityName, guest.getId()));
        final List<BigInteger> resultList = nativeQuery.getResultList();
        for (BigInteger apiKeyId : resultList) {
            if(apiKeyId!=null && apiKeyId.longValue()>0) {
                rebuildMetadata(guest.username, apiKeyId.longValue());
            }
        }
    }

    private void rebuildMetadata(final String username, long apiKeyId) {
        final Guest guest = guestService.getGuest(username);
        String entityName = JPAUtils.getEntityName(LocationFacet.class);
        int i=0;
        final Query facetsQuery = em.createQuery(String.format("SELECT facet FROM %s facet WHERE facet.apiKeyId=? ORDER BY facet.start ASC", entityName));
        facetsQuery.setParameter(1, apiKeyId);
        while(true) {
            facetsQuery.setFirstResult(i);
            facetsQuery.setMaxResults(1000);
            final List<LocationFacet> rawLocations = facetsQuery.getResultList();
            //System.out.println(username + ": retrieved " + rawLocations.size() + " location datapoints (offset is " + i + ")");
            if (rawLocations.size()==0)
                break;
            //System.out.println(username + ":   " + AbstractLocalTimeFacet.timeStorageFormat.withZoneUTC().print(rawLocations.get(0).start));

            long then = System.currentTimeMillis();
            // Loop over the points to see if they're already included in visited cities entries
            // it's important that the location points in the locations list are for a single apiKeyId
            // and are in forward chronological order.  Only add locations that aren't already contained
            // within a VisitedCity item to the newLocations list
            List<LocationFacet> newLocations=new ArrayList<LocationFacet>();
            VisitedCity existingVisitedCityRecord = null;

            for (LocationFacet locationFacet : rawLocations) {
                // Check to see if this location is in the current existingVisitedCityRecord (if any)
                if(existingVisitedCityRecord !=null && locationFacet.start <=existingVisitedCityRecord.end) {
                    // This location falls within the last fetched visited cities record, skip it
                    continue;
                }
                // This location doesn't fall within the last fetched visited cities record (if any).
                // See if it fits in a new one.  Note that this really assumes that locationFacet.start
                // and locationFacet.end are the same and VisitedCity records are non-overlapping.
                // It returns null if there are no visited city
                // records overlapping the current point and non-null if there is one.
                existingVisitedCityRecord = JPAUtils.findUnique(em, VisitedCity.class,
                                                              "visitedCities.byApiAndTime",
                                                              locationFacet.apiKeyId,
                                                              locationFacet.start,
                                                              locationFacet.end);
                if(existingVisitedCityRecord == null) {
                    // This is a new point, add it
                    newLocations.add(locationFacet);
                }
                else {
                    // This point is already covered, skip it
                }
            }

            if(newLocations.size()>0) {
                long start = newLocations.get(0).start;
                updateLocationMetadata(guest.getId(), newLocations);
            }
            else {
                System.out.println(username + ": no new " + entityName + " location datapoints (offset is " + i + ")");
            }

            long now = System.currentTimeMillis();
            System.out.println(String.format(username + ":   updateLocationMetadata took %s ms to complete", (now - then)));
            i+=rawLocations.size();
        }
    }

    /**
     * We want to have an explicit city "check-in" each time that we know we have been some place.
     * Thus, as we loop through a batch of new location datapoints, we keep track of the current date
     * and the current city and when either change we store a new datapoint in the VisitedCity table.
     * Then, for each date in the
     * @param guestId
     * @param locationResources
     */
    @Override
    @Transactional(readOnly=false)
    public void updateLocationMetadata(final long guestId, final List<LocationFacet> locationResources) {
        if (locationResources == null || locationResources.size()==0)
            return;
        // sort the location data in ascending time order
        try {
            Collections.sort(locationResources, new Comparator<LocationFacet>() {
                @Override
                public int compare(final LocationFacet o1, final LocationFacet o2) {
                    if (o1.start > o2.start)
                        return 1;
                    else if (o2.start > o2.start)
                        return -1;
                    else
                        return 0;
                }
            });
        } catch (Throwable t) {
            logger.warn("Could not sort location array: " + ExceptionUtils.getStackTrace(t));
            t.printStackTrace();
        }
        // local vars: current city and current day
        String currentDate = "";
        Point2D.Double anchorLocation = new Point2D.Double(locationResources.get(0).latitude, locationResources.get(0).longitude);
        City anchorCity = getClosestCity(anchorLocation.x, anchorLocation.y);
        int count = 0;
        LocationFacet lastLocationResourceMatchingAnchor=locationResources.get(0);
        long start = locationResources.get(0).start;

        for (LocationFacet locationResource : locationResources) {
            try {
                City newCity = anchorCity;
                Point2D.Double location = new Point2D.Double(locationResource.latitude, locationResource.longitude);
                boolean withinAnchorRange = isWithinRange(location, anchorLocation);

                if (!withinAnchorRange) {
                    anchorLocation = new Point2D.Double(locationResource.latitude, locationResource.longitude);
                    newCity = getClosestCity(locationResource.latitude, locationResource.longitude);
                }

                String newDate = TimeUtils.dateFormatter.withZone(DateTimeZone.forID(newCity.geo_timezone)).print(locationResource.timestampMs);
                final boolean dateChanged = !newDate.equals(currentDate);
                final boolean cityChanged = newCity.geo_id!=anchorCity.geo_id;
                if (dateChanged||cityChanged) {
                    if (count>0)
                        storeCityInfo(lastLocationResourceMatchingAnchor, currentDate, anchorCity, start, count);
                    anchorCity = newCity;
                    start = locationResource.start;
                    count = 0;
                }
                count++;
                // update count on the last location before we finish
                if (locationResources.indexOf(locationResource)==locationResources.size()-1)
                    storeCityInfo(locationResource, newDate, newCity, start, count);
                currentDate = newDate;
                lastLocationResourceMatchingAnchor = locationResource;
            }
            catch (Exception e){
                System.err.println("Exception occurred trying to store location metadata. Skipping datapoint...");
                e.printStackTrace();
            }
        }
        em.flush();
    }

    @Override
    @Transactional(readOnly=false)
    public JSONObject getFoursquareVenueJSON(final String venueId) {
        String url = String.format("https://api.foursquare.com/v2/venues/%s?client_id=%s&client_secret=%s&v=20130624", venueId,
                env.get("foursquare.client.id"), env.get("foursquare.client.secret"));
        try {
            final String fetched = HttpUtils.fetch(url);
            JSONObject json = JSONObject.fromObject(fetched);
            return json;
        }
        catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }
    }

        @Override
    @Transactional(readOnly=false)
    public FoursquareVenue getFoursquareVenue(final String venueId) {
        final TypedQuery<FoursquareVenue> query = em.createQuery("SELECT venue FROM FoursquareVenue venue WHERE venue.foursquareId=:venueId", FoursquareVenue.class);
        query.setParameter("venueId", venueId);
        final List<FoursquareVenue> resultList = query.getResultList();
        if (resultList.size()>0)
            return resultList.get(0);
        else {
            String url = String.format("https://api.foursquare.com/v2/venues/%s?client_id=%s&client_secret=%s&v=20130624", venueId,
                                       env.get("foursquare.client.id"), env.get("foursquare.client.secret"));
            try {
                final String fetched = HttpUtils.fetch(url);
                JSONObject json = JSONObject.fromObject(fetched);
                if (!json.has("meta"))
                    throw new Exception("no meta information");
                JSONObject meta = json.getJSONObject("meta");
                final int code = meta.getInt("code");
                if (code!=200)
                    throw new Exception("error code is " + code);
                JSONObject responseWrapper = json.getJSONObject("response");
                JSONObject response = responseWrapper.getJSONObject("venue");

                // we only use the primary category and flatten its information in the venue
                final Object categoriesObject = response.get("categories");
                JSONArray categories = null;
                if (categoriesObject instanceof JSONArray)
                    categories = (JSONArray) categoriesObject;
                else {
                    categories = new JSONArray();
                    JSONObject categoriesJson = (JSONObject)categoriesObject;
                    categories.add(categoriesJson);
                }
                FoursquareVenue venue = new FoursquareVenue();

                String venueName = response.getString("name");
                String canonicalUrl = response.getString("canonicalUrl");
                venue.name = venueName;
                venue.canonicalUrl = canonicalUrl;
                venue.foursquareId = venueId;

                for(int i=0; i<categories.size(); i++) {
                    JSONObject categoryInfo = categories.getJSONObject(i);
                    if (categoryInfo.has("primary")&&categoryInfo.getBoolean("primary")) {
                        if (categoryInfo.has("id"))
                            venue.categoryFoursquareId = categoryInfo.getString("id");
                        if (categoryInfo.has("name"))
                            venue.categoryName = categoryInfo.getString("name");
                        if (categoryInfo.has("shortName"))
                            venue.categoryShortName = categoryInfo.getString("shortName");
                        if (categoryInfo.has("icon")) {
                            JSONObject iconJson = categoryInfo.getJSONObject("icon");
                            venue.categoryIconUrlPrefix = iconJson.getString("prefix");
                            venue.categoryIconUrlSuffix = iconJson.getString("suffix");
                        }
                    }
                }
                em.persist(venue);
                return venue;
            }
            catch (Exception e) {
                logger.warn("action=getFoursquareVenue venueId=" + venueId + " message=" + e.getMessage());
            }
        }
        return null;
    }

    private void storeCityInfo(final LocationFacet locationResource, String date, final City city, final long start, final int count) {
        VisitedCity previousRecord = JPAUtils.findUnique(em, VisitedCity.class,
                                                         "visitedCities.byApiDateAndCity",
                                                         locationResource.guestId,
                                                         locationResource.apiKeyId,
                                                         date,
                                                         city.geo_id);
        if (previousRecord==null) {
            persistCity(locationResource, date, start, count, city);
        } else {
            // update start time and end time if necessary
            if (start<previousRecord.start) {
                previousRecord.start = start;
                previousRecord.startTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.withZone(DateTimeZone.forID(city.geo_timezone)).print(start);
            }
            if (locationResource.end>previousRecord.end) {
                previousRecord.end = locationResource.end;
                previousRecord.endTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.withZone(DateTimeZone.forID(city.geo_timezone)).print(locationResource.end);
            }
            // update timeUpdated
            previousRecord.timeUpdated = System.currentTimeMillis();
            previousRecord.count += count;
            em.persist(previousRecord);
        }
    }

    private static float getMeterDistance(final Point2D.Double location, final Point2D.Double anchorLocation) {
        double earthRadius = 3958.75;
        double dLat = Math.toRadians(anchorLocation.x-location.x);
        double dLng = Math.toRadians(anchorLocation.y-location.y);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(location.x)) * Math.cos(Math.toRadians(anchorLocation.x)) *
                   Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = earthRadius * c;
        int meterConversion = 1609;
        float meters = new Float(dist * meterConversion).floatValue();
        return meters;
    }

    private boolean isWithinRange(final Point2D.Double location, final Point2D.Double anchorLocation) {
        float meters = getMeterDistance(location, anchorLocation);
        return meters< CITY_RANGE;
    }

    @Transactional(readOnly=false)
    private void persistCity(final LocationFacet locationResource, final String date, long start, int count, final City city) {
        VisitedCity visitedCity = new VisitedCity(locationResource.apiKeyId);
        visitedCity.guestId = locationResource.guestId;
        visitedCity.locationSource = locationResource.source;
        visitedCity.api = locationResource.api;
        visitedCity.date = date;
        visitedCity.city = city;
        visitedCity.count = count;
        visitedCity.start = start;
        visitedCity.end = locationResource.end;
        visitedCity.startTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.withZone(DateTimeZone.forID(city.geo_timezone)).print(start);
        visitedCity.endTimeStorage = AbstractLocalTimeFacet.timeStorageFormat.withZone(DateTimeZone.forID(city.geo_timezone)).print(locationResource.end);
        computeSunriseSunset(locationResource, city, visitedCity);
        em.persist(visitedCity);
    }

    private void computeSunriseSunset(final LocationFacet locationFacet, final City city, final VisitedCity mdFacet) {
        Location location = new Location(String.valueOf(locationFacet.latitude), String.valueOf(locationFacet.longitude));
        final TimeZone timeZone = TimeZone.getTimeZone(city.geo_timezone);
        SunriseSunsetCalculator calc = new SunriseSunsetCalculator(location, timeZone);
        Calendar c = Calendar.getInstance(timeZone);
        c.setTimeInMillis(locationFacet.start);
        Calendar sunrise = calc.getOfficialSunriseCalendarForDate(c);
        Calendar sunset = calc.getOfficialSunsetCalendarForDate(c);
        if (sunrise==null||sunset==null)
            return;
        if (sunrise.getTimeInMillis() > sunset.getTimeInMillis()) {
            Calendar sr = sunrise;
            Calendar ss = sunset;
            sunset = sr;
            sunrise = ss;
        }
        mdFacet.sunrise = AbstractFacetVO.toMinuteOfDay(sunrise.getTime(), timeZone);
        mdFacet.sunset = AbstractFacetVO.toMinuteOfDay(sunset.getTime(),
                                                         timeZone);
    }

    @Transactional(readOnly = false)
    private void fetchWeatherInfo(double latitude, double longitude,
                                  String city, String date) throws IOException {
        List<WeatherInfo> weatherInfo = null;
        try {
            weatherInfo = wwoHelper.getWeatherInfo(latitude,
                                                   longitude, date);
        }
        catch (UnexpectedHttpResponseCodeException e) {
            logger.warn(String.format("Weather Info service down? http code is %s, message: '%s'",
                                      e.getHttpResponseCode(),
                                      e.getHttpResponseMessage()));
        }
        for (WeatherInfo info : weatherInfo) {
            info.city = city;
            info.fdate = date;
            em.persist(info);
        }
    }

    private void addIcons(List<WeatherInfo> weather){
        for (WeatherInfo weatherInfo : weather){
            switch (weatherInfo.weatherCode){
                case 395://Moderate or heavy snow in area with thunder
                    weatherInfo.weatherIconUrl = "images/climacons/CS.png";
                    weatherInfo.weatherIconUrlDay = "images/climacons/CSS.png";
                    weatherInfo.weatherIconUrlNight = "images/climacons/CSM.png";
                    break;
                case 389://Moderate or heavy rain in area with thunder
                    weatherInfo.weatherIconUrl = "images/climacons/CL.png";
                    weatherInfo.weatherIconUrlDay = "images/climacons/CL.png";
                    weatherInfo.weatherIconUrlNight = "images/climacons/CL.png";
                    break;
                case 200://Thundery outbreaks in nearby
                case 386://Patchy light rain in area with thunder
                case 392://Patchy light snow in area with thunder
                    weatherInfo.weatherIconUrl = "images/climacons/CL.png";
                    weatherInfo.weatherIconUrlDay = "images/climacons/CLS.png";
                    weatherInfo.weatherIconUrlNight = "images/climacons/CLM.png";
                    break;
                case 113://Clear/Sunny
                    weatherInfo.weatherIconUrl = "images/climacons/Sun.png";
                    weatherInfo.weatherIconUrlDay = "images/climacons/Sun.png";
                    weatherInfo.weatherIconUrlNight = "images/climacons/Moon.png";
                    break;
                case 116://Partly Cloudy
                    weatherInfo.weatherIconUrl = "images/climacons/Cloud.png";
                    weatherInfo.weatherIconUrlDay = "images/climacons/CS1.png";//CS#1.png
                    weatherInfo.weatherIconUrlNight = "images/climacons/CM.png";
                    break;
                case 122://Overcast
                case 119://Cloudy
                    weatherInfo.weatherIconUrl = "images/climacons/Cloud.png";
                    weatherInfo.weatherIconUrlDay = "images/climacons/Cloud.png";
                    weatherInfo.weatherIconUrlNight = "images/climacons/Cloud.png";
                    break;
                case 299://Moderate rain at times
                case 302://Moderate rain
                case 305://Heavy rain at times
                case 308://Heavy rain
                case 296: //Light rain
                case 293: //Patchy light rain
                case 266://Light drizzle
                case 353://Light rain shower
                case 356://Moderate or heavy rain shower
                case 359://Torrentail rain shower
                    weatherInfo.weatherIconUrl = "images/climacons/CD.png";
                    weatherInfo.weatherIconUrlDay = "images/climacons/CDS.png";
                    weatherInfo.weatherIconUrlNight = "images/climacons/CDM.png";
                    break;
                case 263://patchy light drizzle
                case 176://patchy rain nearby
                case 143://Mist
                    weatherInfo.weatherIconUrl = "images/climacons/CD_Alt.png";
                    weatherInfo.weatherIconUrlDay = "images/climacons/CDS_Alt.png";
                    weatherInfo.weatherIconUrlNight = "images/climacons/CDM_Alt.png";
                    break;
                case 227://Blowing snow
                case 230://Blizzard
                case 329://Patchy moderate snow
                case 332://Moderate snow
                case 335://Patchy heavy snow
                case 338://Heavy snow
                case 368://Light snow showers
                case 371://Moderate or heavy snow showers
                    weatherInfo.weatherIconUrl = "images/climacons/CS.png";
                    weatherInfo.weatherIconUrlDay = "images/climacons/CSS.png";
                    weatherInfo.weatherIconUrlNight = "images/climacons/CSM.png";
                    break;
                case 179://Patchy snow nearby
                case 323://Patchy Light snow
                case 325://Light snow
                    weatherInfo.weatherIconUrl = "images/climacons/CSA.png";
                    weatherInfo.weatherIconUrlDay = "images/climacons/CSSA.png";
                    weatherInfo.weatherIconUrlNight = "images/climacons/CSMA.png";
                    break;
                case 281://Freezing drizzle
                case 185: //Patchy freezing drizzle nearby
                case 182://Patchy sleet nearby
                case 311://Light freezing rain
                case 317://Light sleet
                    weatherInfo.weatherIconUrl = "images/climacons/CH.png";
                    weatherInfo.weatherIconUrlDay = "images/climacons/CHS.png";
                    weatherInfo.weatherIconUrlNight = "images/climacons/CHM.png";
                    break;
                case 260://Freezing Fog
                case 248://Fog
                    weatherInfo.weatherIconUrl = "images/climacons/Fog.png";
                    weatherInfo.weatherIconUrlDay = "images/climacons/FS.png";
                    weatherInfo.weatherIconUrlNight = "images/climacons/FM.png";
                    break;
                case 314://Moderate or Heavy freezing rain
                case 320://Moderate or heavy sleet
                case 284://Heavy freezing drizzle
                case 350://Ice pellets
                case 362://Light sleet showers
                case 365://Moderate or heavy sleet
                case 374://Light showrs of ice pellets
                case 377://Moderate or heavy showres of ice pellets
                    weatherInfo.weatherIconUrl = "images/climacons/CH_Alt.png";
                    weatherInfo.weatherIconUrlDay = "images/climacons/CHS_Alt.png";
                    weatherInfo.weatherIconUrlNight = "images/climacons/CHM_Alt.png";
                    break;
                default:
                    weatherInfo.weatherIconUrl = "images/climacons/WC.png";
                    weatherInfo.weatherIconUrlDay = "images/climacons/WC.png";
                    weatherInfo.weatherIconUrlNight = "images/climacons/WC.png";
                    break;
            }
        }
    }

    public static void main(final String[] args) {
        long now = System.currentTimeMillis();
        //final DateTime dateTime = formatter.withZone(DateTimeZone.forID("Europe/Brussels")).parseDateTime("2013-06-03");
        //System.out.println(dateTime.getMillis());
        //System.out.println(dateTime.getMillis()+DateTimeConstants.MILLIS_PER_DAY);
        //Point2D.Double p1 = new Point2D.Double(0,0);
        //Point2D.Double p2 = new Point2D.Double(0,0.0089);
        //System.out.println(getMeterDistance(p1, p2));
        //p2 = new Point2D.Double(0.00904,0.00);
        //System.out.println(getMeterDistance(p1, p2));
        //p2 = new Point2D.Double(0.00639, 0.00629);
        //System.out.println(getMeterDistance(p1, p2));
        //p1 = new Point2D.Double(40.0, 0.0);
        //p2 = new Point2D.Double(40, 0.0117647);
        //System.out.println(getMeterDistance(p1, p2));
        //p2 = new Point2D.Double(40.00904, 0.0);
        //System.out.println(getMeterDistance(p1, p2));
        //p2 = new Point2D.Double(40.00639, 0.0083);
        //System.out.println(getMeterDistance(p1, p2));
    }

}
