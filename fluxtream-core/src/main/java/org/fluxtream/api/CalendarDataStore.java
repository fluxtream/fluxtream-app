package org.fluxtream.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeSet;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.fluxtream.Configuration;
import org.fluxtream.OutsideTimeBoundariesException;
import org.fluxtream.TimeInterval;
import org.fluxtream.TimeUnit;
import org.fluxtream.aspects.FlxLogger;
import org.fluxtream.auth.AuthHelper;
import org.fluxtream.auth.CoachRevokedException;
import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.ObjectType;
import org.fluxtream.connectors.updaters.UpdateFailedException;
import org.fluxtream.connectors.vos.AbstractFacetVO;
import org.fluxtream.connectors.vos.AbstractTimedFacetVO;
import org.fluxtream.connectors.vos.ImageVOCollection;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.domain.AbstractRepeatableFacet;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.domain.CoachingBuddy;
import org.fluxtream.domain.Guest;
import org.fluxtream.domain.GuestAddress;
import org.fluxtream.domain.GuestSettings;
import org.fluxtream.domain.Notification;
import org.fluxtream.domain.metadata.City;
import org.fluxtream.domain.metadata.VisitedCity;
import org.fluxtream.domain.metadata.WeatherInfo;
import org.fluxtream.metadata.AbstractTimespanMetadata;
import org.fluxtream.metadata.DayMetadata;
import org.fluxtream.metadata.MonthMetadata;
import org.fluxtream.metadata.WeekMetadata;
import org.fluxtream.mvc.models.AddressModel;
import org.fluxtream.mvc.models.ConnectorDigestModel;
import org.fluxtream.mvc.models.ConnectorResponseModel;
import org.fluxtream.mvc.models.DigestModel;
import org.fluxtream.mvc.models.NotificationModel;
import org.fluxtream.mvc.models.SettingsModel;
import org.fluxtream.mvc.models.SolarInfoModel;
import org.fluxtream.mvc.models.StatusModel;
import org.fluxtream.mvc.models.TimeBoundariesModel;
import org.fluxtream.mvc.models.VisitedCityModel;
import org.fluxtream.mvc.models.WeatherModel;
import org.fluxtream.services.CoachingService;
import org.fluxtream.services.GuestService;
import org.fluxtream.services.MetadataService;
import org.fluxtream.services.NotificationsService;
import org.fluxtream.services.SettingsService;
import org.fluxtream.utils.TimeUtils;
import org.fluxtream.utils.Utils;
import com.google.gson.Gson;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.plexus.util.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/calendar")
@Component("RESTCalendarDataStore")
@Scope("request")
public class CalendarDataStore {

    FlxLogger logger = FlxLogger.getLogger(CalendarDataStore.class);

	@Autowired
	GuestService guestService;

	@Autowired
	MetadataService metadataService;

	@Autowired
	SettingsService settingsService;

	@Autowired
	NotificationsService notificationsService;

	@Autowired
    CalendarDataHelper calendarDataHelper;

    @Autowired
    CoachingService coachingService;

    @Autowired
    Configuration env;

	Gson gson = new Gson();

    @GET
    @Path("/location/week/{year}/{week}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getLocationConnectorsWeekData(@PathParam("year") final int year,
                                                @PathParam("week") final int week,
                                                @QueryParam("filter") String filter) {
        return getWeekData(year, week, filter, true);
    }

    @GET
    @Path("/all/week/{year}/{week}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getAllConnectorsWeekData(@PathParam("year") final int year,
                                           @PathParam("week") final int week,
                                           @QueryParam("filter") String filter) {
        return getWeekData(year, week, filter, false);
    }

