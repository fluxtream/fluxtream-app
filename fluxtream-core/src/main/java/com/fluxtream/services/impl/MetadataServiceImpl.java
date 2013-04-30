package com.fluxtream.services.impl;

import java.io.IOException;
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
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.metadata.City;
import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.domain.metadata.VisitedCity;
import com.fluxtream.domain.metadata.WeatherInfo;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.services.NotificationsService;
import com.fluxtream.utils.JPAUtils;
import org.apache.http.HttpException;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
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

    @Autowired
    ServicesHelper servicesHelper;

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
		DayMetadataFacet context = getDayMetadata(guestId, date, true);
		servicesHelper.setTimeZone(context, timeZone);
		em.merge(context);
	}

	@Override
	@Transactional(readOnly = false)
	public DayMetadataFacet getDayMetadata(long guestId, String date,
			boolean create) {

        //TODO: better metadata

        DayMetadataFacet info = new DayMetadataFacet();
        return info;
	}

    @Override
    public List<DayMetadataFacet> getAllDayMetadata(final long guestId) {

        //TODO: better metadata

        return JPAUtils.find(em, DayMetadataFacet.class, "context.all", guestId);
    }

	@Override
	public City getMainCity(long guestId, DayMetadataFacet context) {

        //TODO: better metadata

        return null;
	}

	@Override
	public LocationFacet getLastLocation(long guestId, long time) {
		LocationFacet lastSeen = JPAUtils.findUnique(em, LocationFacet.class,
                                                     "location.lastSeen", guestId, time);
		return lastSeen;
	}

	@Override
	public TimeZone getTimeZone(long guestId, String date) {

        //TODO: better metadata

		DayMetadataFacet thatDay = JPAUtils.findUnique(em,
				DayMetadataFacet.class, "context.byDate", guestId, date);
		if (thatDay != null)
			return TimeZone.getTimeZone(thatDay.timeZone);
		else {
			logger.info("guestId=" + guestId + " date= " + date + " action=getTimeZone message=returning UTC Timezone");
			return TimeZone.getTimeZone("UTC"); // Code should never go here!
		}
	}

	@Override
	public TimeZone getTimeZone(long guestId, long time) {

        // TODO: better metadata

		DayMetadataFacet thatDay = JPAUtils.findUnique(em,
				DayMetadataFacet.class, "context.day.when", guestId, time, time);
		if (thatDay != null)
			return TimeZone.getTimeZone(thatDay.timeZone);
		else {
            logger.info("guestId=" + guestId + " time= " + time + " action=getTimeZone message=returning UTC Timezone");
			return TimeZone.getTimeZone("UTC"); // Code should never go here!
		}
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
        int i=0;
        while(true) {
            final TypedQuery<LocationFacet> query = em.createQuery(String.format("SELECT facet FROM %s facet WHERE facet.guestId=%s ORDER BY facet.start ASC",
                                                                                 entityName, guest.getId()), LocationFacet.class);
            query.setFirstResult(i);
            query.setMaxResults(1000);
            final List<LocationFacet> locations = query.getResultList();
            if (locations.size()==0)
                break;
            updateLocationMetadata(guest.getId(), locations);
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
        Collections.sort(locationResources, new Comparator<LocationFacet>() {
            @Override
            public int compare(final LocationFacet o1, final LocationFacet o2) {
                return o1.start<o2.start?1:-1;
            }
        });
        // local vars: current city and current day
        String currentDate = "";
        long currentCityId  = -1l;
        List<String> affectedDates = new ArrayList<String>();
        for (LocationFacet locationResource : locationResources) {
            City city = getClosestCity(locationResource.latitude, locationResource.longitude);
            String date = formatter.withZone(DateTimeZone.forID(city.geo_timezone)).print(locationResource.timestampMs);
            final boolean dateChanged = !date.equals(currentDate);
            final boolean cityChanged = city.geo_id!=currentCityId;
            if (dateChanged||cityChanged) {
                final boolean wasStored = storeCity(locationResource, date, city);
                if (wasStored)
                    affectedDates.add(date);
            }
            currentDate = date;
            currentCityId = city.geo_id;
        }
        em.flush();
        updateVisitedWeatherInfo(guestId, affectedDates);
        updateSunriseSunsetInfo(guestId, affectedDates);
        updateTimezoneInfo(guestId, affectedDates);
    }

    private void updateTimezoneInfo(final long guestId, final List<String> affectedDates) {

    }

    private void updateSunriseSunsetInfo(final long guestId, final List<String> affectedDates) {
        //To change body of created methods use File | Settings | File Templates.
    }

    private void updateVisitedWeatherInfo(final long guestId, final List<String> affectedDates) {
        //To change body of created methods use File | Settings | File Templates.
    }

    /**
     * Store a new "checkin" in a city only if there isn't already a checkin in the same city before this one and
     * on the same day. This method will also remove checkins in the same city that would happen after this one (again,
     * on the same day).
     * @param locationResource
     * @param date
     * @param city
     * @return <code>true</code> if the check-in was inserted, <code>false</code> otherwise
     */
    private boolean storeCity(final LocationFacet locationResource, String date, final City city) {
        final String entityName = JPAUtils.getEntityName(VisitedCity.class);
        TypedQuery<VisitedCity> query = em.createQuery(
                String.format("SELECT facet from %s facet WHERE facet.guestId=%s AND facet.date='%s' AND facet.start<%s",
                              entityName, locationResource.guestId, date, locationResource.start),
                VisitedCity.class);
        List<VisitedCity> visitedCities = query.getResultList();
        if (visitedCities.size()==0) {
            persistCity(locationResource, date, city);
            // remove checkins in the same city that would happen after this one
            removeRedundantCityInfo(locationResource, date, city);
            return true;
        }
        return false;
    }

    private void removeRedundantCityInfo(final LocationFacet locationResource, final String date, final City city) {
        final String entityName = JPAUtils.getEntityName(VisitedCity.class);
        TypedQuery<VisitedCity> query = em.createQuery(
                String.format("SELECT facet from %s facet WHERE facet.guestId=%s AND facet.date='%s' AND facet.start>%s ORDER BY facet.start",
                              entityName, locationResource.guestId, date, locationResource.start),
                VisitedCity.class);
        List<VisitedCity> visitedCities = query.getResultList();
        for (VisitedCity visitedCity : visitedCities) {
            if (visitedCity.city.geo_id==city.geo_id) {
                em.remove(visitedCity);
            }
        }
    }

    @Transactional(readOnly=false)
    private void persistCity(final LocationFacet locationResource, final String date, final City city) {
        VisitedCity visitedCity = new VisitedCity();
        visitedCity.guestId = locationResource.guestId;
        visitedCity.locationSource = locationResource.source;
        visitedCity.date = date;
        visitedCity.city = city;
        visitedCity.start = locationResource.start;
        visitedCity.end = locationResource.end;
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

}
