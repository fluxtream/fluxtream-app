package com.fluxtream.api;

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

import com.fluxtream.TimeUnit;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.models.AddressModel;
import com.fluxtream.mvc.models.ConnectorDataModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.connectors.vos.ImageVOCollection;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.GuestAddress;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.domain.Notification;
import com.fluxtream.domain.metadata.City;
import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.domain.metadata.DayMetadataFacet.VisitedCity;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.ConnectorResponseModel;
import com.fluxtream.mvc.models.DigestModel;
import com.fluxtream.mvc.models.NotificationModel;
import com.fluxtream.mvc.models.SettingsModel;
import com.fluxtream.mvc.models.SolarInfoModel;
import com.fluxtream.mvc.models.TimeBoundariesModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.services.NotificationsService;
import com.fluxtream.services.SettingsService;
import com.fluxtream.updaters.strategies.UpdateStrategy;
import com.fluxtream.updaters.strategies.UpdateStrategyFactory;
import com.google.gson.Gson;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

@Path("/calendar")
@Component("calendarApi")
@Scope("request")
public class CalendarResource {

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
    CalendarHelper calendarHelper;

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
	public String getAllConnectorsWeekData(@PathParam("year") String year,
			@PathParam("week") String week, @QueryParam("filter") String filter)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
        return "{}";
	}

	@GET
	@Path("/all/month/{year}/{month}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getAllConnectorsMonthData(@PathParam("year") String year,
			@PathParam("month") String month,
			@QueryParam("filter") String filter) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
		return "{}";
	}

	@GET
	@Path("/all/year/{year}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getAllConnectorsYearData(@PathParam("year") String year,
			@QueryParam("filter") String filter) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
		return "{}";
	}

	@GET
	@Path("/all/date/{date}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getAllConnectorsDayData(@PathParam("date") String date,
			@QueryParam("filter") String filter) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
		DigestModel digest = new DigestModel();
        if (filter == null) {
            filter = "";
        }

        Guest guest = ControllerHelper.getGuest();

		long guestId = guest.getId();

		DayMetadataFacet dayMetadata = metadataService.getDayMetadata(guestId,
				date, true);
		digest.tbounds = getStartEndResponseBoundaries(dayMetadata.start,
				dayMetadata.end);

		City city = metadataService.getMainCity(guestId, dayMetadata);

        if (city != null){
            digest.hourlyWeatherData = metadataService.getWeatherInfo(city.geo_latitude,city.geo_longitude, date, 0, 24 * 60);
            Collections.sort(digest.hourlyWeatherData);
        }

		setSolarInfo(digest, city, guestId, dayMetadata);

		List<ApiKey> apiKeySelection = getApiKeySelection(guestId, filter);
		digest.selectedConnectors = connectorNames(apiKeySelection);
		List<ApiKey> allApiKeys = guestService.getApiKeys(guestId);
		allApiKeys = removeConnectorsWithoutFacets(allApiKeys);
		digest.nApis = allApiKeys.size();
		GuestSettings settings = settingsService.getSettings(guestId);

		setCachedData(digest, allApiKeys, settings, apiKeySelection,
				dayMetadata);

		copyMetadata(digest, dayMetadata);
		setVisitedCities(digest, guestId, dayMetadata);
		setNotifications(digest, guestId);
		setCurrentAddress(digest, guestId, dayMetadata.start);
		digest.settings = new SettingsModel(settings);

		// NewRelic.setTransactionName(null, "/api/log/all/date");
		return gson.toJson(digest);
	}

	private List<ApiKey> removeConnectorsWithoutFacets(List<ApiKey> allApiKeys) {
		List<ApiKey> apiKeys = new ArrayList<ApiKey>();
		for (ApiKey apiKey : allApiKeys) {
            if (apiKey.getConnector().hasFacets()) {
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
		Connector connector = Connector.getConnector(connectorName);

		long guestId = ControllerHelper.getGuestId();
		DayMetadataFacet dayMetadata = metadataService.getDayMetadata(guestId,
				date, true);
		GuestSettings settings = settingsService.getSettings(guestId);
		ConnectorResponseModel day = prepareConnectorResponseModel(dayMetadata);
		ObjectType[] objectTypes = connector.objectTypes();
		ApiKey apiKey = guestService.getApiKey(ControllerHelper.getGuestId(),
				connector);
		calendarHelper.refreshApiData(dayMetadata, apiKey, null, day);
        if (objectTypes != null) {
            for (ObjectType objectType : objectTypes) {
                Collection facetCollection = getFacetVos(dayMetadata, settings,
                                                         connector, objectType);
                if (facetCollection.size() > 0) {
                    day.payload = facetCollection;
                }
            }
        }
        else {
            Collection facetCollection = getFacetVos(dayMetadata, settings,
                                                     connector, null);
            day.payload = facetCollection;
        }

		String json = gson.toJson(day);
		// NewRelic.setTransactionName(null, "/api/log/" + connectorName +
		// "/date");
		return json;
	}

	private ConnectorResponseModel prepareConnectorResponseModel(
			DayMetadataFacet dayMetadata) {
		TimeBoundariesModel tb = calendarHelper
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
                    Collection facetCollection = getFacetVos(dayMetadata,
                                                             settings, connector, objectType);
                    setFilterInfo(dayMetadata, digest, apiKeySelection, apiKey,
                                  connector, objectType, facetCollection);
                }
            }
            else {
                Collection facetCollection = getFacetVos(dayMetadata, settings,
                                                         connector, null);
                setFilterInfo(dayMetadata, digest, apiKeySelection, apiKey,
                              connector, null, facetCollection);
            }
		}
	}

	@SuppressWarnings("rawtypes")
	private void setFilterInfo(DayMetadataFacet dayMetadata,
			DigestModel digest, List<ApiKey> apiKeySelection, ApiKey apiKey,
			Connector connector, ObjectType objectType,
			Collection facetCollection) {
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
	private Collection getFacetVos(DayMetadataFacet dayMetadata,
			GuestSettings settings, Connector connector, ObjectType objectType)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {
        List<AbstractFacet> objectTypeFacets = calendarHelper.getFacets(
				connector,
				objectType,
				dayMetadata,
				getLookbackDays(connector.getName(), objectType == null ? null
                                                                        : objectType.getName()));
		Collection facetCollection = new ArrayList();
		if (objectTypeFacets != null) {
			for (AbstractFacet abstractFacet : objectTypeFacets) {
				AbstractFacetVO<AbstractFacet> facetVO = AbstractFacetVO
						.getFacetVOClass(abstractFacet).newInstance();
				facetVO.extractValues(abstractFacet,
						dayMetadata.getTimeInterval(), settings);
				facetCollection.add(facetVO);
			}
		}
		return facetCollection;
	}

    @GET
    @Path("/days/{start}/{end}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getDailyConnectorData(@QueryParam("connectorNames") String connectorNames,
                                        @QueryParam("objectTypes") String objectTypes,
                                        @PathParam("start") long start,
                                        @PathParam("end") long end)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        long guestId = ControllerHelper.getGuestId();
        GuestSettings settings = settingsService.getSettings(guestId);
        TimeInterval timeInterval = new TimeInterval(start, end, TimeUnit.DAY, TimeZone.getTimeZone("UTC"));
        String[] names = connectorNames.split(",");
        String[] types = objectTypes.split(",");
        if (names.length!=types.length) {
            throw new RuntimeException("You need to supply the exact same number of connector names" +
                                       "and objectType names");
        }
        List<ConnectorDataModel> dataModels = new ArrayList<ConnectorDataModel>();
        for (int i=0; i<names.length; i++) {
            Connector connector = Connector.getConnector(names[i]);
            ObjectType objectType = ObjectType.getObjectType(connector, types[i]);
            List<AbstractFacet> objectTypeFacets = calendarHelper.getFacets(
                    connector,
                    objectType,
                    timeInterval);
            Collection facetCollection = new ArrayList();
            if (objectTypeFacets != null) {
                for (AbstractFacet abstractFacet : objectTypeFacets) {
                    AbstractFacetVO<AbstractFacet> facetVO = AbstractFacetVO
                            .getFacetVOClass(abstractFacet).newInstance();
                    facetVO.extractValues(abstractFacet,
                                          timeInterval, settings);
                    facetCollection.add(facetVO);
                }
            }
            ConnectorDataModel dataModel = new ConnectorDataModel();
            dataModel.connector = names[i];
            dataModel.objectType = types[i];
            dataModel.facetVos = facetCollection;
            dataModels.add(dataModel);
        }

        return gson.toJson(dataModels);
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

	private List<String> connectorNames(List<ApiKey> apis) {
		List<String> connectorNames = new ArrayList<String>();
        for (ApiKey apiKey : apis) {
            connectorNames.add(apiKey.getConnector().getName());
        }
		return connectorNames;
	}

	private List<ApiKey> getApiKeySelection(long guestId, String filter) {
		List<ApiKey> userKeys = guestService.getApiKeys(guestId);
		String[] uncheckedConnectors = filter.split(",");
		List<String> filteredOutConnectors = new ArrayList<String>();
        if (uncheckedConnectors != null && uncheckedConnectors.length > 0) {
            filteredOutConnectors = new ArrayList<String>(
                    Arrays.asList(uncheckedConnectors));
        }
		List<ApiKey> apiKeySelection = getCheckedApiKeys(userKeys,
				filteredOutConnectors);
		return apiKeySelection;
	}

	private List<ApiKey> getCheckedApiKeys(List<ApiKey> apiKeys,
			List<String> uncheckedConnectors) {
		List<ApiKey> result = new ArrayList<ApiKey>();
		there: for (ApiKey apiKey : apiKeys) {
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
		if (sunrise != null && sunset != null) {
			solarInfo.sunrise = AbstractFacetVO.toMinuteOfDay(
					sunrise.getTime(), timeZone);
			solarInfo.sunset = AbstractFacetVO.toMinuteOfDay(sunset.getTime(),
					timeZone);
		}
		return solarInfo;
	}

	TimeBoundariesModel getStartEndResponseBoundaries(long start, long end) {
		TimeBoundariesModel tb = new TimeBoundariesModel();
		tb.start = start;
		tb.end = end;
		return tb;
	}
}