	public String getWeekData(final int year,
			                  final int week,
                              String filter,
                              boolean locationDataOnly) {
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

            WeekMetadata weekMetadata = metadataService.getWeekMetadata(guestId, year, week);
            DigestModel digest = new DigestModel(TimeUnit.WEEK, weekMetadata, env);

            if (filter == null) {
                filter = "";
            }

            setMetadata(digest, weekMetadata,metadataService.getDatesForWeek(year,week).toArray(new String[]{}));

            digest.tbounds = getStartEndResponseBoundaries(weekMetadata.start,
                                                           weekMetadata.end);

            List<ApiKey> apiKeySelection = getApiKeySelection(guestId, filter, coachee);
            digest.selectedConnectors = connectorInfos(guestId,apiKeySelection);
            List<ApiKey> allApiKeys = guestService.getApiKeys(guestId);
            allApiKeys = removeConnectorsWithoutFacets(allApiKeys, coachee);
            digest.nApis = allApiKeys.size();
            GuestSettings settings = settingsService.getSettings(AuthHelper.getGuestId());

            Map<Long,Object> connectorSettings = new HashMap<Long,Object>();

            setCachedData(digest, allApiKeys, settings, connectorSettings, apiKeySelection, weekMetadata, locationDataOnly);

            setNotifications(digest, AuthHelper.getGuestId());
            setCurrentAddress(digest, guestId, weekMetadata.start);
            digest.settings = new SettingsModel(settings, connectorSettings, guest);

            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getAllConnectorsWeekData")
                    .append(" year=").append(year)
                    .append(" week=").append(week)
                    .append(" timeTaken=").append(System.currentTimeMillis()-then)
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());

            digest.generationTimestamp = new java.util.Date().getTime();

            final String s = toJacksonJson(digest);
            return s;
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

    @GET
    @Path("/location/month/{year}/{month}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getLocationConnectorsMonthData(@PathParam("year") final int year,
                                                 @PathParam("month") final int month,
                                                 @QueryParam("filter") String filter) {
        return getMonthData(year, month, filter, true);
    }

    @GET
    @Path("/all/month/{year}/{month}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getAllConnectorsMonthData(@PathParam("year") final int year,
                                            @PathParam("month") final int month,
                                            @QueryParam("filter") String filter) {
        return getMonthData(year, month, filter, false);
    }

    private String getMonthData(final int year, final int month, String filter, boolean locationDataOnly) {
        Guest guest = AuthHelper.getGuest();
        long guestId = guest.getId();
        CoachingBuddy coachee;
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
            MonthMetadata monthMetadata = metadataService.getMonthMetadata(guestId, year, month);
            DigestModel digest = new DigestModel(TimeUnit.MONTH, monthMetadata, env);

            digest.metadata.timeUnit = "MONTH";
            if (filter == null) {
                filter = "";
            }

            setMetadata(digest, monthMetadata,metadataService.getDatesForMonth(year,month).toArray(new String[]{}));

            digest.tbounds = getStartEndResponseBoundaries(monthMetadata.start,
                                                           monthMetadata.end);

            List<ApiKey> apiKeySelection = getApiKeySelection(guestId, filter, coachee);
            digest.selectedConnectors = connectorInfos(guestId,apiKeySelection);
            List<ApiKey> allApiKeys = guestService.getApiKeys(guestId);
            allApiKeys = removeConnectorsWithoutFacets(allApiKeys, coachee);
            digest.nApis = allApiKeys.size();
            GuestSettings settings = settingsService.getSettings(AuthHelper.getGuestId());

            Map<Long,Object> connectorSettings = new HashMap<Long,Object>();
            setCachedData(digest, allApiKeys, settings, connectorSettings, apiKeySelection, monthMetadata, locationDataOnly);

            setNotifications(digest, AuthHelper.getGuestId());
            setCurrentAddress(digest, guestId, monthMetadata.start);
            digest.settings = new SettingsModel(settings, connectorSettings, guest);

            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getAllConnectorsMonthData")
                    .append(" year=").append(year)
                    .append(" month=").append(month)
                    .append(" timeTaken=").append(System.currentTimeMillis()-then)
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());

            digest.generationTimestamp = new java.util.Date().getTime();

            final String s = toJacksonJson(digest);
            return s;
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
    @Path("/weather/date/{date}")
    @Produces({ MediaType.APPLICATION_JSON })
    public WeatherModel getWeatherDataForADay(@PathParam("date") String date) {

        Guest guest = AuthHelper.getGuest();
        long guestId = guest.getId();

        GuestSettings settings = settingsService.getSettings(guestId);

        DayMetadata dayMetadata = metadataService.getDayMetadata(guestId, date);
        WeatherModel model = new WeatherModel(settings.temperatureUnit);
        model.tbounds = getStartEndResponseBoundaries(dayMetadata.start, dayMetadata.end);

        City city = dayMetadata.consensusVisitedCity.city;
        if (city != null){
            final List<WeatherInfo> weatherInfo = metadataService.getWeatherInfo(city.geo_latitude, city.geo_longitude, date);
            Collections.sort(weatherInfo);
            model.hourlyWeatherData = weatherInfo;
            setMinMaxTemperatures(model, weatherInfo);
            model.solarInfo = getSolarInfo(city.geo_latitude, city.geo_longitude, dayMetadata);
        }

        return model;
    }

