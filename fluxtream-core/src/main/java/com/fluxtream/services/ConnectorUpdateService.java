package com.fluxtream.services;

import java.util.List;
import java.util.Set;

import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.ScheduleResult;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.domain.UpdateWorkerTask;

public interface ConnectorUpdateService {

	public void cleanupRunningUpdateTasks();

    public List<ScheduleResult> updateConnector(long guestId, Connector connector);

    public List<ScheduleResult> updateAllConnectors(long guestId);

    public List<ApiUpdate> getUpdates(long guestId, Connector connector, int pageSize, int page);

	public void addUpdater(Connector connector, AbstractUpdater updater);

	public AbstractUpdater getUpdater(Connector connector);

	public ApiUpdate getLastUpdate(long guestId, Connector api);

	public ApiUpdate getLastSuccessfulUpdate(long guestId, Connector api);

	public ApiUpdate getLastSuccessfulUpdate(long guestId, Connector api,
			int objectTypes);

	public Set<Long> getConnectorGuests(Connector connector);
	
	public void addApiUpdate(long guestId, Connector api, int objectTypes,
			long ts, long elapsed, String query, boolean success, long lastSync);

	public void addApiNotification(Connector api, long guestId, String content);

	public ScheduleResult scheduleUpdate(long guestId, String connectorName,
			int objectTypes, UpdateInfo.UpdateType updateType,
			long timeScheduled, String... jsonParams);

	public UpdateWorkerTask isUpdateScheduled(long guestId, String connectorName,
			UpdateInfo.UpdateType updateType, int objectTypes);

	public boolean isHistoryUpdateCompleted(long guestId, String connectorName,
			int objectTypes);

	public void pollScheduledUpdates();

	public void setUpdateWorkerTaskStatus(long updateWorkerTaskId, UpdateWorkerTask.Status status);

	public ScheduleResult reScheduleUpdateTask(UpdateWorkerTask updateWorkerTask, long time,
                                               boolean incrementRetries, UpdateWorkerTask.AuditTrailEntry auditTrailEntry);

	public UpdateWorkerTask getNextScheduledUpdateTask(long guestId, Connector connector);

	public void deleteScheduledUpdateTasks(long guestId, Connector connector);

	public long getTotalNumberOfGuestsUsingConnector(Connector connector);

	public long getTotalNumberOfUpdates(Connector connector);

	public long getTotalNumberOfUpdatesSince(Connector connector, long then);

	public long getNumberOfUpdates(long guestId, Connector connector);

	public long getNumberOfUpdatesSince(long guestId, Connector connector,
			long then);

    ApiUpdate getLastSuccessfulSync(long guestId, Connector api,
                                    int objectTypes);
}
