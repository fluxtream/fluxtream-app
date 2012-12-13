package com.fluxtream.api;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.NavigableSet;
import java.util.TimeZone;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.fluxtream.TimeInterval;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.auth.CoachRevokedException;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.connectors.vos.ImageVOCollection;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.CoachingBuddy;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.GuestAddress;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.domain.Notification;
import com.fluxtream.domain.metadata.City;
import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.domain.metadata.DayMetadataFacet.VisitedCity;
import com.fluxtream.mvc.models.AddressModel;
import com.fluxtream.mvc.models.ConnectorDigestModel;
import com.fluxtream.mvc.models.ConnectorResponseModel;
import com.fluxtream.mvc.models.DigestModel;
import com.fluxtream.mvc.models.GuestModel;
import com.fluxtream.mvc.models.NotificationModel;
import com.fluxtream.mvc.models.SettingsModel;
import com.fluxtream.mvc.models.SolarInfoModel;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.mvc.models.TimeBoundariesModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.CoachingService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.services.NotificationsService;
import com.fluxtream.services.SettingsService;
import com.fluxtream.updaters.strategies.UpdateStrategy;
import com.fluxtream.updaters.strategies.UpdateStrategyFactory;
import com.fluxtream.utils.Utils;
import com.google.gson.Gson;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import org.apache.log4j.Logger;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.newrelic.api.agent.NewRelic.setTransactionName;

@Path("/calendar")
@Component("RESTCalendarDataStore")
@Scope("request")
public class CalendarDataStore {

    Logger logger = Logger.getLogger(CalendarDataStore.class);

	@Autowired
	GuestService guestService;

	@Autowired
	MetadataService metadataService;

	@Autowired
	SettingsService settingsService;

	@Autowired
	ApiDataService apiDataService;

	@Autowired
	NotificationsService notificationsService;

	@Autowired
	UpdateStrategyFactory updateStrategyFactory;

	@Autowired
    CalendarDataHelper calendarDataHelper;

    @Autowired
    CoachingService coachingService;

	Gson gson = new Gson();

