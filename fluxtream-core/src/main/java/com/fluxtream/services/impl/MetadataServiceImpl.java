package com.fluxtream.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NavigableSet;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.google_latitude.LocationFacet;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.domain.metadata.City;
import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.domain.metadata.DayMetadataFacet.TravelType;
import com.fluxtream.domain.metadata.DayMetadataFacet.VisitedCity;
import com.fluxtream.domain.metadata.WeatherInfo;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.services.NotificationsService;
import com.fluxtream.thirdparty.helpers.WWOHelper;
import com.fluxtream.utils.JPAUtils;
import com.fluxtream.utils.TimeUtils;

@Service
@Transactional(readOnly = true)
public class MetadataServiceImpl implements MetadataService {

	Logger logger = Logger.getLogger(MetadataServiceImpl.class);

	@Autowired
	Configuration env;

	@PersistenceContext
	EntityManager em;

	@Autowired
	GuestService guestService;

	@Autowired
	WWOHelper wwoHelper;

	@Autowired
	NotificationsService notificationsService;

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
		LocationFacet lastLocation = getLastLocation(guestId,
				System.currentTimeMillis());
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
		setTimeZone(context, timeZone);
		em.merge(context);
	}

	private boolean setTimeZone(DayMetadataFacet info, String timeZone) {
		boolean sameTz = false;
		if (timeZone != null) {
			if (info.timeZone == null) {
				info.timeZone = timeZone;
				return true;
			} else if (!info.timeZone.equals(timeZone)) {
				info.otherTimeZone = timeZone;
				TimeZone otherTz = TimeZone.getTimeZone(info.otherTimeZone);
				TimeZone tz = TimeZone.getTimeZone(timeZone);
				int otherOffset = otherTz.getRawOffset();
				int offset = tz.getRawOffset();
				int otherDSTSavings = otherTz.getDSTSavings();
				int dSTSavings = tz.getDSTSavings();
				sameTz = otherOffset == offset && otherDSTSavings == dSTSavings;
				if (sameTz)
					info.otherTimeZone = null;
			}
		}
		// TODO: we are using the "main" timezone but... shouldn't we be
		// more cautious?
		TimeZone tz = TimeZone.getTimeZone(info.timeZone);
		DateTime time = formatter.withZone(DateTimeZone.forTimeZone(tz))
				.parseDateTime(info.date);
		info.start = TimeUtils.fromMidnight(time.getMillis(), tz);
		info.end = TimeUtils.toMidnight(time.getMillis(), tz);
		return sameTz;
	}

	private String addCity(DayMetadataFacet info, City city) {
		String country = env.getCountry(city.geo_country_code);
		if (country == null)
			return city.geo_name;
		country = WordUtils.capitalize(country.toLowerCase());
		String state = city.geo_admin1_code;
		if (state == null || state.equals(""))
			state = "-";
		String cityLabel = city.geo_name;
		if ("US".equals(city.geo_country_code))
			cityLabel = city.geo_name + "/" + state + "/" + country;
		else
			cityLabel = city.geo_name + "/" + country;

		if (info.cities == null || info.cities.length() == 0) {
			info.cities = cityLabel + "/" + city.population + "/1";
		} else if (info.cities.indexOf(cityLabel) == -1) {
			info.cities += "|" + cityLabel + "/" + city.population + "/1";
		} else {
			info.cities = incrementCityCount(info.cities, cityLabel);
		}
		return city.geo_name;
	}

	private String incrementCityCount(String cities, String cityLabel) {
		StringTokenizer st = new StringTokenizer(cities, "|");
		List<String> citiesList = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String city = st.nextToken();
			if (city.startsWith(cityLabel)) {
				String populationAndCount = city
						.substring(cityLabel.length() + 1);
				String population = populationAndCount.split("/")[0];
				String countString = populationAndCount.split("/")[1];
				int count = Integer.valueOf(countString);
				citiesList
						.add(cityLabel + "/" + population + "/" + (count + 1));
			} else {
				citiesList.add(city);
			}
		}
		StringBuffer sb = new StringBuffer();
		for (String city : citiesList) {
			if (sb.length() > 0)
				sb.append("|");
			sb.append(city);
		}
		return sb.toString();
	}

	@Override
	public void setTraveling(long guestId, String date, TravelType travelType) {
		DayMetadataFacet info = getDayMetadata(guestId, date, false);
		if (info == null)
			return;
		info.travelType = travelType;
		em.merge(info);
	}

	@Override
	public void addTimeSpentAtHome(long guestId, long startTime, long endTime) {
		// TODO Auto-generated method stub

	}

	@Override
	@Transactional(readOnly = false)
	public DayMetadataFacet getDayMetadata(long guestId, String date,
			boolean create) {
		DayMetadataFacet info = JPAUtils.findUnique(em, DayMetadataFacet.class,
				"context.byDate", guestId, date);
		if (info != null)
			return info;
		else if (create)
			info = copyNextDailyContextualInfo(guestId, date);
		if (info != null)
			return info;
		else {
			info = new DayMetadataFacet();
			info.date = date;
			info.guestId = guestId;
			return info;
		}
	}

	@Transactional(readOnly = false)
	public DayMetadataFacet copyNextDailyContextualInfo(long guestId,
			String date) {
		DayMetadataFacet next = getNextExistingDayMetadata(guestId, date);
		DayMetadataFacet info = new DayMetadataFacet();
		info.guestId = guestId;
		info.date = date;

		if (next == null) {
			return null;
		} else {
			City mainCity = getMainCity(guestId, next);
			if (mainCity != null) {
				addCity(info, mainCity);
				setTimeZone(info, next.timeZone);
				TimeZone tz = TimeZone.getTimeZone(next.timeZone);
				List<WeatherInfo> weatherInfo = getWeatherInfo(
						mainCity.geo_latitude, mainCity.geo_longitude, date,
						toMinuteOfDay(info.start, tz),
						toMinuteOfDay(info.end, tz));
				setWeatherInfo(info, weatherInfo);
			}
			setTimeZone(info, next.timeZone);
		}
		em.persist(info);
		return info;
	}

	private int toMinuteOfDay(long time, TimeZone timeZone) {
		return AbstractFacetVO.toMinuteOfDay(new Date(time), timeZone);
	}

	private DayMetadataFacet getNextExistingDayMetadata(long guestId,
			String todaysDate) {
		// TODO: not totally acurate, since we are ignoring the timezone
		// of todaysDate, but should work in most cases
		long start = formatter.parseMillis(todaysDate);
		DayMetadataFacet next = JPAUtils.findUnique(em, DayMetadataFacet.class,
				"context.day.next", guestId, start);
		if (next == null)
			next = JPAUtils.findUnique(em, DayMetadataFacet.class,
					"context.day.oldest", guestId);
		return next;
	}

	@Override
	public City getMainCity(long guestId, DayMetadataFacet context) {
		NavigableSet<VisitedCity> cities = context.getOrderedCities();
		if (cities.size() == 0) {
			logger.warn("guestId=" + guestId + " message=no_main_city date=" + context.date);
			return null;
		}
		VisitedCity mostVisited = cities.last();
		City city = JPAUtils.findUnique(em, City.class,
				"city.byNameAndCountryCode", mostVisited.name,
				env.getCountryCode(mostVisited.country));
		return city;
	}

	private void updateFloatingTimeZoneFacets(long guestId, long time) {
		// TODO Auto-generated method stub

	}

	private void setWeatherInfo(DayMetadataFacet info,
			List<WeatherInfo> weatherInfo) {
		if (weatherInfo.size() == 0)
			return;

		for (WeatherInfo weather : weatherInfo) {
			if (weather.tempC < info.minTempC)
				info.minTempC = weather.tempC;
			if (weather.tempF < info.minTempF)
				info.minTempF = weather.tempF;
			if (weather.tempC > info.maxTempC)
				info.maxTempC = weather.tempC;
			if (weather.tempF > info.maxTempF)
				info.maxTempF = weather.tempF;
		}

	}

	@Override
	@Transactional(readOnly = false)
	public void addGuestLocation(long guestId, long time, float latitude,
			float longitude) {
		City city = getClosestCity(latitude, longitude);
		String date = formatter.withZone(DateTimeZone.forID(city.geo_timezone))
				.print(time);

		DayMetadataFacet info = getDayMetadata(guestId, date, true);
		addCity(info, city);
		boolean timeZoneWasSet = setTimeZone(info, city.geo_timezone);
		if (timeZoneWasSet)
			updateFloatingTimeZoneFacets(guestId, time);

		TimeZone tz = TimeZone.getTimeZone(info.timeZone);
		List<WeatherInfo> weatherInfo = getWeatherInfo(city.geo_latitude,
				city.geo_longitude, info.date, toMinuteOfDay(info.start, tz),
				toMinuteOfDay(info.end, tz));
		setWeatherInfo(info, weatherInfo);

		em.merge(info);
	}

	@Override
	public LocationFacet getLastLocation(long guestId, long time) {
		LocationFacet lastSeen = JPAUtils.findUnique(em, LocationFacet.class,
				"google_latitude.lastSeen", guestId, time);
		return lastSeen;
	}

	@Override
	public LocationFacet getNextLocation(long guestId, long time) {
		LocationFacet lastSeen = JPAUtils.findUnique(em, LocationFacet.class,
				"google_latitude.nextSeen", guestId, time);
		return lastSeen;
	}

	@Override
	public List<WeatherInfo> getWeatherInfo(double latitude, double longitude,
			String date, int startMinute, int endMinute) {
		City closestCity = getClosestCity(latitude, longitude);
		List<WeatherInfo> weather = JPAUtils.find(em, WeatherInfo.class,
				"weather.byDateAndCity.between", closestCity.geo_name, date,
				startMinute, endMinute);

		if (weather != null && weather.size() > 0)
			return weather;
		else {
			try {
				fetchWeatherInfo(latitude, longitude, closestCity.geo_name,
						date);
			} catch (Exception e) {
				logger.warn("action=fetchWeather error");
			}
			weather = JPAUtils.find(em, WeatherInfo.class,
					"weather.byDateAndCity.between", closestCity.geo_name,
					date, startMinute, endMinute);
            for (WeatherInfo weatherInfo : weather){
                switch (weatherInfo.weatherCode){
                    case 395://Moderate or heavy snow in area with thunder
                        weatherInfo.weatherIconUrl = "/static/images/climacons/CS.png";
                        weatherInfo.weatherIconUrlDay = "/static/images/climacons/CSS.png";
                        weatherInfo.weatherIconUrlNight = "/static/images/climacons/CSM.png";
                        break;
                    case 389://Moderate or heavy rain in area with thunder
                        weatherInfo.weatherIconUrl = "/static/images/climacons/CL.png";
                        weatherInfo.weatherIconUrlDay = "/static/images/climacons/CL.png";
                        weatherInfo.weatherIconUrlNight = "/static/images/climacons/CL.png";
                        break;
                    case 200://Thundery outbreaks in nearby
                    case 386://Patchy light rain in area with thunder
                    case 392://Patchy light snow in area with thunder
                        weatherInfo.weatherIconUrl = "/static/images/climacons/CL.png";
                        weatherInfo.weatherIconUrlDay = "/static/images/climacons/CLS.png";
                        weatherInfo.weatherIconUrlNight = "/static/images/climacons/CLM.png";
                        break;
                    case 113://Clear/Sunny
                        weatherInfo.weatherIconUrl = "/static/images/climacons/Sun.png";
                        weatherInfo.weatherIconUrlDay = "/static/images/climacons/Sun.png";
                        weatherInfo.weatherIconUrlNight = "/static/images/climacons/Moon.png";
                        break;
                    case 116://Partly Cloudy
                        weatherInfo.weatherIconUrl = "/static/images/climacons/Cloud.png";
                        weatherInfo.weatherIconUrlDay = "/static/images/climacons/CS%231.png";//CS#1.png
                        weatherInfo.weatherIconUrlNight = "/static/images/climacons/CM.png";
                        break;
                    case 122://Overcast
                    case 119://Cloudy
                        weatherInfo.weatherIconUrl = "/static/images/climacons/Cloud.png";
                        weatherInfo.weatherIconUrlDay = "/static/images/climacons/Cloud.png";
                        weatherInfo.weatherIconUrlNight = "/static/images/climacons/Cloud.png";
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
                        weatherInfo.weatherIconUrl = "/static/images/climacons/CD.png";
                        weatherInfo.weatherIconUrlDay = "/static/images/climacons/CDS.png";
                        weatherInfo.weatherIconUrlNight = "/static/images/climacons/CDM.png";
                        break;
                    case 263://patchy light drizzle
                    case 176://patchy rain nearby
                    case 143://Mist
                        weatherInfo.weatherIconUrl = "/static/images/climacons/CD%20Alt.png";
                        weatherInfo.weatherIconUrlDay = "/static/images/climacons/CDS%20Alt.png";
                        weatherInfo.weatherIconUrlNight = "/static/images/climacons/CDM%20Alt.png";
                        break;
                    case 227://Blowing snow
                    case 230://Blizzard
                    case 329://Patchy moderate snow
                    case 332://Moderate snow
                    case 335://Patchy heavy snow
                    case 338://Heavy snow
                    case 368://Light snow showers
                    case 371://Moderate or heavy snow showers
                        weatherInfo.weatherIconUrl = "/static/images/climacons/CS.png";
                        weatherInfo.weatherIconUrlDay = "/static/images/climacons/CSS.png";
                        weatherInfo.weatherIconUrlNight = "/static/images/climacons/CSM.png";
                        break;
                    case 179://Patchy snow nearby
                    case 323://Patchy Light snow
                    case 325://Light snow
                        weatherInfo.weatherIconUrl = "/static/images/climacons/CSA.png";
                        weatherInfo.weatherIconUrlDay = "/static/images/climacons/CSSA.png";
                        weatherInfo.weatherIconUrlNight = "/static/images/climacons/CSMA.png";
                        break;
                    case 281://Freezing drizzle
                    case 185: //Patchy freezing drizzle nearby
                    case 182://Patchy sleet nearby
                    case 311://Light freezing rain
                    case 317://Light sleet
                        weatherInfo.weatherIconUrl = "/static/images/climacons/CH.png";
                        weatherInfo.weatherIconUrlDay = "/static/images/climacons/CHS.png";
                        weatherInfo.weatherIconUrlNight = "/static/images/climacons/CHM.png";
                        break;
                    case 260://Freezing Fog
                    case 248://Fog
                        weatherInfo.weatherIconUrl = "/static/images/climacons/Fog.png";
                        weatherInfo.weatherIconUrlDay = "/static/images/climacons/FS.png";
                        weatherInfo.weatherIconUrlNight = "/static/images/climacons/FM.png";
                        break;
                    case 314://Moderate or Heavy freezing rain
                    case 320://Moderate or heavy sleet
                    case 284://Heavy freezing drizzle
                    case 350://Ice pellets
                    case 362://Light sleet showers
                    case 365://Moderate or heavy sleet
                    case 374://Light showrs of ice pellets
                    case 377://Moderate or heavy showres of ice pellets
                        weatherInfo.weatherIconUrl = "/static/images/climacons/CH%20Alt.png";
                        weatherInfo.weatherIconUrlDay = "/static/images/climacons/CHS%20Alt.png";
                        weatherInfo.weatherIconUrlNight = "/static/images/climacons/CHM%20Alt.png";
                        break;
                    default:
                        weatherInfo.weatherIconUrl = "/static/images/climacons/WC.png";
                        weatherInfo.weatherIconUrlDay = "/static/images/climacons/WC.png";
                        weatherInfo.weatherIconUrlNight = "/static/images/climacons/WC.png";
                        break;
                }
            }
			return weather;
		}
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

	@Transactional(readOnly = false)
	private void fetchWeatherInfo(double latitude, double longitude,
			String city, String date) throws HttpException, IOException {
		List<WeatherInfo> weatherInfo = wwoHelper.getWeatherInfo(latitude,
				longitude, date);
		for (WeatherInfo info : weatherInfo) {
			info.city = city;
			info.fdate = date;
			em.persist(info);
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
	public DayMetadataFacet getLastDayMetadata(long guestId) {
		DayMetadataFacet lastDay = JPAUtils.findUnique(em,
				DayMetadataFacet.class, "context.day.last", guestId);
		return lastDay;
	}

	@Override
	public TimeZone getTimeZone(long guestId, String date) {
		DayMetadataFacet thatDay = JPAUtils.findUnique(em,
				DayMetadataFacet.class, "context.byDate", guestId, date);
		if (thatDay != null)
			return TimeZone.getTimeZone(thatDay.timeZone);
		else {
			logger.warn("guestId=" + guestId + " action=getTimeZone warning=returning UTC Timezone");
			return TimeZone.getTimeZone("UTC"); // Code should never go here!
		}
	}

	@Override
	public TimeZone getTimeZone(long guestId, long time) {
		DayMetadataFacet thatDay = JPAUtils.findUnique(em,
				DayMetadataFacet.class, "context.day.when", guestId, time);
		if (thatDay != null)
			return TimeZone.getTimeZone(thatDay.timeZone);
		else {
			logger.warn("guestId=" + guestId + " action=getTimeZone warning=returning UTC Timezone");
			return TimeZone.getTimeZone("UTC"); // Code should never go here!
		}
	}

	@Override
	@Transactional(readOnly=false)
	public void setDayCommentTitle(long guestId, String date, String title) {
		DayMetadataFacet thatDay = getDayMetadata(guestId,
				date, true);
		thatDay.title = title;
		em.merge(thatDay);
	}

	@Override
	@Transactional(readOnly=false)
	public void setDayCommentBody(long guestId, String date, String body) {
		DayMetadataFacet thatDay = JPAUtils.findUnique(em,
				DayMetadataFacet.class, "context.byDate", guestId, date);
		thatDay.comment = body;
		em.merge(thatDay);
	}

}
