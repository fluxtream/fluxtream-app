package com.fluxtream.updaters.strategies;

import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Autowired;

import com.fluxtream.Configuration;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.ConnectorUpdateService;

abstract class AbstractUpdateStrategy implements UpdateStrategy {

	protected final TimeZone UTC = TimeZone.getTimeZone("UTC");
	
	@Autowired
	Configuration env;
	
	@Autowired
	ConnectorUpdateService connectorUpdateService;
	
	/**
	 * We know that a guest's connector data need history update if we
	 * cannot find the trace of a successfull UpdateWorkerTask in our records.
	 * We must also check that a scheduled update is not ongoing
	 */
	final private boolean needsHistoryUpdate(ApiKey apiKey, int objectTypes) {
		boolean historyComplete = connectorUpdateService.isHistoryUpdateCompleted(apiKey.getGuestId(), apiKey.getConnector().getName(), objectTypes);
		if (!historyComplete) {
			boolean isAlreadyScheduled = connectorUpdateService.isUpdateScheduled(apiKey.getGuestId(), apiKey.getConnector().getName(), UpdateInfo.UpdateType.INITIAL_HISTORY_UPDATE, objectTypes);
			return !isAlreadyScheduled;
		}
		return false;
	}
	
	@Override
	final public UpdateInfo getUpdateInfo(ApiKey apiKey, int objectTypes,
			TimeInterval timeInterval) {
		if (needsHistoryUpdate(apiKey, objectTypes))
			return UpdateInfo.initialHistoryUpdateInfo(apiKey, objectTypes);
		else
			return computeUpdateInfo(apiKey, objectTypes, timeInterval);
	}
	
	abstract UpdateInfo computeUpdateInfo(ApiKey apiKey, int objetTypes,
			TimeInterval timeInterval);
	

}