	@GET
	@Path("/all/continuous")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getAllConnectorsContinuous()
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
		return "{}";
	}

	@GET
	@Path("/all/week/{year}/{week}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getAllConnectorsWeekData(@PathParam("year") int year,
			@PathParam("week") int week, @QueryParam("filter") String filter)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
        setTransactionName(null, "GET /calendar/all/week/{year}/{week}");
        Guest guest = AuthHelper.getGuest();
        long guestId = guest.getId();
        CoachingBuddy coachee = null;
        try {
            coachee = AuthHelper.getCoachee();
        } catch (CoachRevokedException e) {
            return gson.toJson(new StatusModel(false, "Sorry, permission to access this data has been revoked."));
        }
        if (coachee!=null) {
            guestId = coachee.guestId;
            guest = guestService.getGuestById(guestId);
        }
        try{
            long then = System.currentTimeMillis();
            //TODO:proper week data retrieval implementation
            //this implementation is just a dirt hacky way to make it work and some aspects (weather info) don't work

            DigestModel digest = new DigestModel();

            digest.timeUnit = "WEEK";
            if (filter == null) {
                filter = "";
            }

            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR,year);
            c.set(Calendar.WEEK_OF_YEAR,week);
            c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            DecimalFormat datePartFormat = new DecimalFormat("00");
            DayMetadataFacet dayMetaStart = metadataService.getDayMetadata(guest.getId(), year + "-" + datePartFormat.format(c.get(Calendar.MONTH) + 1) +
                                                                                          "-" + datePartFormat.format(c.get(Calendar.DAY_OF_MONTH)), true);
            int newDay = c.get(Calendar.DAY_OF_YEAR) + 6;
            if (newDay > (isLeapYear(year) ? 366 : 365)){
                newDay -= isLeapYear(year) ? 366 : 365;
                year += 1;
                c.set(Calendar.YEAR,year);
            }
            c.set(Calendar.DAY_OF_YEAR,newDay);
            DayMetadataFacet dayMetaEnd = metadataService.getDayMetadata(guest.getId(), year + "-" + datePartFormat.format(c.get(Calendar.MONTH) + 1) +
                                                                                        "-" + datePartFormat.format(c.get(Calendar.DAY_OF_MONTH)), true);

            DayMetadataFacet dayMetadata = new DayMetadataFacet();
            dayMetadata.timeZone = dayMetaStart.timeZone;
            dayMetadata.start = dayMetaStart.start;
            dayMetadata.end = dayMetaEnd.end;

            digest.tbounds = getStartEndResponseBoundaries(dayMetadata.start,
                                                           dayMetadata.end);
            digest.timeZoneOffset = TimeZone.getTimeZone(dayMetadata.timeZone).getOffset((digest.tbounds.start + digest.tbounds.end)/2);

            City city = metadataService.getMainCity(guestId, dayMetadata);

            /*if (city != null){                          well
                digest.hourlyWeatherData = metadataService.getWeatherInfo(city.geo_latitude,city.geo_longitude, date, 0, 24 * 60);
                Collections.sort(digest.hourlyWeatherData);
            }*/

            setSolarInfo(digest, city, guestId, dayMetadata);

            List<ApiKey> apiKeySelection = getApiKeySelection(guestId, filter, coachee);
            digest.selectedConnectors = connectorInfos(guestId,apiKeySelection);
            List<ApiKey> allApiKeys = guestService.getApiKeys(guestId);
            allApiKeys = removeConnectorsWithoutFacets(allApiKeys, coachee);
            digest.nApis = allApiKeys.size();
            GuestSettings settings = settingsService.getSettings(AuthHelper.getGuestId());

            setCachedData(digest, allApiKeys, settings, apiKeySelection,
                          dayMetadata);

            copyMetadata(digest, dayMetadata);
            setVisitedCities(digest, guestId, dayMetadata);
            setNotifications(digest, AuthHelper.getGuestId());
            setCurrentAddress(digest, guestId, dayMetadata.start);
            digest.settings = new SettingsModel(settings,guest);

            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getAllConnectorsWeekData")
                    .append(" year=").append(year)
                    .append(" week=").append(week)
                    .append(" timeTaken=").append(System.currentTimeMillis()-then)
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());

            return gson.toJson(digest);
        }
        catch (Exception e){
            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getAllConnectorsWeekData")
                    .append(" year=").append(year)
                    .append(" week=").append(week)
                    .append(" guestId=").append(guestId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return gson.toJson(new StatusModel(false,"Failed to get digest: " + e.getMessage()));
        }
	}

    private boolean isLeapYear(int year){
        if (year % 400 == 0)
            return true;
        if (year % 100 == 0)
            return false;
        return year % 4 == 0;
    }

	@GET
	@Path("/all/month/{year}/{month}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getAllConnectorsMonthData(@PathParam("year") int year,
			@PathParam("month") int month,
			@QueryParam("filter") String filter) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
        setTransactionName(null, "GET /calendar/all/month/{year}/{month}");
        Guest guest = AuthHelper.getGuest();
        long guestId = guest.getId();
        CoachingBuddy coachee = null;
        try {
            coachee = AuthHelper.getCoachee();
        } catch (CoachRevokedException e) {
            return gson.toJson(new StatusModel(false, "Sorry, permission to access this data has been revoked."));
        }
        if (coachee!=null) {
            guestId = coachee.guestId;
            guest = guestService.getGuestById(guestId);
        }
        try{
            long then = System.currentTimeMillis();
            DigestModel digest = new DigestModel();

            digest.timeUnit = "MONTH";
            if (filter == null) {
                filter = "";
            }

            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR,year);
            c.set(Calendar.MONTH,month - 1);
            int endDayNum = c.getActualMaximum(Calendar.DAY_OF_MONTH);

            isLeapYear(year);

            DayMetadataFacet dayMetaStart = metadataService.getDayMetadata(guest.getId(), year + "-" + month + "-01", true);

            DayMetadataFacet dayMetaEnd = metadataService.getDayMetadata(guest.getId(), year + "-" + month + "-" + endDayNum, true);

            DayMetadataFacet dayMetadata = new DayMetadataFacet();
            dayMetadata.timeZone = dayMetaStart.timeZone;
            dayMetadata.start = dayMetaStart.start;
            dayMetadata.end = dayMetaEnd.end;

            digest.tbounds = getStartEndResponseBoundaries(dayMetadata.start,
                                                           dayMetadata.end);
            digest.timeZoneOffset = TimeZone.getTimeZone(dayMetadata.timeZone).getOffset((digest.tbounds.start + digest.tbounds.end)/2);

            City city = metadataService.getMainCity(guestId, dayMetadata);

            /*if (city != null){
                digest.hourlyWeatherData = metadataService.getWeatherInfo(city.geo_latitude,city.geo_longitude, date, 0, 24 * 60);
                Collections.sort(digest.hourlyWeatherData);
            }*/

            setSolarInfo(digest, city, guestId, dayMetadata);

            List<ApiKey> apiKeySelection = getApiKeySelection(guestId, filter, coachee);
            digest.selectedConnectors = connectorInfos(guestId,apiKeySelection);
            List<ApiKey> allApiKeys = guestService.getApiKeys(guestId);
            allApiKeys = removeConnectorsWithoutFacets(allApiKeys, coachee);
            digest.nApis = allApiKeys.size();
            GuestSettings settings = settingsService.getSettings(AuthHelper.getGuestId());

            setCachedData(digest, allApiKeys, settings, apiKeySelection,
                          dayMetadata);

            copyMetadata(digest, dayMetadata);
            setVisitedCities(digest, guestId, dayMetadata);
            setNotifications(digest, AuthHelper.getGuestId());
            setCurrentAddress(digest, guestId, dayMetadata.start);
            digest.settings = new SettingsModel(settings, guest);

            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getAllConnectorsMonthData")
                    .append(" year=").append(year)
                    .append(" month=").append(month)
                    .append(" timeTaken=").append(System.currentTimeMillis()-then)
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());

            return gson.toJson(digest);
        }
        catch (Exception e){
            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getAllConnectorsMonthData")
                    .append(" year=").append(year)
                    .append(" month=").append(month)
                    .append(" guestId=").append(guestId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return gson.toJson(new StatusModel(false,"Failed to get digest: " + e.getMessage()));
        }
	}

	@GET
	@Path("/all/year/{year}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getAllConnectorsYearData(@PathParam("year") int year,
			@QueryParam("filter") String filter) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
        setTransactionName(null, "GET /calendar/all/year/{year}");
        Guest guest = AuthHelper.getGuest();
        long guestId = guest.getId();
        CoachingBuddy coachee = null;
        try {
            coachee = AuthHelper.getCoachee();
        } catch (CoachRevokedException e) {
            return gson.toJson(new StatusModel(false, "Sorry, permission to access this data has been revoked."));
        }
        if (coachee!=null) {
            guestId = coachee.guestId;
            guest = guestService.getGuestById(guestId);
        }
        try {
            long then = System.currentTimeMillis();
            DigestModel digest = new DigestModel();

            digest.timeUnit = "YEAR";
            if (filter == null) {
                filter = "";
            }

            DayMetadataFacet dayMetaStart = metadataService.getDayMetadata(guest.getId(), year + "-01-01", true);

            DayMetadataFacet dayMetaEnd = metadataService.getDayMetadata(guest.getId(), year + "-12-31", true);

            DayMetadataFacet dayMetadata = new DayMetadataFacet();
            dayMetadata.timeZone = dayMetaStart.timeZone;
            dayMetadata.start = dayMetaStart.start;
            dayMetadata.end = dayMetaEnd.end;

            digest.tbounds = getStartEndResponseBoundaries(dayMetadata.start,
                                                           dayMetadata.end);
            digest.timeZoneOffset = TimeZone.getTimeZone(dayMetadata.timeZone).getOffset((digest.tbounds.start + digest.tbounds.end)/2);

            City city = metadataService.getMainCity(guestId, dayMetadata);

            /*if (city != null){
                digest.hourlyWeatherData = metadataService.getWeatherInfo(city.geo_latitude,city.geo_longitude, date, 0, 24 * 60);
                Collections.sort(digest.hourlyWeatherData);
            }*/

            setSolarInfo(digest, city, guestId, dayMetadata);

            List<ApiKey> apiKeySelection = getApiKeySelection(guestId, filter, coachee);
            digest.selectedConnectors = connectorInfos(guestId, apiKeySelection);
            List<ApiKey> allApiKeys = guestService.getApiKeys(guestId);
            allApiKeys = removeConnectorsWithoutFacets(allApiKeys, coachee);
            digest.nApis = allApiKeys.size();
            GuestSettings settings = settingsService.getSettings(AuthHelper.getGuestId());

            List<ApiKey> userKeys = new ArrayList<ApiKey>(allApiKeys);
            for (int i = 0; i < userKeys.size(); i++)
                if (userKeys.get(i).getConnector().getName().equals("google_latitude"))
                    userKeys.remove(i--);

            setCachedData(digest, userKeys, settings, apiKeySelection,
                          dayMetadata);

            copyMetadata(digest, dayMetadata);
            setVisitedCities(digest, guestId, dayMetadata);
            setNotifications(digest, AuthHelper.getGuestId());
            setCurrentAddress(digest, guestId, dayMetadata.start);
            digest.settings = new SettingsModel(settings,guest);

            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getAllConnectorsYearData")
                    .append(" year=").append(year)
                    .append(" timeTaken=").append(System.currentTimeMillis()-then)
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());
            return gson.toJson(digest);
       }
       catch (Exception e){
           StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getAllConnectorsYearData")
                   .append(" year=").append(year)
                   .append(" guestId=").append(guestId);
           logger.warn(sb.toString());
           return gson.toJson(new StatusModel(false,"Failed to get digest: " + e.getMessage()));
       }
	}

	@GET
	@Path("/all/date/{date}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getAllConnectorsDayData(@PathParam("date") String date,
			@QueryParam("filter") String filter) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
        setTransactionName(null, "GET /calendar/all/date/{date}");
        Guest guest = AuthHelper.getGuest();
        long guestId = guest.getId();
        CoachingBuddy coachee = null;
        try {
            coachee = AuthHelper.getCoachee();
        } catch (CoachRevokedException e) {
            return gson.toJson(new StatusModel(false, "Sorry, permission to access this data has been revoked. Please reload your browser window"));
        }
        if (coachee!=null) {
            guestId = coachee.guestId;
            guest = guestService.getGuestById(guestId);
        }
        try{
            long then = System.currentTimeMillis();
            DigestModel digest = new DigestModel();

            digest.timeUnit = "DAY";
            if (filter == null) {
                filter = "";
            }

            DayMetadataFacet dayMetadata = metadataService.getDayMetadata(guestId,
                    date, true);
            digest.tbounds = getStartEndResponseBoundaries(dayMetadata.start,
                    dayMetadata.end);
            digest.timeZoneOffset = TimeZone.getTimeZone(dayMetadata.timeZone).getOffset((digest.tbounds.start + digest.tbounds.end)/2);

            City city = metadataService.getMainCity(guestId, dayMetadata);

            if (city != null){
                digest.hourlyWeatherData = metadataService.getWeatherInfo(city.geo_latitude,city.geo_longitude, date, 0, 24 * 60);
                Collections.sort(digest.hourlyWeatherData);
            }

            setSolarInfo(digest, city, guestId, dayMetadata);

            List<ApiKey> apiKeySelection = getApiKeySelection(guestId, filter, coachee);
            digest.selectedConnectors = connectorInfos(guestId,apiKeySelection);
            List<ApiKey> allApiKeys = guestService.getApiKeys(guestId);
            allApiKeys = removeConnectorsWithoutFacets(allApiKeys, coachee);
            digest.nApis = allApiKeys.size();
            GuestSettings settings = settingsService.getSettings(AuthHelper.getGuestId());

            setCachedData(digest, allApiKeys, settings, apiKeySelection,
                    dayMetadata);

            copyMetadata(digest, dayMetadata);
            setVisitedCities(digest, guestId, dayMetadata);
            setNotifications(digest, AuthHelper.getGuestId());
            setCurrentAddress(digest, guestId, dayMetadata.start);
            digest.settings = new SettingsModel(settings,guest);

            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getAllConnectorsDayData")
                    .append(" date=").append(date)
                    .append(" timeTaken=").append(System.currentTimeMillis()-then)
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());
            return gson.toJson(digest);
        }
        catch (Exception e){
            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getAllConnectorsYearData")
                    .append(" date=").append(date)
                    .append(" guestId=").append(guestId);
            logger.warn(sb.toString());
            return gson.toJson(new StatusModel(false,"Failed to get digest: " + e.getMessage()));
        }
	}

    List<GuestModel> toGuestModels(List<Guest> guests) {
        List<GuestModel> models = new ArrayList<GuestModel>();
        for (Guest guest : guests) {
            models.add(new GuestModel(guest));
        }
        return models;
    }

	private List<ApiKey> removeConnectorsWithoutFacets(List<ApiKey> allApiKeys, CoachingBuddy coachee) {
		List<ApiKey> apiKeys = new ArrayList<ApiKey>();
		for (ApiKey apiKey : allApiKeys) {
            if (apiKey!=null && apiKey.getConnector()!=null && apiKey.getConnector().hasFacets()
                && (coachee==null||coachee.hasAccessToConnector(apiKey.getConnector().getName()))) {
                apiKeys.add(apiKey);
            }
		}
		return apiKeys;
	}

	@SuppressWarnings("rawtypes")
	@GET
	@Path("/{connectorName}/date/{date}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getConnectorData(@PathParam("date") String date,
			@PathParam("connectorName") String connectorName)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
        setTransactionName(null, "GET /calendar/" + connectorName + "/date/{date}");
        try{
            long then = System.currentTimeMillis();
            Connector connector = Connector.getConnector(connectorName);

            long guestId = AuthHelper.getGuestId();
            DayMetadataFacet dayMetadata = metadataService.getDayMetadata(guestId,
                                                                          date, true);
            GuestSettings settings = settingsService.getSettings(guestId);
            ConnectorResponseModel day = prepareConnectorResponseModel(dayMetadata);
            ObjectType[] objectTypes = connector.objectTypes();
            ApiKey apiKey = guestService.getApiKey(AuthHelper.getGuestId(),
                                                   connector);

            calendarDataHelper.refreshApiData(dayMetadata, apiKey, null, day);

            if (objectTypes != null) {
                for (ObjectType objectType : objectTypes) {
                    Collection<AbstractFacetVO<AbstractFacet>> facetCollection = null;
                    if (objectType.isDateBased())
                        facetCollection = getFacetVos(Arrays.asList(date), settings, connector, objectType, dayMetadata.getTimeInterval());
                    else
                        facetCollection = getFacetVos(dayMetadata, settings, connector, objectType);
                    if (facetCollection.size() > 0) {
                        day.payload = facetCollection;
                    }
                }
            }
            else {
                Collection<AbstractFacetVO<AbstractFacet>> facetCollection = getFacetVos(dayMetadata, settings,
                                                                                         connector, null);
                day.payload = facetCollection;
            }

            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getConnectorData")
                    .append(" date=").append(date)
                    .append(" connector=").append(connectorName)
                    .append(" timeTaken=").append(System.currentTimeMillis()-then)
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());

            String json = gson.toJson(day);
            return json;
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to get digest: " + e.getMessage()));
        }
	}

	private ConnectorResponseModel prepareConnectorResponseModel(
			DayMetadataFacet dayMetadata) {
		TimeBoundariesModel tb = calendarDataHelper
				.getStartEndResponseBoundaries(dayMetadata);
		ConnectorResponseModel jsr = new ConnectorResponseModel();
		jsr.tbounds = tb;
		return jsr;
	}

	@SuppressWarnings("rawtypes")
	private void setCachedData(DigestModel digest, List<ApiKey> userKeys,
			GuestSettings settings, List<ApiKey> apiKeySelection,
			DayMetadataFacet dayMetadata) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
		for (ApiKey apiKey : userKeys) {
			Connector connector = apiKey.getConnector();
			ObjectType[] objectTypes = connector.objectTypes();
            if (objectTypes != null) {
                for (ObjectType objectType : objectTypes) {
                    Collection<AbstractFacetVO<AbstractFacet>> facetCollection = null;
                    if (objectType.isDateBased())
                        facetCollection = getFacetVos(toDates(dayMetadata.start,
                                                              dayMetadata.end,
                                                              TimeZone.getTimeZone(dayMetadata.timeZone)),
                                                      settings,
                                                      connector,
                                                      objectType,
                                                      dayMetadata.getTimeInterval());
                    else
                        facetCollection = getFacetVos(dayMetadata, settings, connector, objectType);

                    setFilterInfo(dayMetadata, digest, apiKeySelection, apiKey,
                                  connector, objectType, facetCollection);
                }
            }
            else {
                Collection<AbstractFacetVO<AbstractFacet>> facetCollection = getFacetVos(dayMetadata, settings,
                                                                                         connector, null);
                setFilterInfo(dayMetadata, digest, apiKeySelection, apiKey,
                              connector, null, facetCollection);
            }
		}
	}

    private static DateTimeFormatter simpleDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");

    private List<String> toDates(final long start, final long end, TimeZone timeZoneAtFirstDay) {
        Calendar calendar = Calendar.getInstance();
        List<String> dates = new ArrayList<String>();
        calendar.setTimeInMillis(start);
        long current = start;
        for(current=start; current<end; current+=86400000) {
            dates.add(simpleDateFormatter.withZone(DateTimeZone.forTimeZone(timeZoneAtFirstDay)).print(current));
            current+=86400000;
        }
        return dates;
    }

    @SuppressWarnings("rawtypes")
	private void setFilterInfo(DayMetadataFacet dayMetadata,
			DigestModel digest, List<ApiKey> apiKeySelection, ApiKey apiKey,
			Connector connector, ObjectType objectType,
			Collection<AbstractFacetVO<AbstractFacet>> facetCollection) {
		digest.hasData(connector.getName(), facetCollection.size() > 0);
		boolean needsUpdate = needsUpdate(apiKey, dayMetadata);
        if (needsUpdate) {
            digest.setUpdateNeeded(apiKey.getConnector().getName());
        }
        if (facetCollection instanceof ImageVOCollection) {
            digest.hasPictures = true;
        }
        if (!apiKeySelection.contains(apiKey)) {
            return;
        }
		if (facetCollection.size() > 0) {
			StringBuilder sb = new StringBuilder(connector.getName());
            if (objectType != null) {
                sb.append("-").append(objectType.getName());
            }
			digest.cachedData.put(sb.toString(), facetCollection);
		}
	}

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Collection<AbstractFacetVO<AbstractFacet>> getFacetVos(List<String> dates,
                                                                   GuestSettings settings,
                                                                   Connector connector,
                                                                   ObjectType objectType,
                                                                   TimeInterval timeInterval)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        List<AbstractFacet> objectTypeFacets = calendarDataHelper.getFacets(
                connector,
                objectType,
                dates);
        return getAbstractFacetVOs(settings, objectTypeFacets, timeInterval);
    }

    private Collection<AbstractFacetVO<AbstractFacet>> getAbstractFacetVOs(final GuestSettings settings,
                                                                           final List<AbstractFacet> objectTypeFacets,
                                                                           final TimeInterval timeInterval)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Collection<AbstractFacetVO<AbstractFacet>> facetCollection = new ArrayList<AbstractFacetVO<AbstractFacet>>();
        if (objectTypeFacets != null) {
            for (AbstractFacet abstractFacet : objectTypeFacets) {
                AbstractFacetVO<AbstractFacet> facetVO = AbstractFacetVO
                        .getFacetVOClass(abstractFacet).newInstance();
                facetVO.extractValues(abstractFacet,
                                      timeInterval, settings);
                facetCollection.add(facetVO);
            }
        }
        return facetCollection;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	private Collection<AbstractFacetVO<AbstractFacet>> getFacetVos(DayMetadataFacet dayMetadata,
                                                                   GuestSettings settings, Connector connector,
                                                                   ObjectType objectType)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
        List<AbstractFacet> objectTypeFacets = calendarDataHelper.getFacets(
				connector,
				objectType,
				dayMetadata,
				getLookbackDays(connector.getName(), objectType == null ? null
                                                                        : objectType.getName()));
        return getAbstractFacetVOs(settings, objectTypeFacets, dayMetadata.getTimeInterval());
	}

    private boolean needsUpdate(ApiKey apiKey, DayMetadataFacet dayMetadata) {
		TimeInterval interval = dayMetadata.getTimeInterval();
		UpdateStrategy updateStrategy = updateStrategyFactory
				.getUpdateStrategy(apiKey.getConnector());
        if (updateStrategy == null) {
            return false;
        }
		List<UpdateInfo> updateInfos = getUpdateInfos(updateStrategy, apiKey,
				interval);
		for (UpdateInfo info : updateInfos) {
            if (info.getUpdateType() != UpdateInfo.UpdateType.NOOP_UPDATE) {
                return true;
            }
		}
		return false;
	}

	private List<UpdateInfo> getUpdateInfos(UpdateStrategy updateStrategy,
			ApiKey apiKey, TimeInterval interval) {
		List<UpdateInfo> updateInfos = new ArrayList<UpdateInfo>();
		int[] objectTypeValues = apiKey.getConnector().objectTypeValues();
		if (objectTypeValues != null && objectTypeValues.length > 0) {
			for (int i = 0; i < objectTypeValues.length; i++) {
				UpdateInfo updateInfo = updateStrategy.getUpdateInfo(apiKey,
						objectTypeValues[i], interval);
				updateInfos.add(updateInfo);
			}
		} else {
			UpdateInfo updateInfo = updateStrategy.getUpdateInfo(apiKey, -1,
					interval);
			updateInfos.add(updateInfo);
		}
		return updateInfos;
	}

	private int getLookbackDays(String connectorName, String objectTypeName) {
        if (objectTypeName == null) {
            return 0;
        }
        if (connectorName.equals("withings") && objectTypeName.equals("weight")) {
            return 15;
        }
		return 0;
	}

	private void setCurrentAddress(DigestModel digest, long guestId, long start) {
        List<GuestAddress> addresses = settingsService.getAllAddressesForDate(guestId, start);
		digest.addresses = new HashMap<String,Collection>();
        for (GuestAddress address : addresses){
            Collection collection = digest.addresses.get(address.type);
            if (collection == null){
                collection = new ArrayList();
                digest.addresses.put(address.type,collection);
            }
            collection.add(new AddressModel(address));
        }
	}

	private void setVisitedCities(DigestModel digest, long guestId,
			DayMetadataFacet md) {
		List<VisitedCity> visitedCities = new ArrayList<VisitedCity>();
		NavigableSet<VisitedCity> orderedCities = md.getOrderedCities();
		if (orderedCities != null) {
			NavigableSet<VisitedCity> descendingSet = orderedCities
					.descendingSet();
            for (VisitedCity visitedCity : descendingSet) {
                visitedCities.add(visitedCity);
            }
		}
		digest.cities = visitedCities;
	}

	private void setNotifications(DigestModel digest, long guestId) {
		List<Notification> notifications = notificationsService
				.getNotifications(guestId);
		for (Notification notification : notifications) {
			digest.addNotification(new NotificationModel(notification));
		}
	}

	private void copyMetadata(DigestModel digest, DayMetadataFacet md) {
		digest.inTransit = md.inTransit;
		digest.travelType = md.travelType;
		digest.minTempC = md.minTempC;
		digest.minTempF = md.minTempF;
		digest.maxTempC = md.maxTempC;
		digest.maxTempF = md.maxTempF;
    }

    private List<ConnectorDigestModel> connectorInfos(long guestId, List<ApiKey> apis){
        List<ConnectorDigestModel> connectors = new ArrayList<ConnectorDigestModel>();
        for (ApiKey apiKey : apis){
            if(apiKey!=null && apiKey.getConnector()!=null && apiKey.getConnector().getName()!=null) {
                Connector connector = apiKey.getConnector();
                ConnectorDigestModel model = new ConnectorDigestModel();
                connectors.add(model);
                model.connectorName = connector.getName();
                model.prettyName = connector.prettyName();
                model.channelNames = settingsService.getChannelsForConnector(guestId,connector);
                ObjectType[] objTypes = connector.objectTypes();
                if (objTypes == null)
                    continue;
                for (ObjectType obj : objTypes){
                    model.facetTypes.add(model.connectorName + "-" + obj.getName());
                }
            }
        }
        return  connectors;
    }

	private List<String> connectorNames(List<ApiKey> apis) {
		List<String> connectorNames = new ArrayList<String>();
        for (ApiKey apiKey : apis) {
            connectorNames.add(apiKey.getConnector().getName());
        }
		return connectorNames;
	}

	private List<ApiKey> getApiKeySelection(long guestId, String filter, CoachingBuddy coachee) {
		List<ApiKey> userKeys = guestService.getApiKeys(guestId);
		String[] uncheckedConnectors = filter.split(",");
		List<String> filteredOutConnectors = new ArrayList<String>();
        if (uncheckedConnectors != null && uncheckedConnectors.length > 0) {
            filteredOutConnectors = new ArrayList<String>(
                    Arrays.asList(uncheckedConnectors));
        }
		List<ApiKey> apiKeySelection = getCheckedApiKeys(userKeys,
				filteredOutConnectors, coachee);
		return apiKeySelection;
	}

	private List<ApiKey> getCheckedApiKeys(List<ApiKey> apiKeys,
			List<String> uncheckedConnectors, CoachingBuddy coachee) {
		List<ApiKey> result = new ArrayList<ApiKey>();
		there: for (ApiKey apiKey : apiKeys) {

            // Check to make sure the apiKey is valid.  Skip if it is not.
            if (apiKey==null || apiKey.getConnector()==null || apiKey.getConnector().getName()==null) {
                continue;
            }

            if (coachee!=null && !coachee.hasAccessToConnector((apiKey.getConnector().getName())))
                continue;

            // Check if apiKey should be skipped due to being in uncheckedConnectors list.
			for (int i = 0; i < uncheckedConnectors.size(); i++) {
				String connectorName = uncheckedConnectors.get(i);
				if (apiKey.getConnector().getName().equals(connectorName)) {
					continue there;
				}
			}
			result.add(apiKey);
		}
		return result;
	}

	private void setSolarInfo(DigestModel digest, City city, long guestId,
			DayMetadataFacet dayMetadata) {
        if (city != null) {
            digest.solarInfo = getSolarInfo(city.geo_latitude,
                                            city.geo_longitude, dayMetadata);
        }
        else {
            List<GuestAddress> addresses = settingsService.getAllAddressesForDate(guestId,dayMetadata.start);
            GuestAddress guestAddress = addresses.size() == 0 ? null : addresses.get(0);
            if (guestAddress != null) {
                digest.solarInfo = getSolarInfo(guestAddress.latitude,
                                                guestAddress.longitude, dayMetadata);
            }
        }
	}

	private SolarInfoModel getSolarInfo(double latitude, double longitude,
			DayMetadataFacet dayMetadata) {
		SolarInfoModel solarInfo = new SolarInfoModel();
		Location location = new Location(String.valueOf(latitude),
				String.valueOf(longitude));
		TimeZone timeZone = metadataService.getTimeZone(latitude, longitude);
		SunriseSunsetCalculator calc = new SunriseSunsetCalculator(location,
				timeZone);
		Calendar c = dayMetadata.getStartCalendar();
		Calendar sunrise = calc.getOfficialSunriseCalendarForDate(c);
		Calendar sunset = calc.getOfficialSunsetCalendarForDate(c);
		if (sunrise.getTimeInMillis() > sunset.getTimeInMillis()) {
			Calendar sr = sunrise;
			Calendar ss = sunset;
			sunset = sr;
			sunrise = ss;
		}
        solarInfo.sunrise = AbstractFacetVO.toMinuteOfDay(
                sunrise.getTime(), timeZone);
        solarInfo.sunset = AbstractFacetVO.toMinuteOfDay(sunset.getTime(),
                timeZone);
		return solarInfo;
	}

	TimeBoundariesModel getStartEndResponseBoundaries(long start, long end) {
		TimeBoundariesModel tb = new TimeBoundariesModel();
		tb.start = start;
		tb.end = end;
		return tb;
	}
}
