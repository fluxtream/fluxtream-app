package com.fluxtream.services.impl;

import static com.fluxtream.utils.Utils.stackTrace;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.connectors.updaters.UpdateResult;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.ScheduledUpdate;
import com.fluxtream.domain.ScheduledUpdate.Status;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;

@Component
@Scope("prototype")
class UpdaterTask implements Runnable {

	Logger logger = Logger.getLogger(UpdaterTask.class);

	@Autowired
	ConnectorUpdateService connectorUpdateService;

	@Autowired
	ApiDataService apiDataService;

	@Autowired
	GuestService guestService;

	@Autowired
	Configuration env;

	ScheduledUpdate su;

	public UpdaterTask() {
	}

	@Override
	public void run() {
		StringBuilder sb = new StringBuilder();
		sb.append("guestId=");
		sb.append(su.getGuestId());
		sb.append(" action=bg_update stage=start");
		sb.append(" connectorName=");
		sb.append(su.connectorName);
		sb.append(" objectType=");
		sb.append(su.objectTypes);
		logger.info(sb.toString());
		Connector connector = Connector.getConnector(su.connectorName);
		ApiKey apiKey = guestService.getApiKey(su.guestId, connector);
		AbstractUpdater updater = connectorUpdateService.getUpdater(connector);
		
		switch (su.updateType) {
		case INITIAL_HISTORY_UPDATE:
			updateDataHistory(connector, apiKey, updater);
			break;
		case PUSH_TRIGGERED_UPDATE:
			pushTriggeredUpdate(connector, apiKey, updater);
			break;
		}
	}

	private void pushTriggeredUpdate(Connector connector, ApiKey apiKey,
			AbstractUpdater updater) {
		try {
			UpdateInfo updateInfo = UpdateInfo.pushTriggeredUpdateInfo(apiKey,
					su.objectTypes, su.jsonParams);
			UpdateResult updateResult = updater.updateData(updateInfo);
			handleUpdateResult(connector, updateResult);
		} catch (Throwable e) {
			String stackTrace = stackTrace(e);
			logger.warn("guestId=" + su.guestId + " action=bg_update type=initialHistory "
					+ "stage=unexpected_exception connector="
					+ su.connectorName + " objectType=" + su.objectTypes);
			retry(connector, stackTrace);
		}
	}

	private void updateDataHistory(Connector connector, ApiKey apiKey,
			AbstractUpdater updater) {
		try {
			UpdateInfo updateInfo = UpdateInfo.initialHistoryUpdateInfo(apiKey,
					su.objectTypes);
			UpdateResult updateResult = updater.updateDataHistory(updateInfo);
			handleUpdateResult(connector, updateResult);
		} catch (Throwable e) {
			String stackTrace = stackTrace(e);
			logger.warn("guestId=" + su.guestId + " action=bg_update type=initialHistory "
					+ "stage=unexpected_exception connector="
					+ su.connectorName + " objectType=" + su.objectTypes);
			retry(connector, stackTrace);
		}
	}

	private void handleUpdateResult(Connector connector,
			UpdateResult updateResult) {
		switch (updateResult.type) {
		case DUPLICATE_UPDATE:
			warn();
			break;
		case HAS_REACHED_RATE_LIMIT:
			longReschedule(connector);
			break;
		case UPDATE_SUCCEEDED:
			success();
			break;
		case UPDATE_FAILED:
			retry(connector, updateResult.stackTrace);
			break;
		case NO_RESULT:
			abort();
			break;
		}
	}

	private void warn() {
		logger.warn("guestId=" + su.getGuestId() + " action=bg_update stage=updating" +
				" connectorName=" + su.connectorName + " objectType="
						+ su.objectTypes + " reason=duplicate_update");
	}

	private void success() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("guestId=");
		stringBuilder.append(su.getGuestId());
		stringBuilder.append(" action=bg_update stage=success");
		stringBuilder.append(" connectorName=");
		stringBuilder.append(su.connectorName);
		stringBuilder.append(" objectType=");
		stringBuilder.append(su.objectTypes);
		logger.info(stringBuilder.toString());
		connectorUpdateService.setScheduledUpdateStatus(su.getId(),
				ScheduledUpdate.Status.DONE);
	}

	private void abort() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("guestId=");
		stringBuilder.append(su.getGuestId());
		stringBuilder.append(" action=bg_update stage=no_result");
		stringBuilder.append(" connectorName=");
		stringBuilder.append(su.connectorName);
		stringBuilder.append(" objectType=");
		stringBuilder.append(su.objectTypes);
		logger.info(stringBuilder.toString());
		connectorUpdateService.setScheduledUpdateStatus(su.getId(),
				Status.FAILED);
	}

	private void retry(Connector connector, String stackTrace) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("guestId=");
		stringBuilder.append(su.getGuestId());
		stringBuilder.append(" action=bg_update stage=retry");
		stringBuilder.append(" connectorName=");
		stringBuilder.append(su.connectorName);
		stringBuilder.append(" objectType=");
		stringBuilder.append(su.objectTypes);
		stringBuilder.append("\n");
		stringBuilder.append(stackTrace);
		logger.info(stringBuilder.toString());
		int maxRetries = 0;
		try {
			maxRetries = getMaxRetries(connector);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		if (su.retries < maxRetries) {
			shortReschedule(connector);
		} else {
			longReschedule(connector);
		}
	}

	private void longReschedule(Connector connector) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("guestId=");
		stringBuilder.append(su.getGuestId());
		stringBuilder.append(" action=bg_update stage=failed");
		stringBuilder.append(" connectorName=");
		stringBuilder.append(su.connectorName);
		stringBuilder.append("objectType=");
		stringBuilder.append(su.objectTypes);
		logger.info(stringBuilder.toString());
		// re-schedule when we are below rate limit again
		connectorUpdateService.reScheduleUpdate(su, System.currentTimeMillis()
				+ getLongRetryDelay(connector), false);
	}

	private void shortReschedule(Connector connector) {
		StringBuilder sb = new StringBuilder();
		sb.append("guestId=");
		sb.append(su.getGuestId());
		sb.append(" action=bg_update");
		sb.append("stage=increment_retries connectorName=");
		sb.append(su.connectorName);
		sb.append(" objectType=");
		sb.append(su.objectTypes);
		sb.append(" retries=");
		sb.append(String.valueOf(su.retries));
		logger.info(sb.toString());
		// schedule 1 minute later, typically
		connectorUpdateService.reScheduleUpdate(su, System.currentTimeMillis()
				+ getShortRetryDelay(connector), true);
	}

	private int getMaxRetries(Connector connector) {
		String key = connector.getName() + ".maxRetries";
		if (env.connectors.containsKey(key)) {
			int retries = Integer.valueOf(env.connectors.get(key));
			return retries;
		} else
			return Integer.valueOf(env.connectors.get("maxRetries"));
	}

	private int getShortRetryDelay(Connector connector) {
		String key = connector.getName() + ".shortRetryDelay";
		if (env.connectors.containsKey(key)) {
			int delay = Integer.valueOf(env.connectors.get(key));
			return delay;
		} else
			return Integer.valueOf(env.connectors.get("shortRetryDelay"));
	}

	private int getLongRetryDelay(Connector connector) {
		String key = connector.getName() + ".longRetryDelay";
		if (env.connectors.containsKey(key)) {
			Integer delay = Integer.valueOf(env.connectors.get(key));
			return delay;
		} else
			return Integer.valueOf(env.connectors.get("longRetryDelay"));
	}

}
