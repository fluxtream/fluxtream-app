package com.fluxtream.api;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.ScheduleResult;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.connectors.updaters.UpdateResult;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.ConnectorResponseModel;
import com.fluxtream.mvc.models.HomeModel;
import com.fluxtream.mvc.models.TimeBoundariesModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.updaters.strategies.UpdateStrategy;
import com.fluxtream.updaters.strategies.UpdateStrategyFactory;

@Component
public class CalendarHelper {

	@Autowired
	private ApiDataService apiDataService;

	@Autowired
	private ConnectorUpdateService connectorUpdateService;

	@Autowired
	private UpdateStrategyFactory updateStrategyFactory;

	/**
	 * This is to let the client discard responses that are coming "too late"
	 * 
	 */
	TimeBoundariesModel getStartEndResponseBoundaries(DayMetadataFacet dayMetadata) {
		TimeBoundariesModel tb = new TimeBoundariesModel();
		tb.start = dayMetadata.start;
		tb.end = dayMetadata.end;
		return tb;
	}

	boolean isToday(HttpServletRequest request) {
		HomeModel homeModel = getHomeModel(request);
		return homeModel.isToday();
	}

	void removeGoogleLatitude(long guestId, List<ApiKey> userKeys) {
		for (ApiKey apiKey : userKeys) {
			if (apiKey.getConnector().getName().equals("google_latitude")) {
				userKeys.remove(apiKey);
				return;
			}
		}
	}

	static void setJsonCacheHeaders(HttpServletResponse response,
			int cacheMillis) {
		response.setContentType("application/json; charset=utf-8");
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MILLISECOND, cacheMillis);
		DateTimeFormatter format = DateTimeFormat
				.forPattern("EEE, d MMM yyyy HH:mm:ss Z");
		response.setHeader("Expires", format.print(c.getTimeInMillis()));
	}

	public List<AbstractFacet> getFacets(Connector connector,
			ObjectType objectType, DayMetadataFacet dayMetadata,
			int lookbackDays) {
		List<AbstractFacet> facets = null;
		try {
			facets = apiDataService.getApiDataFacets(
					ControllerHelper.getGuestId(), connector, objectType,
					dayMetadata.getTimeInterval());
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return facets;
	}

    public List<AbstractFacet> getFacets(Connector connector,
                                         ObjectType objectType,
                                         TimeInterval timeInterval) {
        List<AbstractFacet> facets = null;
        try {
            facets = apiDataService.getApiDataFacets(
                    ControllerHelper.getGuestId(), connector, objectType,
                    timeInterval);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return facets;
    }

    public List<AbstractFacet> getFacets(DayMetadataFacet dayMetadata,
			Connector connector, boolean lookback) {
		ObjectType[] objectTypes = connector.objectTypes();
		if (objectTypes == null)
			return getFacets(connector, null, dayMetadata,
					getLookbackDays(connector, null));
		List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
		for (ObjectType objectType : objectTypes) {
			List<AbstractFacet> objectTypeFacets = getFacets(connector,
					objectType, dayMetadata,
					lookback ? getLookbackDays(connector, objectType) : 0);
			if (objectTypeFacets != null)
				facets.addAll(objectTypeFacets);
		}
		return facets;
	}

	private int getLookbackDays(Connector connector, ObjectType objectType) {
		if (objectType != null) {
			if (connector.getName().equals("withings")
					&& objectType.getName().equals("weight"))
				return 14; // TODO: strange results when this number is bigger
							// (e.g. 25 or 30) -> understand why!
		}
		return 0;
	}

	void refreshApiData(DayMetadataFacet dayMetadata, ApiKey apiKey,
			ObjectType objectType, ConnectorResponseModel crm) {
		// if objectType is not specified and the connector has multiple object
		// types
		// then we update all object types for this connector
		TimeInterval interval = dayMetadata.getTimeInterval();
		int[] objectTypeValues = apiKey.getConnector().objectTypeValues();
		if (objectType == null && objectTypeValues != null
				&& objectTypeValues.length > 0) {
			for (int i = 0; i < objectTypeValues.length; i++) {
				updateObjectTypeData(apiKey, objectTypeValues[i], crm, interval);
			}
		} else {
			updateObjectTypeData(apiKey, -1, crm, interval);
		}
	}

	public void updateObjectTypeData(ApiKey apiKey, int objectTypes,
			ConnectorResponseModel crm, TimeInterval interval) {
		UpdateStrategy updateStrategy = updateStrategyFactory
				.getUpdateStrategy(apiKey.getConnector());
		UpdateInfo updateInfo = updateStrategy.getUpdateInfo(apiKey,
				objectTypes, interval);
		// either we need to initiate a history update
		if (updateInfo.getUpdateType() == UpdateInfo.UpdateType.INITIAL_HISTORY_UPDATE) {
			ScheduleResult scheduleResult = connectorUpdateService
					.scheduleUpdate(apiKey.getGuestId(), apiKey.getConnector()
							.getName(), objectTypes,
							UpdateInfo.UpdateType.INITIAL_HISTORY_UPDATE,
							System.currentTimeMillis());
			crm.addScheduleResult(scheduleResult);
		}
		// or, unless we were told not to (NOOP), we update the api data
		else if (updateInfo.getUpdateType() != UpdateInfo.UpdateType.NOOP_UPDATE) {
			UpdateResult updateResult = doUpdateApiData(apiKey, objectTypes,
					updateInfo);
			crm.addUpdateResult(updateResult);
		}
	}

	UpdateResult doUpdateApiData(ApiKey apiKey, int objectTypes,
			UpdateInfo updateInfo) {
		AbstractUpdater updater = connectorUpdateService.getUpdater(apiKey
				.getConnector());
		UpdateResult updateResult = updater.updateData(updateInfo);
		return updateResult;
	}

	HomeModel getHomeModel(HttpServletRequest request) {
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		return homeModel;
	}
}
