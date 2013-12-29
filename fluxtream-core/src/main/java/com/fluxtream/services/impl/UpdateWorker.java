package com.fluxtream.services.impl;

import java.util.Date;
import com.fluxtream.Configuration;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.connectors.updaters.UpdateResult;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.ConnectorInfo;
import com.fluxtream.domain.Notification;
import com.fluxtream.domain.UpdateWorkerTask;
import com.fluxtream.domain.UpdateWorkerTask.Status;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.NotificationsService;
import com.fluxtream.services.SystemService;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.fluxtream.utils.Utils.stackTrace;

@Component
@Scope("prototype")
class UpdateWorker implements Runnable {

	FlxLogger logger = FlxLogger.getLogger(UpdateWorker.class);

    @Qualifier("connectorUpdateServiceImpl")
    @Autowired
	ConnectorUpdateService connectorUpdateService;

    @Qualifier("apiDataServiceImpl")
    @Autowired
	ApiDataService apiDataService;

	@Autowired
	GuestService guestService;

    @Autowired
    SystemService systemService;

    @Autowired
    NotificationsService notificationsService;

	@Autowired
	Configuration env;

	UpdateWorkerTask task;

	public UpdateWorker() {
	}

    @Trace(dispatcher=true)
	@Override
	public void run() {
        logNR();
        StringBuilder sb = new StringBuilder("module=updateQueue component=worker action=start")
                .append(" guestId=").append(task.getGuestId())
                .append(" connector=").append(task.connectorName)
                .append(" objectType=").append(task.objectTypes)
                .append(" apiKeyId=").append(task.apiKeyId);
		logger.info(sb.toString());

		ApiKey apiKey = guestService.getApiKey(task.apiKeyId);
        Connector conn = apiKey.getConnector();

        // Check if this connector type is enabled and supportsSync before calling update.
        // If it is disabled and/or does not support sync, don't try to update it.
        boolean doUpdate = true;

        if (conn!=null) {
            try {
                final ConnectorInfo connectorInfo = systemService.getConnectorInfo(apiKey.getConnector().getName());
                // Make sure that this connector type supports sync and is enabled in this Fluxtream instance
                if (!connectorInfo.supportsSync || !connectorInfo.enabled) {
                    doUpdate = false;
                }
            }
            catch (Throwable e) {
                // Skip this connector
                doUpdate = false;
            }
        }
        else {
            doUpdate=false;
        }

        if(doUpdate) {
            AbstractUpdater updater = connectorUpdateService.getUpdater(conn);
            guestService.setApiKeyToSynching(apiKey.getId(), true);
            switch (task.updateType) {
                case INITIAL_HISTORY_UPDATE:
                    updateDataHistory(apiKey, updater);
                    break;
                case PUSH_TRIGGERED_UPDATE:
                    pushTriggeredUpdate(apiKey, updater);
                    break;
                case INCREMENTAL_UPDATE:
                    updateData(apiKey, updater);
                    break;
                default:
                    logger.warn("module=updateQueue component=worker message=\"UpdateType was not handled (" + task.updateType + ")\"");
                    connectorUpdateService.setUpdateWorkerTaskStatus(task.getId(), Status.FAILED);
            }
        }
        else {
            // This connector does not support update so mark the update task as done
            connectorUpdateService.setUpdateWorkerTaskStatus(task.getId(), Status.DONE);

            StringBuilder sb2 = new StringBuilder("module=updateQueue component=worker")
                    .append(" guestId=").append(task.getGuestId())
                    .append(" connector=").append(task.connectorName)
                    .append(" apiKeyId=").append(task.apiKeyId)
                    .append(" message=\"Connector does not support sync, skipping update\"");
    		logger.info(sb2.toString());
        }
	}

    private void logNR() {
        try {
            StringBuilder taskName = new StringBuilder("Background_Update_");
            taskName.append(task.connectorName);
            taskName.append("_").append(task.objectTypes);
            NewRelic.setTransactionName(null, taskName.toString());
            NewRelic.addCustomParameter("connector", task.connectorName);
            NewRelic.addCustomParameter("objectType", task.objectTypes);
            NewRelic.addCustomParameter("guestId", task.getGuestId());
        } catch (Throwable t) {
            logger.warn("Could not set NR info..." + task.connectorName);
        }
    }

