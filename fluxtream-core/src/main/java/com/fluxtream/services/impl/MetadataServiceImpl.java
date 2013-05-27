package com.fluxtream.services.impl;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import com.fluxtream.Configuration;
import com.fluxtream.DayMetadata;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.domain.AbstractLocalTimeFacet;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.metadata.City;
import com.fluxtream.domain.metadata.VisitedCity;
import com.fluxtream.domain.metadata.WeatherInfo;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.services.NotificationsService;
import com.fluxtream.utils.JPAUtils;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import org.apache.http.HttpException;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//import com.fluxtream.thirdparty.helpers.WWOHelper;

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

    //@Autowired
    //WWOHelper wwoHelper;

    private static final DateTimeFormatter formatter = DateTimeFormat
            .forPattern("yyyy-MM-dd");

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
	public DayMetadata getDayMetadata(long guestId, String date) {
        // get visited cities for a specific date . If we don't have any data for that date,
        // retrieve cities for the first date for which we do have data
        List<VisitedCity> cities = getVisitedCitiesForDate(guestId, date);
        if (cities.size()==0) {
            VisitedCity existingCity = searchCityBefore(guestId, date);
            if (existingCity==null)
                existingCity = searchCityAfter(guestId, date);
            if (existingCity!=null) {
                cities = getVisitedCitiesForDate(guestId, existingCity.date);
            }
        }
        if (cities.size()>0) {
            final VisitedCity consensusVisitedCity = getConsensusVisitedCity(cities);
            DayMetadata info = new DayMetadata(cities, consensusVisitedCity, date);
            return info;
        } else {
            DayMetadata info = new DayMetadata(date);
            return info;
        }
	}

    private List<VisitedCity> getVisitedCitiesForDate(final long guestId, final String date) {
        TypedQuery<VisitedCity> query = em.createQuery("SELECT facet FROM " + JPAUtils.getEntityName(VisitedCity.class) + " facet WHERE facet.guestId=? AND facet.date=?", VisitedCity.class);
        query.setParameter(1, guestId);
        query.setParameter(2, date);
        final List<VisitedCity> cities = query.getResultList();
        return cities;
    }

    /**
     * Get only the consensus city: for now, just the city where the user has spent the most time in
     * @param cities
     * @return
     */
    private VisitedCity getConsensusVisitedCity(final List<VisitedCity> cities) {
        Collections.sort(cities, new Comparator<VisitedCity>() {
            @Override
            public int compare(final VisitedCity a, final VisitedCity b) {
                int timeSpentInA = (int) (a.end-a.start+1); //add one if start and end are equal
                int timeSpentInB = (int) (b.end-b.start+1);
                return timeSpentInA-timeSpentInB;
            }
        });

        return cities.get(0);
    }

    private String findClosestKnownDateForTime(final long guestId, final long time) {
        VisitedCity existingCity = searchCityBefore(guestId, time);
        if (existingCity==null)
            existingCity = searchCityAfter(guestId, time);
        if (existingCity!=null) {
            return existingCity.date;
        }
        return formatter.withZoneUTC().print(time);
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

    private VisitedCity getVisitedCity(final long guestId, final long instant, final TypedQuery<VisitedCity> query) {
        query.setMaxResults(1);
        query.setParameter(1, guestId);
        query.setParameter(2, instant);
        final List<VisitedCity> cities = query.getResultList();
        if (cities.size()>0)
            return cities.get(0);
        return null;
    }

    private VisitedCity searchCityBefore(final long guestId, final String date) {
        return searchCity("SELECT facet FROM " + JPAUtils.getEntityName(VisitedCity.class) + " facet WHERE facet.guestId=? AND facet.start<? ORDER BY facet.start", guestId, date);
    }

    private VisitedCity searchCityAfter(final long guestId, final String date) {
        return searchCity("SELECT facet FROM " + JPAUtils.getEntityName(VisitedCity.class) + " facet WHERE facet.guestId=? AND facet.start>? ORDER BY facet.start", guestId, date);
    }

    private VisitedCity searchCity(final String queryString, final long guestId, final String date) {
        long time = formatter.withZoneUTC().parseDateTime(date).getMillis();
        final TypedQuery<VisitedCity> query = em.createQuery(queryString, VisitedCity.class);
        return getVisitedCity(guestId, time, query);
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
        return TimeZone.getTimeZone(dayMetadata.timeZone);
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
                                            String date, int startMinute, int endMinute) {
        City closestCity = getClosestCity(latitude, longitude);
        List<WeatherInfo> weather = JPAUtils.find(em, WeatherInfo.class, "weather.byDateAndCity.between", closestCity.geo_name, date, startMinute, endMinute);

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
                                    date, startMinute, endMinute);
            addIcons(weather);
        }
        return weather;
    }

    @Override
    public void rebuildMetadata(final String username) {
        final Guest guest = guestService.getGuest(username);
        String entityName = JPAUtils.getEntityName(LocationFacet.class);
        final Query nativeQuery = em.createNativeQuery(String.format("SELECT DISTINCT apiKeyId FROM %s WHERE guestId=%s", entityName, guest.getId()));
        final List<BigInteger> resultList = nativeQuery.getResultList();
        for (BigInteger apiKeyId : resultList) {
            rebuildMetadata(username, apiKeyId.longValue());
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
            final List<LocationFacet> locations = facetsQuery.getResultList();
            System.out.println("retrieved " + locations.size() + " location datapoints (offset is " + i + ")");
            if (locations.size()==0)
                break;
            System.out.println(AbstractLocalTimeFacet.timeStorageFormat.withZoneUTC().print(locations.get(0).start));
            long then = System.currentTimeMillis();
            updateLocationMetadata(guest.getId(), locations);
            long now = System.currentTimeMillis();
            System.out.println(String.format("updateLocationMetadata took %s ms to complete", (now-then)));
            i+=locations.size();
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
        // sort the location data in ascending time order
        System.out.println("processing " + locationResources.size() + " location datapoints...");
        Collections.sort(locationResources, new Comparator<LocationFacet>() {
            @Override
            public int compare(final LocationFacet o1, final LocationFacet o2) {
                return o1.start>o2.start?1:-1;
            }
        });
        // local vars: current city and current day
        String currentDate = "";
        Point2D.Double anchorLocation = new Point2D.Double(locationResources.get(0).latitude, locationResources.get(0).longitude);
        City anchorCity = getClosestCity(anchorLocation.x, anchorLocation.y);
        int count = 0;
        LocationFacet lastLocationResourceMatchingAnchor=locationResources.get(0);
        long start = locationResources.get(0).start;

        for (LocationFacet locationResource : locationResources) {
            City newCity = anchorCity;
            Point2D.Double location = new Point2D.Double(locationResource.latitude, locationResource.longitude);
            boolean withinAnchorRange = isWithinRange(location, anchorLocation);

            if (!withinAnchorRange) {
                anchorLocation = new Point2D.Double(locationResource.latitude, locationResource.longitude);
                newCity = getClosestCity(locationResource.latitude, locationResource.longitude);
            }

            String newDate = formatter.withZone(DateTimeZone.forID(newCity.geo_timezone)).print(locationResource.timestampMs);
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
        em.flush();
    }

    private void storeCityInfo(final LocationFacet locationResource, String date, final City city, final long start, final int count) {
        VisitedCity previousRecord = JPAUtils.findUnique(em, VisitedCity.class,
                                                         "visitedCities.byApiDateAndCity",
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

    private WeatherInfo getWeatherForLocation(final LocationFacet locationFacet, TimeZone tz) {
        String date = formatter.withZone(DateTimeZone.forTimeZone(tz)).print(locationFacet.start);
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(locationFacet.start);
        int startMinute = c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE);
        final List<WeatherInfo> weatherInfo = getWeatherInfo(locationFacet.latitude, locationFacet.longitude, date, startMinute, startMinute);
        if (weatherInfo.size()>0)
            return weatherInfo.get(0);
        else return null;
    }

    @Transactional(readOnly = false)
    private void fetchWeatherInfo(double latitude, double longitude,
                                  String city, String date) throws HttpException, IOException {
        //List<WeatherInfo> weatherInfo = wwoHelper.getWeatherInfo(latitude,
        //                                                         longitude, date);
        //for (WeatherInfo info : weatherInfo) {
        //    info.city = city;
        //    info.fdate = date;
        //    em.persist(info);
        //}
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
        Point2D.Double p1 = new Point2D.Double(0,0);
        Point2D.Double p2 = new Point2D.Double(0,0.0089);
        System.out.println(getMeterDistance(p1, p2));
        p2 = new Point2D.Double(0.00904,0.00);
        System.out.println(getMeterDistance(p1, p2));
        p2 = new Point2D.Double(0.00639, 0.00629);
        System.out.println(getMeterDistance(p1, p2));
        p1 = new Point2D.Double(40.0, 0.0);
        p2 = new Point2D.Double(40, 0.0117647);
        System.out.println(getMeterDistance(p1, p2));
        p2 = new Point2D.Double(40.00904, 0.0);
        System.out.println(getMeterDistance(p1, p2));
        p2 = new Point2D.Double(40.00639, 0.0083);
        System.out.println(getMeterDistance(p1, p2));
    }

}