    public void setMinMaxTemperatures(WeatherModel info,
                                      List<WeatherInfo> weatherInfo) {
        if (weatherInfo.size() == 0)
            return;

        info.maxTempC = Integer.MIN_VALUE;
        info.minTempC = Integer.MAX_VALUE;
        info.maxTempF = Integer.MIN_VALUE;
        info.minTempF = Integer.MAX_VALUE;

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

	@GET
	@Path("/all/date/{date}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getAllConnectorsDayData(@PathParam("date") String date,
			@QueryParam("filter") String filter) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
        Guest guest;
        long guestId;
        try {
            guest = AuthHelper.getGuest();
            guestId = guest.getId();
        } catch (Throwable e) {
            return gson.toJson(new StatusModel(false, "You are no longer logged in. Please reload your browser window"));
        }

        CoachingBuddy coachee;
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
            DayMetadata dayMetadata = metadataService.getDayMetadata(guestId, date);
            DigestModel digest = new DigestModel(TimeUnit.DAY, dayMetadata, env);

            digest.metadata.timeUnit = "DAY";
            if (filter == null) {
                filter = "";
            }

            digest.tbounds = getStartEndResponseBoundaries(dayMetadata.start,
                    dayMetadata.end);

            setMetadata(digest, dayMetadata, new String[]{date});

            if (digest.metadata.mainCity!=null)
                setSolarInfo(digest, dayMetadata.consensusVisitedCity.city, guestId, dayMetadata);

            List<ApiKey> apiKeySelection = getApiKeySelection(guestId, filter, coachee);
            digest.selectedConnectors = connectorInfos(guestId,apiKeySelection);
            List<ApiKey> allApiKeys = guestService.getApiKeys(guestId);
            allApiKeys = removeConnectorsWithoutFacets(allApiKeys, coachee);
            digest.nApis = allApiKeys.size();
            GuestSettings settings = settingsService.getSettings(AuthHelper.getGuestId());

            Map<Long,Object> connectorSettings = new HashMap<Long,Object>();
            setCachedData(digest, allApiKeys, settings, connectorSettings, apiKeySelection, dayMetadata, false);

            setNotifications(digest, AuthHelper.getGuestId());
            setCurrentAddress(digest, guestId, dayMetadata.start);
            digest.settings = new SettingsModel(settings, connectorSettings, guest);

            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getAllConnectorsDayData")
                    .append(" date=").append(date)
                    .append(" timeTaken=").append(System.currentTimeMillis()-then)
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());

            digest.generationTimestamp = new java.util.Date().getTime();

