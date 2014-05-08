package org.fluxtream.core.api;

import com.google.gson.Gson;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.Authorization;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.plexus.util.ExceptionUtils;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.TimeUnit;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.auth.CoachRevokedException;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.connectors.vos.AbstractFacetVO;
import org.fluxtream.core.connectors.vos.AbstractTimedFacetVO;
import org.fluxtream.core.domain.*;
import org.fluxtream.core.domain.metadata.City;
import org.fluxtream.core.domain.metadata.VisitedCity;
import org.fluxtream.core.domain.metadata.WeatherInfo;
import org.fluxtream.core.metadata.AbstractTimespanMetadata;
import org.fluxtream.core.metadata.DayMetadata;
import org.fluxtream.core.metadata.MonthMetadata;
import org.fluxtream.core.metadata.WeekMetadata;
import org.fluxtream.core.mvc.models.*;
import org.fluxtream.core.services.*;
import org.fluxtream.core.utils.TimeUtils;
import org.fluxtream.core.utils.Utils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;

@Path("/calendar")
@Api(value = "/calendar", description = "Main devices and service API facets consumption operations")
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
    @ApiOperation(value = "Get the user's location data for a specific week", response = DigestModel.class,
                  notes="Locations can get quite heavy and take a while to parse, so we provide them in a separate call.")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getLocationConnectorsWeekData(@ApiParam(value="Year", required=true) @PathParam("year") final int year,
                                                @ApiParam(value="Week", required=true) @PathParam("week") final int week,
                                                @ApiParam(value="filter JSON", required=true) @QueryParam("filter") String filter) {
        return getWeekData(year, week, filter, true);
    }

    @GET
    @Path("/all/week/{year}/{week}")
    @ApiOperation(value = "Get all the user's connectors' data for a specific week", response = DigestModel.class,
                  notes="Unlike its date-based equivalent, this call will not contain Location data")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getAllConnectorsWeekData(@ApiParam(value="Year", required=true) @PathParam("year") final int year,
                                           @ApiParam(value="Week", required=true) @PathParam("week") final int week,
                                           @ApiParam(value="filter JSON", required=true) @QueryParam("filter") String filter) {
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
            CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, "week/" + year + "/" + week);
            DigestModel digest = new DigestModel(TimeUnit.WEEK, weekMetadata, env, calendarModel);

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
    @ApiOperation(value = "Get the user's location data for a specific month", response = DigestModel.class,
                  notes="Locations can get quite heavy and take a while to parse, so we provide them in a separate call.")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getLocationConnectorsMonthData(@ApiParam(value="Year", required=true) @PathParam("year") final int year,
                                                 @ApiParam(value="Month", required=true) @PathParam("month") final int month,
                                                 @ApiParam(value="Filter JSON", required=true) @QueryParam("filter") String filter) {
        return getMonthData(year, month, filter, true);
    }

    @GET
    @Path("/all/month/{year}/{month}")
    @ApiOperation(value = "Get all the user's connectors' data for a specific month", response = DigestModel.class,
                  notes="Unlike its date-based equivalent, this call will not contain Location data")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getAllConnectorsMonthData(@ApiParam(value="Year", required=true) @PathParam("year") final int year,
                                            @ApiParam(value="Month", required=true) @PathParam("month") final int month,
                                            @ApiParam(value="Filter JSON", required=true) @QueryParam("filter") String filter) {
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
            CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, "month/" + year + "/" + month);
            DigestModel digest = new DigestModel(TimeUnit.MONTH, monthMetadata, env, calendarModel);

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
    @ApiOperation(value = "Get the user's location-based weather data on a specific date", response = WeatherModel.class)
    @Produces({ MediaType.APPLICATION_JSON })
    public WeatherModel getWeatherDataForADay(@ApiParam(value="Date", required=true) @PathParam("date") String date) {

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
    @ApiOperation(value = "Get the user's connectors' data for a specific date", response = DigestModel.class)
	@Produces({ MediaType.APPLICATION_JSON })
	public String getAllConnectorsDayData(@ApiParam(value="Date (YYYY-MM-DD)", required=true) @PathParam("date") String date,
                                          @ApiParam(value="Filter JSON", required=false) @QueryParam(value="filter") String filter) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
        if (StringUtils.isEmpty(filter))
            filter = "{}";
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
            CalendarModel calendarModel = CalendarModel.fromState(guestId, metadataService, "date/" + date);
            DigestModel digest = new DigestModel(TimeUnit.DAY, dayMetadata, env, calendarModel);

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

    private String toJacksonJson(ConnectorResponseModel  connectorResponse) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        objectMapper.setVisibilityChecker(
                objectMapper.getSerializationConfig().getDefaultVisibilityChecker().
                        withFieldVisibility(JsonAutoDetect.Visibility.NON_PRIVATE));
        return objectMapper.writeValueAsString(connectorResponse);
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

    private Collection<AbstractFacetVO<AbstractFacet>> getFacetCollection(AbstractTimespanMetadata timespanMetadata, GuestSettings settings, Connector connector, ObjectType objectType){
        try{
            Collection<AbstractFacetVO<AbstractFacet>> facetCollection;
            if (objectType != null) {
                if (objectType.isMixedType()){
                    facetCollection = getFacetVos(timespanMetadata, settings, connector, objectType);
                    facetCollection.addAll(getFacetVOs(timespanMetadata, settings, connector, objectType, timespanMetadata.getTimeInterval()));
                }
                else if (objectType.isDateBased())
                    facetCollection = getFacetVos(timespanMetadata, toDates(timespanMetadata), settings, connector, objectType, timespanMetadata.getTimeInterval());
                else
                    facetCollection = getFacetVos(timespanMetadata, settings, connector, objectType);

            }
            else {
                facetCollection = getFacetVos(timespanMetadata, settings, connector, null);
            }
            return facetCollection;
        }
        catch (Exception e){
            return new ArrayList<AbstractFacetVO<AbstractFacet>>();
        }
    }

    private void appendFacetsToConnectorResponseModel(ConnectorResponseModel model, Collection<AbstractFacetVO<AbstractFacet>> facetCollection,Connector connector, ObjectType objectType){
        if (facetCollection.size() > 0){
            if (model.facets == null)
                model.facets = new HashMap<String,Collection<AbstractFacetVO<AbstractFacet>>>();
            StringBuilder name = new StringBuilder(connector.getName());
            if (objectType != null)
                name.append("-").append(objectType.getName());
            model.facets.put(name.toString(),facetCollection);
        }
    }

    List<ApiKey> getApiKeyListFromConnectorObjectsEncoding(Guest guest, String connectorObjectsEncoded){
        String[] mainList = connectorObjectsEncoded.split(",");
        List<ApiKey> apiKeys = guestService.getApiKeys(guest.getId());
        List<ApiKey> apiKeysToReturn = new LinkedList<ApiKey>();
        for (String connectorObject : mainList){
            String connectorIdentifier = connectorObject.split("-")[0];
            if (connectorIdentifier.matches("[1-9][0-9]*")){//it's an id
                long apiKeyId = Long.parseLong(connectorIdentifier);
                for (Iterator<ApiKey> iter = apiKeys.iterator(); iter.hasNext();){
                    ApiKey apiKey = iter.next();
                    if (apiKey.getId() == apiKeyId){
                        apiKeysToReturn.add(apiKey);
                        iter.remove();
                        break;
                    }
                }
            }
            else{
                for (Iterator<ApiKey> iter = apiKeys.iterator(); iter.hasNext();){
                    ApiKey apiKey = iter.next();
                    if (apiKey.getConnector().getName().equals(connectorIdentifier)){
                        apiKeysToReturn.add(apiKey);
                        iter.remove();
                        break;
                    }
                }

            }

        }
        return apiKeysToReturn;
    }

    Map<ApiKey,List<ObjectType>> getObjectTypesFromConnectorObjectsEncoding(List<ApiKey> apiKeys, String connectorObjectsEncoded){
        String[] mainList = connectorObjectsEncoded.split(",");
        Map<ApiKey,List<ObjectType>> result = new HashMap<ApiKey,List<ObjectType>>();
        for (ApiKey apiKey : apiKeys){
            result.put(apiKey,new ArrayList<ObjectType>());
        }
        for (String connectorObject : mainList){
            String[] parts = connectorObject.split("-");
            if (parts.length < 2)
                continue;
            String connectorIdentifier = parts[0];
            String objectTypeName = parts[1];
            ApiKey apiKey = null;
            if (connectorIdentifier.matches("[1-9][0-9]*")){//it's an id
                long apiKeyId = Long.parseLong(connectorIdentifier);
                for (ApiKey cur : apiKeys) {
                    if (cur.getId() == apiKeyId) {
                        apiKey = cur;
                        break;
                    }
                }
            }
            else{
                for (ApiKey cur : apiKeys) {
                    if (cur.getConnector().getName().equals(connectorIdentifier)) {
                        apiKey = cur;
                        break;
                    }
                }
            }
            if (apiKey != null){
                ObjectType objectType = null;
                for (ObjectType objType : apiKey.getConnector().objectTypes()){
                    if (objType.getName().equals(objectTypeName)){
                        objectType = objType;
                    }
                }
                if (objectType != null){
                    result.get(apiKey).add(objectType);
                }
            }
        }
        for (ApiKey apiKey : apiKeys){
            List<ObjectType> list = result.get(apiKey);
            if (list.size() == 0){
                Collections.addAll(list, apiKey.getConnector().objectTypes());
            }
        }
        return result;

    }

    /**
     *
     * @param date  - the date being queried
     * @param connectorObjectsEncoded - this is an encoded list of the facet types to be returned the encoded is:
     *                                <facetType>,<facetType>,...
     *                                where <facetType> is:
     *                                <connectorIdentifier>(optionally attached: -<objectTypeName>
     *                                where <connectorIdentifier> is either the connector name or the apiKey id
     *                                and objectTypeName is the name of the facet Type
     *                                example:
     *                                64-weight,fitbit,withings-heart_pulse
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("rawtypes")
	@GET
	@Path("/{connectorObjectsEncoded}/date/{date}")
    @ApiOperation(value = "Get data from a specific connector at a specific date", response = ConnectorResponseModel.class)
	@Produces({ MediaType.APPLICATION_JSON })
	public String getConnectorData(@ApiParam(value="Date", required=true) @PathParam("date") String date,
                                   @ApiParam(value="an encoded list of the facet types to be returned. " +
                                                   "The encoding is <facetType>,<facetType>,... where <facetType> is " +
                                                   "<connectorIdentifier>(optionally attached: -<objectTypeName> " +
                                                   "where <connectorIdentifier> is either the connector name or the apiKey id " +
                                                   "and objectTypeName is the name of the facet Type - example: 64-weight,fitbit,withings-heart_pulse",
                                           required=true) @PathParam("connectorObjectsEncoded") String connectorObjectsEncoded)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
        try{
            Guest guest;
            long guestId;

            try {
                guest = AuthHelper.getGuest();
                guestId = guest.getId();
            } catch (Throwable e) {
                return gson.toJson(new StatusModel(false, "You are no longer logged in. Please reload your browser window"));
            }
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

            List<ApiKey> apiKeyList = getApiKeyListFromConnectorObjectsEncoding(guest,connectorObjectsEncoded);
            Map<ApiKey,List<ObjectType>> objectTypesMap = getObjectTypesFromConnectorObjectsEncoding(apiKeyList,connectorObjectsEncoded);

            long then = System.currentTimeMillis();


            DayMetadata dayMetadata = metadataService.getDayMetadata(guestId, date);
            GuestSettings settings = settingsService.getSettings(guestId);
            ConnectorResponseModel day = prepareConnectorResponseModel(dayMetadata);

            for (ApiKey apiKey : apiKeyList){
                List<ObjectType> objectTypes = objectTypesMap.get(apiKey);
                if (objectTypes.size() > 0) {
                    for (ObjectType objectType : objectTypes) {
                        appendFacetsToConnectorResponseModel(day,getFacetCollection(dayMetadata, settings, apiKey.getConnector(), objectType),apiKey.getConnector(),objectType);
                    }
                }
                else {
                    appendFacetsToConnectorResponseModel(day,getFacetCollection(dayMetadata,settings,apiKey.getConnector(),null),apiKey.getConnector(),null);
                }
            }



            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getConnectorData")
                    .append(" date=").append(date)
                    .append(" connectorObjectsEncoded=").append(connectorObjectsEncoded)
                    .append(" timeTaken=").append(System.currentTimeMillis()-then)
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());

            String json = toJacksonJson(day);
            return json;
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to get digest: " + e.getMessage()));
        }
	}

    @SuppressWarnings("rawtypes")
    @GET
    @Path("/{connectorObjectsEncoded}/week/{year}/{week}")
    @ApiOperation(value = "Get data from a specific connector for a specific week", response = ConnectorResponseModel.class)
    @Produces({ MediaType.APPLICATION_JSON })
    public String getConnectorData(@ApiParam(value="Year", required=true) @PathParam("year") final int year,
                                   @ApiParam(value="Week", required=true) @PathParam("week") final int week,
                                   @ApiParam(value="an encoded list of the facet types to be returned. " +
                                                   "The encoding is <facetType>,<facetType>,... where <facetType> is " +
                                                   "<connectorIdentifier>(optionally attached: -<objectTypeName> " +
                                                   "where <connectorIdentifier> is either the connector name or the apiKey id " +
                                                   "and objectTypeName is the name of the facet Type - example: 64-weight,fitbit,withings-heart_pulse",
                                             required=true) @PathParam("connectorObjectsEncoded") String connectorObjectsEncoded)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        try{
            Guest guest;
            long guestId;

            try {
                guest = AuthHelper.getGuest();
                guestId = guest.getId();
            } catch (Throwable e) {
                return gson.toJson(new StatusModel(false, "You are no longer logged in. Please reload your browser window"));
            }
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

            List<ApiKey> apiKeyList = getApiKeyListFromConnectorObjectsEncoding(guest,connectorObjectsEncoded);
            Map<ApiKey,List<ObjectType>> objectTypesMap = getObjectTypesFromConnectorObjectsEncoding(apiKeyList,connectorObjectsEncoded);

            long then = System.currentTimeMillis();

            WeekMetadata weekMetadata = metadataService.getWeekMetadata(guestId, year, week);
            GuestSettings settings = settingsService.getSettings(guestId);
            ConnectorResponseModel day = prepareConnectorResponseModel(weekMetadata);

            for (ApiKey apiKey : apiKeyList){
                List<ObjectType> objectTypes = objectTypesMap.get(apiKey);
                if (objectTypes.size() > 0) {
                    for (ObjectType objectType : objectTypes) {
                        appendFacetsToConnectorResponseModel(day,getFacetCollection(weekMetadata, settings, apiKey.getConnector(), objectType),apiKey.getConnector(),objectType);
                    }
                }
                else {
                    appendFacetsToConnectorResponseModel(day,getFacetCollection(weekMetadata,settings,apiKey.getConnector(),null),apiKey.getConnector(),null);
                }
            }
            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getConnectorData")
                    .append(" year=").append(year)
                    .append(" week=").append(week)
                    .append(" connectorObjectsEncoded=").append(connectorObjectsEncoded)
                    .append(" timeTaken=").append(System.currentTimeMillis()-then)
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());

            String json = toJacksonJson(day);
            return json;
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to get digest: " + e.getMessage()));
        }
    }

    @SuppressWarnings("rawtypes")
    @GET
    @ApiOperation(value = "Get data from a specific connector for a specific month", response = ConnectorResponseModel.class)
    @Path("/{connectorObjectsEncoded}/month/{year}/{month}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getConnectorDataMonth(@ApiParam(value="Year", required=true) @PathParam("year") final int year,
                                        @ApiParam(value="Month", required=true) @PathParam("month") final int month,
                                        @ApiParam(value="an encoded list of the facet types to be returned. " +
                                                        "The encoding is <facetType>,<facetType>,... where <facetType> is " +
                                                        "<connectorIdentifier>(optionally attached: -<objectTypeName> " +
                                                        "where <connectorIdentifier> is either the connector name or the apiKey id " +
                                                        "and objectTypeName is the name of the facet Type - example: 64-weight,fitbit,withings-heart_pulse",
                                                  required=true) @PathParam("connectorObjectsEncoded") String connectorObjectsEncoded)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        try{
            Guest guest;
            long guestId;

            try {
                guest = AuthHelper.getGuest();
                guestId = guest.getId();
            } catch (Throwable e) {
                return gson.toJson(new StatusModel(false, "You are no longer logged in. Please reload your browser window"));
            }
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

            List<ApiKey> apiKeyList = getApiKeyListFromConnectorObjectsEncoding(guest,connectorObjectsEncoded);
            Map<ApiKey,List<ObjectType>> objectTypesMap = getObjectTypesFromConnectorObjectsEncoding(apiKeyList,connectorObjectsEncoded);

            long then = System.currentTimeMillis();

            MonthMetadata monthMetadata = metadataService.getMonthMetadata(guestId, year, month);
            GuestSettings settings = settingsService.getSettings(guestId);
            ConnectorResponseModel day = prepareConnectorResponseModel(monthMetadata);

            for (ApiKey apiKey : apiKeyList){
                List<ObjectType> objectTypes = objectTypesMap.get(apiKey);
                if (objectTypes.size() > 0) {
                    for (ObjectType objectType : objectTypes) {
                        appendFacetsToConnectorResponseModel(day,getFacetCollection(monthMetadata, settings, apiKey.getConnector(), objectType),apiKey.getConnector(),objectType);
                    }
                }
                else {
                    appendFacetsToConnectorResponseModel(day,getFacetCollection(monthMetadata,settings,apiKey.getConnector(),null),apiKey.getConnector(),null);
                }
            }

            StringBuilder sb = new StringBuilder("module=API component=calendarDataStore action=getConnectorData")
                    .append(" year=").append(year)
                    .append(" connectorObjectsEncoded=").append(month)
                    .append(" connector=").append(connectorObjectsEncoded)
                    .append(" timeTaken=").append(System.currentTimeMillis()-then)
                    .append(" guestId=").append(guestId);
            logger.info(sb.toString());

            String json = toJacksonJson(day);
            return json;
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to get digest: " + e.getMessage()));
        }
    }

	private ConnectorResponseModel prepareConnectorResponseModel(
			AbstractTimespanMetadata metadata) {
		TimeBoundariesModel tb = calendarDataHelper
				.getStartEndResponseBoundaries(metadata);
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
                    Collection<AbstractFacetVO<AbstractFacet>> facetCollection = getFacetCollection(timespanMetadata,settings,connector,objectType);
                    setFilterInfo(digest, apiKeySelection, apiKey,
                                  connector, objectType, facetCollection);
                }
            }
            else {
                Collection<AbstractFacetVO<AbstractFacet>> facetCollection = getFacetCollection(timespanMetadata, settings,
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
        if (!apiKeySelection.contains(apiKey)) {
            return;
        }
		if (facetCollection.size() > 0) {
			StringBuilder sb = new StringBuilder(connector.getName());
            if (objectType != null) {
                sb.append("-").append(objectType.getName());
            }
			digest.facets.put(sb.toString(), facetCollection);
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
                        facetVO.setStart(startTime.getMillis());
                        facetVO.setEnd(startTime.getMillis() + DateTimeConstants.MILLIS_PER_DAY);
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
		digest.addresses = new HashMap<String,List<AddressModel>>();
        for (GuestAddress address : addresses){
            List collection = digest.addresses.get(address.type);
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
                model.apiKeyId = apiKey.getId();
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
