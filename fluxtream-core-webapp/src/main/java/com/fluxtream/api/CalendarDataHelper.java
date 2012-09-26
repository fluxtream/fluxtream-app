package com.fluxtream.api;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
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
import com.fluxtream.mvc.controllers.AuthHelper;
import com.fluxtream.mvc.models.CalendarModel;
import com.fluxtream.mvc.models.ConnectorResponseModel;
import com.fluxtream.mvc.models.TimeBoundariesModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.updaters.strategies.UpdateStrategy;
import com.fluxtream.updaters.strategies.UpdateStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CalendarDataHelper {

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

	void removeGoogleLatitude(long guestId, List<ApiKey> userKeys) {
		for (ApiKey apiKey : userKeys) {
			if (apiKey.getConnector().getName().equals("google_latitude")) {
				userKeys.remove(apiKey);
				return;
			}
		}
	}

    public List<AbstractFacet> getFacets(Connector connector,
                                         ObjectType objectType,
                                         List<String> dates) {
        List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        try {
            if (AuthHelper.isViewingGranted(connector.getName()))
                facets = apiDataService.getApiDataFacets(
                        AuthHelper.getVieweeId(), connector, objectType,
                        dates);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return facets;
    }

	public List<AbstractFacet> getFacets(Connector connector,
			ObjectType objectType, DayMetadataFacet dayMetadata,
			int lookbackDays) {
		List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
		try {
            if (AuthHelper.isViewingGranted(connector.getName()))
                facets = apiDataService.getApiDataFacets(
                        AuthHelper.getVieweeId(), connector, objectType,
                        dayMetadata.getTimeInterval());
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return facets;
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

	CalendarModel getHomeModel(HttpServletRequest request) {
		CalendarModel calendarModel = (CalendarModel) request.getSession().getAttribute(
				"calendarModel");
		return calendarModel;
	}
}