    private void pushTriggeredUpdate(ApiKey apiKey,
			AbstractUpdater updater) {
		try {
            // TODO: check if this connector type is enabled and supportsSync before calling update.
            // If it is disabled and/or does not support sync, don't try to update it.

            logger.info("module=updateQueue component=worker action=pushTriggeredUpdate " +
                        "connector=" + apiKey.getConnector().getName() + " guestId=" + apiKey.getGuestId());
			UpdateInfo updateInfo = UpdateInfo.pushTriggeredUpdateInfo(apiKey,
					task.objectTypes, task.jsonParams);
			UpdateResult updateResult = updater.updateData(updateInfo);
			handleUpdateResult(updateInfo, updateResult);
		} catch (Throwable e) {
			String stackTrace = stackTrace(e);
			logger.warn("module=updateQueue component=worker action=pushTriggeredUpdate " +
                        "message=\"Unexpected Exception\" " +
                        "guestId=" + task.guestId + " objectType=" + task.objectTypes +
                        " stackTrace=<![CDATA[" + stackTrace + "]]>");
		}
	}

	private void updateDataHistory(ApiKey apiKey,
			AbstractUpdater updater) {
        String message = "<img class=\"loading-animation\" src=\"/static/img/loading.gif\"/>You have successfully added a new connector: "
                         + apiKey.getConnector().prettyName()
                         + ". Your data is now being retrieved. "
                         + "It may take a little while until it becomes visible.";
        notificationsService.addNamedNotification(apiKey.getGuestId(), Notification.Type.INFO,
                                                  apiKey.getConnector().statusNotificationName(), message);
        // TODO: check if this connector type is enabled and supportsSync before calling update.
        // If it is disabled and/or does not support sync, don't try to update it.
        logger.info("module=updateQueue component=worker action=updateDataHistory " +
                    "connector=" + apiKey.getConnector().getName() + " guestId=" + apiKey.getGuestId());
        UpdateInfo updateInfo = UpdateInfo.initialHistoryUpdateInfo(apiKey,
                task.objectTypes);
        UpdateResult updateResult = updater.updateDataHistory(updateInfo);
        handleUpdateResult(updateInfo, updateResult);
	}

    private void updateData(final ApiKey apiKey, final AbstractUpdater updater) {
        // TODO: check if this connector type is enabled and supportsSync before calling update.
        // If it is disabled and/or does not support sync, don't try to update it.
        logger.info("module=updateQueue component=worker action=\"updateData (incremental update)\"" +
                    " connector=" + apiKey.getConnector().getName() + " guestId=" + apiKey.getGuestId());
        UpdateInfo updateInfo = UpdateInfo.IncrementalUpdateInfo(apiKey, task.objectTypes);
        UpdateResult result = updater.updateData(updateInfo);
        handleUpdateResult(updateInfo, result);
    }