            final String s = toJacksonJson(digest);
            return s;
        }
        catch (Exception e){
            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getAllConnectorsDayData")
                    .append(" date=").append(date)
                    .append(" guestId=").append(guestId);
            logger.warn(sb.toString());
            final StatusModel src = new StatusModel(false, "Failed to get digest: " + e.getMessage());
            src.payload = ExceptionUtils.getStackTrace(e);
            return gson.toJson(src);
        }
	}

    private String toJacksonJson(final DigestModel digest) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        objectMapper.setVisibilityChecker(
                objectMapper.getSerializationConfig().getDefaultVisibilityChecker().
                        withFieldVisibility(JsonAutoDetect.Visibility.NON_PRIVATE));
        return objectMapper.writeValueAsString(digest);
    }

    private void setMetadata(final DigestModel digest, final AbstractTimespanMetadata dayMetadata, String[] dates) {
        digest.metadata.mainCity = new VisitedCityModel(dayMetadata.consensusVisitedCity, env,dates[0]);
        List<VisitedCityModel> cityModels = new ArrayList<VisitedCityModel>();
        TreeSet<VisitedCity> orderedCities = new TreeSet<VisitedCity>(dayMetadata.getCities());
        for (VisitedCity city : orderedCities) {
            VisitedCityModel cityModel = new VisitedCityModel(city, env);
            cityModels.add(cityModel);
        }
        digest.metadata.cities = cityModels;
        List<VisitedCityModel> consensusCityModels = new ArrayList<VisitedCityModel>();
        TreeSet<VisitedCity> orderedConsensusCities = new TreeSet<VisitedCity>(dayMetadata.getConsensusCities());
        for (VisitedCity city : orderedConsensusCities) {
            VisitedCityModel cityModel = new VisitedCityModel(city, env);
            consensusCityModels.add(cityModel);
        }
        digest.metadata.consensusCities = consensusCityModels;
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
        try{
            long then = System.currentTimeMillis();
            Connector connector = Connector.getConnector(connectorName);

            long guestId = AuthHelper.getGuestId();
            DayMetadata dayMetadata = metadataService.getDayMetadata(guestId, date);
            GuestSettings settings = settingsService.getSettings(guestId);
            ConnectorResponseModel day = prepareConnectorResponseModel(dayMetadata);
            ObjectType[] objectTypes = connector.objectTypes();

            if (objectTypes != null) {
                for (ObjectType objectType : objectTypes) {
                    Collection<AbstractFacetVO<AbstractFacet>> facetCollection;
                    if (objectType.isDateBased())
                        facetCollection = getFacetVos(dayMetadata, Arrays.asList(date), settings, connector, objectType, dayMetadata.getTimeInterval());
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
			DayMetadata dayMetadata) {
		TimeBoundariesModel tb = calendarDataHelper
				.getStartEndResponseBoundaries(dayMetadata);
		ConnectorResponseModel jsr = new ConnectorResponseModel();
		jsr.tbounds = tb;
		return jsr;
	}

	@SuppressWarnings("rawtypes")
	private void setCachedData(DigestModel digest,
                               List<ApiKey> userKeys,
                               GuestSettings settings,
                               final Map<Long, Object> connectorSettings,
                               List<ApiKey> apiKeySelection,
                               AbstractTimespanMetadata timespanMetadata,
                               boolean locationDataOnly)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, OutsideTimeBoundariesException, UpdateFailedException
    {
		for (ApiKey apiKey : userKeys) {
			Connector connector = apiKey.getConnector();
            final Object apiKeySettings = settingsService.getConnectorSettings(apiKey.getId());
            if (apiKeySettings!=null)
                connectorSettings.put(apiKey.getId(), apiKeySettings);
            ObjectType[] objectTypes = connector.objectTypes();
            if (objectTypes != null) {
                for (ObjectType objectType : objectTypes) {
                    if (!objectType.isClientFacet())
                        continue;
                    final TimeUnit timeUnit = timespanMetadata.getTimeInterval().getTimeUnit();
                    if (timeUnit !=TimeUnit.DAY) {
                        if (!locationDataOnly&&objectType!=null&&objectType.getName().equals("location"))
                            continue;
                        else if (locationDataOnly &&
                                 ((objectType==null)||(objectType!=null&&!objectType.getName().equals("location")))){
                            continue;
                        }
                    }
                    Collection<AbstractFacetVO<AbstractFacet>> facetCollection;
                    if (objectType.isMixedType()) {
                        facetCollection = getFacetVos(timespanMetadata, settings, connector, objectType);
                        facetCollection.addAll(getFacetVOs(timespanMetadata, settings, connector, objectType, timespanMetadata.getTimeInterval()));
                    }
                    else if (objectType.isDateBased())
                        facetCollection = getFacetVos(timespanMetadata,
                                                      toDates(timespanMetadata),
                                                      settings,
                                                      connector,
                                                      objectType,
                                                      timespanMetadata.getTimeInterval());
                    else
                        facetCollection = getFacetVos(timespanMetadata, settings, connector, objectType);

                    setFilterInfo(digest, apiKeySelection, apiKey,
                                  connector, objectType, facetCollection);
                }
            }
            else {
                Collection<AbstractFacetVO<AbstractFacet>> facetCollection = getFacetVos(timespanMetadata, settings,
                                                                                         connector, null);
                setFilterInfo(digest, apiKeySelection, apiKey,
                              connector, null, facetCollection);
            }
		}
	}

    private String firstDate(final AbstractTimespanMetadata timespanMetadata) {
        return(timespanMetadata.getDateList().get(0));
    }

    private String lastDate(final AbstractTimespanMetadata timespanMetadata) {
        final List<String> dateList = timespanMetadata.getDateList();
        return(dateList.get(dateList.size() - 1));
    }

    private List<String> toDates(final AbstractTimespanMetadata timespanMetadata) {
        return(timespanMetadata.getDateList());

        // The above was added by Anne on 6/29/13.  The original version was below, but it assumed that
        // the cities array contains entries corresponding exactly to the first and last day of the week.
        // That assumption does not appear to be true.
        //final List<VisitedCity> cities = timespanMetadata.getCities();
        //List<String> dates = new ArrayList<String>();
        //for (VisitedCity city : cities) {
        //    dates.add(city.date);
        //}
        //return dates;
    }

    @SuppressWarnings("rawtypes")
	private void setFilterInfo(DigestModel digest, List<ApiKey> apiKeySelection, ApiKey apiKey,
			Connector connector, ObjectType objectType,
			Collection<AbstractFacetVO<AbstractFacet>> facetCollection) {
		digest.hasData(connector.getName(), facetCollection.size() > 0);
        //boolean needsUpdate = needsUpdate(apiKey, dayMetadata);
        //if (needsUpdate) {
        //    digest.setUpdateNeeded(apiKey.getConnector().getName());
        //}
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

    private Collection<AbstractFacetVO<AbstractFacet>> getFacetVOs(AbstractTimespanMetadata timespanMetadata,
                                                                   final GuestSettings settings, final Connector connector,
                                                                   final ObjectType objectType, final TimeInterval timeInterval)
            throws ClassNotFoundException, OutsideTimeBoundariesException, InstantiationException, IllegalAccessException {
        List<AbstractRepeatableFacet> objectTypeFacets = calendarDataHelper.getFacets(connector, objectType, firstDate(timespanMetadata), lastDate(timespanMetadata));
        final Collection<AbstractFacetVO<AbstractFacet>> vos = expandToFacetVOs(settings, timespanMetadata, objectTypeFacets, timeInterval);
        return filterByDate(connector, timespanMetadata, vos);
    }

    private Collection<AbstractFacetVO<AbstractFacet>> filterByDate(Connector connector, AbstractTimespanMetadata timespanMetadata, final Collection<AbstractFacetVO<AbstractFacet>> vos) {
        if (connector.getName().equals("moves")) return vos;
        final List<String> dateList = timespanMetadata.getDateList();
        final List<AbstractFacetVO<AbstractFacet>> filtered = new ArrayList<AbstractFacetVO<AbstractFacet>>();
        for (AbstractFacetVO<AbstractFacet> vo : vos) {
            if (dateList.contains(vo.date)||(vo.api==1999&&vo.objectType==4))
                filtered.add(vo);
        }
        return filtered;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Collection<AbstractFacetVO<AbstractFacet>> getFacetVos(AbstractTimespanMetadata timespanMetadata,
                                                                   List<String> dates,
                                                                   GuestSettings settings,
                                                                   Connector connector,
                                                                   ObjectType objectType,
                                                                   TimeInterval timeInterval)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, OutsideTimeBoundariesException
    {
        List<AbstractFacet> objectTypeFacets = calendarDataHelper.getFacets(
                connector,
                objectType,
                dates);
        final Collection<AbstractFacetVO<AbstractFacet>> vos = getAbstractFacetVOs(settings, objectTypeFacets, timeInterval);
        return filterByDate(connector, timespanMetadata, vos);
    }

    private Collection<AbstractFacetVO<AbstractFacet>> expandToFacetVOs(final GuestSettings settings, final AbstractTimespanMetadata timespanMetadata, final List<AbstractRepeatableFacet> objectTypeFacets, TimeInterval timeInterval)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, OutsideTimeBoundariesException
    {
        Collection<AbstractFacetVO<AbstractFacet>> facetCollection = new ArrayList<AbstractFacetVO<AbstractFacet>>();
        final List<String> timespanDates = timespanMetadata.getDateList();
        if (objectTypeFacets != null) {
            for (AbstractRepeatableFacet abstractFacet : objectTypeFacets) {
                final List<String> repeatedDates = abstractFacet.getRepeatedDates();
                for (String repeatedDate : repeatedDates) {
                    if (!timespanDates.contains(repeatedDate))
                        continue;
                    final TimeZone timeZone = timeInterval.getTimeZone(repeatedDate);
                    AbstractTimedFacetVO<AbstractFacet> facetVO = (AbstractTimedFacetVO<AbstractFacet>)AbstractFacetVO
                            .getFacetVOClass((AbstractFacet)abstractFacet).newInstance();
                    try {
                        facetVO.extractValues(abstractFacet, timeInterval, settings);
                        final DateTime startTime = TimeUtils.dateFormatter.withZone(DateTimeZone.forTimeZone(timeZone)).parseDateTime(repeatedDate);
                        facetVO.start = startTime.getMillis();
                        facetVO.end = startTime.getMillis() + DateTimeConstants.MILLIS_PER_DAY;
                        facetCollection.add(facetVO);
                    } catch(OutsideTimeBoundariesException e) {
                        // OutsideTimeBoundariesException can legitimately happen in the case that the timezone
                        // for a date differs from the date used by a given service to return the data.
                        // Don't print a stack trace.
                        //e.printStackTrace();
                    } catch(Throwable e) {
                        // An unexpected error happened generating the VO for this facet.  Skip it.
                        e.printStackTrace();
                    }
                }

            }
        }
        return facetCollection;
    }

    private Collection<AbstractFacetVO<AbstractFacet>> getAbstractFacetVOs(final GuestSettings settings,
                                                                           final List<AbstractFacet> objectTypeFacets,
                                                                           final TimeInterval timeInterval)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, OutsideTimeBoundariesException
    {
        Collection<AbstractFacetVO<AbstractFacet>> facetCollection = new ArrayList<AbstractFacetVO<AbstractFacet>>();
        if (objectTypeFacets != null) {
            for (AbstractFacet abstractFacet : objectTypeFacets) {
                AbstractFacetVO<AbstractFacet> facetVO = AbstractFacetVO
                        .getFacetVOClass(abstractFacet).newInstance();
                try {
                    facetVO.extractValues(abstractFacet,
                                        timeInterval, settings);
                    facetCollection.add(facetVO);
                } catch(OutsideTimeBoundariesException e) {
                    // OutsideTimeBoundariesException can legitimately happen in the case that the timezone
                    // for a date differs from the date used by a given service to return the data.
                    // Don't print a stack trace.
                    //e.printStackTrace();
                } catch(Throwable e) {
                    // An unexpected error happened generating the VO for this facet.  Skip it.
                    e.printStackTrace();
                }

            }
        }
        return facetCollection;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	private Collection<AbstractFacetVO<AbstractFacet>> getFacetVos(AbstractTimespanMetadata timespanMetadata,
                                                                   GuestSettings settings, Connector connector,
                                                                   ObjectType objectType)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, OutsideTimeBoundariesException
    {
        List<AbstractFacet> objectTypeFacets = calendarDataHelper.getFacets(connector, objectType, timespanMetadata);
        return getAbstractFacetVOs(settings, objectTypeFacets, timespanMetadata.getTimeInterval());
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

	private void setNotifications(DigestModel digest, long guestId) {
		List<Notification> notifications = notificationsService
				.getNotifications(guestId);
		for (Notification notification : notifications) {
			digest.addNotification(new NotificationModel(notification));
		}
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
			DayMetadata dayMetadata) {
        if (city != null) {
            digest.metadata.solarInfo = getSolarInfo(city.geo_latitude,
                                            city.geo_longitude, dayMetadata);
        }
        else {
            List<GuestAddress> addresses = settingsService.getAllAddressesForDate(guestId,dayMetadata.start);
            GuestAddress guestAddress = addresses.size() == 0 ? null : addresses.get(0);
            if (guestAddress != null) {
                digest.metadata.solarInfo = getSolarInfo(guestAddress.latitude,
                                                guestAddress.longitude, dayMetadata);
            }
        }
	}

	private SolarInfoModel getSolarInfo(double latitude, double longitude,
			DayMetadata dayMetadata) {
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
