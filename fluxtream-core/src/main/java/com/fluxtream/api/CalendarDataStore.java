package com.fluxtream.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeSet;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.fluxtream.Configuration;
import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.auth.CoachRevokedException;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
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
import com.fluxtream.domain.metadata.VisitedCity;
import com.fluxtream.domain.metadata.WeatherInfo;
import com.fluxtream.metadata.AbstractTimespanMetadata;
import com.fluxtream.metadata.DayMetadata;
import com.fluxtream.metadata.MonthMetadata;
import com.fluxtream.metadata.WeekMetadata;
import com.fluxtream.mvc.models.AddressModel;
import com.fluxtream.mvc.models.ConnectorDigestModel;
import com.fluxtream.mvc.models.ConnectorResponseModel;
import com.fluxtream.mvc.models.DigestModel;
import com.fluxtream.mvc.models.NotificationModel;
import com.fluxtream.mvc.models.SettingsModel;
import com.fluxtream.mvc.models.SolarInfoModel;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.mvc.models.TimeBoundariesModel;
import com.fluxtream.mvc.models.VisitedCityModel;
import com.fluxtream.mvc.models.WeatherModel;
import com.fluxtream.services.CoachingService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.services.NotificationsService;
import com.fluxtream.services.SettingsService;
import com.fluxtream.utils.Utils;
import com.google.gson.Gson;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import org.codehaus.plexus.util.ExceptionUtils;
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
	@Path("/all/week/{year}/{week}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getAllConnectorsWeekData(@PathParam("year") final int year,
			@PathParam("week") final int week, @QueryParam("filter") String filter)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
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

            setCachedData(digest, allApiKeys, settings, apiKeySelection,
                          weekMetadata);

            setNotifications(digest, AuthHelper.getGuestId());
            setCurrentAddress(digest, guestId, weekMetadata.start);
            digest.settings = new SettingsModel(settings,guest);

            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getAllConnectorsWeekData")
                    .append(" year=").append(year)
                    .append(" week=").append(week)
                    .append(" timeTaken=").append(System.currentTimeMillis()-then)
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());

            digest.generationTimestamp = new java.util.Date().getTime();

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

	@GET
	@Path("/all/month/{year}/{month}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getAllConnectorsMonthData(@PathParam("year") final int year,
			@PathParam("month") final int month,
			@QueryParam("filter") String filter) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
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

            setCachedData(digest, allApiKeys, settings, apiKeySelection,
                          monthMetadata);

            setNotifications(digest, AuthHelper.getGuestId());
            setCurrentAddress(digest, guestId, monthMetadata.start);
            digest.settings = new SettingsModel(settings, guest);

            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getAllConnectorsMonthData")
                    .append(" year=").append(year)
                    .append(" month=").append(month)
                    .append(" timeTaken=").append(System.currentTimeMillis()-then)
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());

            digest.generationTimestamp = new java.util.Date().getTime();

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
            final List<WeatherInfo> weatherInfo = metadataService.getWeatherInfo(city.geo_latitude, city.geo_longitude, date, 0, 24 * 60);
            Collections.sort(weatherInfo);
            model.hourlyWeatherData = weatherInfo;
            setMinMaxTemperatures(model, weatherInfo);
            model.solarInfo = getSolarInfo(city.geo_latitude, city.geo_longitude, dayMetadata);
        }

        return model;
    }

    public void setMinMaxTemperatures(WeatherModel info,
                                      List<WeatherInfo> weatherInfo) {
        info.maxTempC = Integer.MIN_VALUE;
        info.minTempC = Integer.MAX_VALUE;
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

            setCachedData(digest, allApiKeys, settings, apiKeySelection,
                    dayMetadata);

            setNotifications(digest, AuthHelper.getGuestId());
            setCurrentAddress(digest, guestId, dayMetadata.start);
            digest.settings = new SettingsModel(settings,guest);

            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getAllConnectorsDayData")
                    .append(" date=").append(date)
                    .append(" timeTaken=").append(System.currentTimeMillis()-then)
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());

            digest.generationTimestamp = new java.util.Date().getTime();

            return gson.toJson(digest);
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

    private void setMetadata(final DigestModel digest, final AbstractTimespanMetadata dayMetadata, String[] dates) {
        digest.metadata.mainCity = new VisitedCityModel(dayMetadata.consensusVisitedCity, env,dates[0]);
        List<VisitedCityModel> cityModels = new ArrayList<VisitedCityModel>();
        TreeSet<VisitedCity> orderedCities = new TreeSet<VisitedCity>(dayMetadata.getCities());
        final Iterator<VisitedCity> cityIterator = orderedCities.iterator();
        int count = 0;
        while (cityIterator.hasNext()) {
            VisitedCity city = cityIterator.next();
            VisitedCityModel cityModel = new VisitedCityModel(city, env);
            cityModels.add(cityModel);
        }
        digest.metadata.cities = cityModels;
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
			DayMetadata dayMetadata) {
		TimeBoundariesModel tb = calendarDataHelper
				.getStartEndResponseBoundaries(dayMetadata);
		ConnectorResponseModel jsr = new ConnectorResponseModel();
		jsr.tbounds = tb;
		return jsr;
	}

	@SuppressWarnings("rawtypes")
	private void setCachedData(DigestModel digest, List<ApiKey> userKeys,
			GuestSettings settings, List<ApiKey> apiKeySelection,
			AbstractTimespanMetadata timespanMetadata)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, OutsideTimeBoundariesException
    {
		for (ApiKey apiKey : userKeys) {
			Connector connector = apiKey.getConnector();
			ObjectType[] objectTypes = connector.objectTypes();
            if (objectTypes != null) {
                for (ObjectType objectType : objectTypes) {
                    Collection<AbstractFacetVO<AbstractFacet>> facetCollection = null;
                    if (objectType.isDateBased())
                        facetCollection = getFacetVos(toDates(timespanMetadata),
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Collection<AbstractFacetVO<AbstractFacet>> getFacetVos(List<String> dates,
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
        return getAbstractFacetVOs(settings, objectTypeFacets, timeInterval);
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