	private void handleUpdateResult(final UpdateInfo updateInfo, UpdateResult updateResult) {
        guestService.setApiKeyToSynching(updateInfo.apiKey.getId(), false);
		switch (updateResult.getType()) {
		case DUPLICATE_UPDATE:
			duplicateUpdate();
			break;
		case HAS_REACHED_RATE_LIMIT:
            final UpdateWorkerTask.AuditTrailEntry rateLimit = new UpdateWorkerTask.AuditTrailEntry(new Date(), updateResult.getType().toString(), "long reschedule");
            rateLimit.stackTrace = updateResult.stackTrace;
			rescheduleAccordingToQuotaSpecifications(updateInfo, rateLimit);
			break;
		case UPDATE_SUCCEEDED:
            // Check for existing status notification
            long guestId=updateInfo.apiKey.getGuestId();
            String statusName = updateInfo.apiKey.getConnector().statusNotificationName();
            Notification notification = notificationsService.getNamedNotification(guestId,statusName);
            if (updateInfo.getUpdateType()== UpdateInfo.UpdateType.INITIAL_HISTORY_UPDATE ||
                (notification!=null && notification.deleted==false)) {
                // This is either an initial history update or there's an existing visible status notification.
                // Update the notification to show the update succeeded.
                notificationsService.addNamedNotification(guestId, Notification.Type.INFO,
                                                          statusName,
                                                          "<i class=\"icon-ok\" style=\"margin-right:7px\"/>Your " + updateInfo.apiKey.getConnector().getPrettyName() + " data was successfully imported.  " +
                                                          "See <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a> dialog for details.");
            }
			success(updateInfo.apiKey);
			break;
		case UPDATE_FAILED:
        case UPDATE_FAILED_PERMANENTLY:
            final UpdateWorkerTask.AuditTrailEntry failed = new UpdateWorkerTask.AuditTrailEntry(new Date(), updateResult.getType().toString(), "abort");
            failed.stackTrace = updateResult.stackTrace;
            connectorUpdateService.addAuditTrail(task.getId(), failed);
            final UpdateWorkerTask.AuditTrailEntry retry = new UpdateWorkerTask.AuditTrailEntry(new Date(), updateResult.getType().toString(), "retry");
            retry.stackTrace = updateResult.stackTrace;
            // Consider this a transient failure and retry if the failure type was not permanent
            // and the current status of the connector instance is not already STATUS_PERMANENT_FAILURE.
            //
            if(updateResult.getType()==UpdateResult.ResultType.UPDATE_FAILED &&
               updateInfo.apiKey.getStatus()!=ApiKey.Status.STATUS_PERMANENT_FAILURE) {
                retry(updateInfo, retry);
            }
            else {
                // This was a permanent failure, so we should set status to permanent failure and
                // we should not retry
                abort(updateInfo.apiKey,failed);
            }
            if (updateInfo.getUpdateType()== UpdateInfo.UpdateType.INITIAL_HISTORY_UPDATE)
                notificationsService.addNamedNotification(updateInfo.apiKey.getGuestId(), Notification.Type.ERROR,
                                                          updateInfo.apiKey.getConnector().statusNotificationName(),
                                                          "<i class=\"icon-remove-sign\" style=\"color:red;margin-right:7px\"/>There was a problem while importing your " + updateInfo.apiKey.getConnector().getPrettyName() + " data. We will try again later.  " +
                                                          "See <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a> dialog for details."
                );
			break;
		case NO_RESULT:
			abort(updateInfo.apiKey,null);
			break;
		}
	}

    private void rescheduleAccordingToQuotaSpecifications(final UpdateInfo updateInfo, final UpdateWorkerTask.AuditTrailEntry auditTrailEntry) {
        longReschedule(updateInfo, auditTrailEntry);
        guestService.setApiKeyStatus(updateInfo.apiKey.getId(), ApiKey.Status.STATUS_OVER_RATE_LIMIT, auditTrailEntry.stackTrace);
    }

    private void duplicateUpdate() {
		logger.warn("module=updateQueue component=worker action=duplicateUpdate guestId=" + task.getGuestId() +
				" connector=" + task.connectorName + " objectType="
                + task.objectTypes);
	}

	private void success(ApiKey apiKey) {
		StringBuilder stringBuilder = new StringBuilder("module=updateQueue component=worker action=success")
                .append(" guestId=").append(task.getGuestId())
                .append(" connector=").append(task.objectTypes);
		logger.info(stringBuilder.toString());
        guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_UP, null);
		connectorUpdateService.setUpdateWorkerTaskStatus(task.getId(), Status.DONE);
	}

	private void abort(ApiKey apiKey, UpdateWorkerTask.AuditTrailEntry auditTrailEntry) {
		StringBuilder stringBuilder = new StringBuilder("module=updateQueue component=worker action=abort")
                .append(" guestId=").append(task.getGuestId())
                .append(" connector=").append(task.connectorName)
                .append(" objectType=").append(task.objectTypes);
		logger.info(stringBuilder.toString());
        guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, auditTrailEntry.stackTrace);
		connectorUpdateService.setUpdateWorkerTaskStatus(task.getId(), Status.FAILED);
	}

	private void retry(UpdateInfo updateInfo, UpdateWorkerTask.AuditTrailEntry auditTrailEntry) {
		StringBuilder stringBuilder = new StringBuilder("module=updateQueue component=worker action=retry")
                .append(" guestId=").append(task.getGuestId())
                .append(" connector=").append(task.connectorName)
                .append(" objectType=").append(task.objectTypes);
        if (auditTrailEntry.stackTrace!=null) {
            stringBuilder.append(" stackTrace=<![CDATA[")
                    .append(auditTrailEntry.stackTrace)
                    .append("]]>");
        }
		logger.info(stringBuilder.toString());
		int maxRetries = 0;
		try {
			maxRetries = getMaxRetries(updateInfo.apiKey.getConnector());
		} catch (Throwable t) {
			t.printStackTrace();
		}
		if (task.retries < maxRetries) {
            auditTrailEntry.nextAction = "short reschedule";
            shortReschedule(updateInfo.apiKey, auditTrailEntry);
		} else {
            auditTrailEntry.nextAction = "long reschedule";
 			longReschedule(updateInfo, auditTrailEntry);
		}
	}

	private void longReschedule(UpdateInfo updateInfo, UpdateWorkerTask.AuditTrailEntry auditTrailEntry) {
		StringBuilder stringBuilder = new StringBuilder("module=updateQueue component=worker action=longReschedule")
                .append(" guestId=").append(task.getGuestId())
                .append(" connector=").append(task.connectorName)
                .append(" objectType=").append(task.objectTypes);
		logger.info(stringBuilder.toString());
        guestService.setApiKeyStatus(updateInfo.apiKey.getId(), ApiKey.Status.STATUS_TRANSIENT_FAILURE, auditTrailEntry.stackTrace);
		// re-schedule when we are below rate limit again
        Long resetTime = updateInfo.getSafeResetTime();
        if (resetTime==null) {
            final int longRetryDelay = getLongRetryDelay(updateInfo.apiKey.getConnector());
            resetTime = System.currentTimeMillis() + longRetryDelay;
        }
        connectorUpdateService.reScheduleUpdateTask(task.getId(), resetTime,
                                                    false, auditTrailEntry);
	}

	private void shortReschedule(ApiKey apiKey, UpdateWorkerTask.AuditTrailEntry auditTrailEntry) {
		StringBuilder sb = new StringBuilder("module=updateQueue component=worker action=shortReschedule")
                .append(" guestId=").append(task.getGuestId())
                .append(" connector=").append(task.connectorName)
                .append(" objectType=").append(task.objectTypes)
                .append(" retries=").append(String.valueOf(task.retries));
		logger.info(sb.toString());
		// schedule 1 minute later, typically
        guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_TRANSIENT_FAILURE, auditTrailEntry.stackTrace);
		connectorUpdateService.reScheduleUpdateTask(task.getId(), System.currentTimeMillis() + getShortRetryDelay(apiKey.getConnector()),
                                                    true, auditTrailEntry);
	}

	private int getMaxRetries(Connector connector) {
		String key = connector.getName() + ".maxRetries";
        if (env.connectors.containsKey(key)) {
            return Integer.valueOf((String)env.connectors.getProperty(key));
        }
        else {
            return Integer.valueOf((String)env.connectors.getProperty("maxRetries"));
        }
	}

	private int getShortRetryDelay(Connector connector) {
		String key = connector.getName() + ".shortRetryDelay";
        if (env.connectors.containsKey(key)) {
            return Integer.valueOf((String)env.connectors.getProperty(key));
        }
        else {
            return Integer.valueOf((String)env.connectors.getProperty("shortRetryDelay"));
        }
	}

	private int getLongRetryDelay(Connector connector) {
		String key = connector.getName() + ".longRetryDelay";
        if (env.connectors.containsKey(key)) {
            return Integer.valueOf((String)env.connectors.getProperty(key));
        }
        else {
            return Integer.valueOf((String)env.connectors.getProperty("longRetryDelay"));
        }
	}

}
