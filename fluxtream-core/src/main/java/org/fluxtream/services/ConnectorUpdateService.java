package org.fluxtream.services;

import java.util.Collection;
import java.util.List;
import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.updaters.AbstractUpdater;
import org.fluxtream.connectors.updaters.ScheduleResult;
import org.fluxtream.connectors.updaters.UpdateInfo;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.domain.ApiUpdate;
import org.fluxtream.domain.UpdateWorkerTask;

public interface ConnectorUpdateService {

    public static final String UNCLAIMED = "unassigned";

    /**
     * Schedules updates for the given connector for the user
     * @param apiKey The apiKey for which we want to update a specific facet/object type
     * @param force force an update (sync now)
     * @return A list containing data about what was scheduled
     */
    public List<ScheduleResult> updateConnector(ApiKey apiKey, boolean force);
    public List<ScheduleResult> updateConnector(ApiKey apiKey, boolean force, long updateTime);

    /**
     * Schedules an updated for on ObjectType of the given connector for the given user
     * @param apiKey The apiKey for which we want to update a specific facet/object type
     * @param objectTypes the objectType that is being updated. This is a bitmask which can represent multiple objectTypes
     *                    The value of each objectType is defined in the ObjectType spec. Values are always powers of 2
     *                    which allows for the bitmask. For example: objectTypes = 5 means that both the objectType of
     *                    value 4 and the objectType of value 1 are to be updated
     * @param force force an update (sync now)
     * @return A list containing data about what was scheduled
     */
    public List<ScheduleResult> updateConnectorObjectType(ApiKey apiKey,
                                                              int objectTypes,
                                                              boolean force,
                                                              boolean historyUpdate);
    public List<ScheduleResult> updateConnectorObjectType(ApiKey apiKey,
                                                          int objectTypes,
                                                          boolean force,
                                                          boolean historyUpdate,
                                                          long updateTime);

    public List<ScheduleResult> updateAllConnectors(long guestId, boolean force);
    public List<ScheduleResult> updateAllConnectors(final long guestId, boolean force, long updateTime);

    public List<ApiUpdate> getUpdates(ApiKey apiKey, int pageSize, int page);

	public void addUpdater(Connector connector, AbstractUpdater updater);

	public AbstractUpdater getUpdater(Connector connector);

	public ApiUpdate getLastUpdate(ApiKey apiKey);

    public ApiUpdate getLastSuccessfulUpdate(ApiKey apiKey);

	public ApiUpdate getLastSuccessfulUpdate(ApiKey apiKey,
			int objectTypes);

	public void addApiUpdate(final ApiKey apiKey, int objectTypes, long ts, long elapsed, String query,
                             boolean success, Integer httpResponseCode, String reason);

	public void addApiNotification(Connector connector, long guestId, String content);

	public ScheduleResult scheduleUpdate(ApiKey apiKey,
			int objectTypes, UpdateInfo.UpdateType updateType,
			long timeScheduled, String... jsonParams);

	public UpdateWorkerTask getUpdateWorkerTask(ApiKey apiKey, int objectTypes);

	public boolean isHistoryUpdateCompleted(ApiKey apiKey,
			int objectTypes);

	public void pollScheduledUpdateWorkerTasks();

    /**
     * Sets the updateWorkerTask to the given status
     * @param updateWorkerTaskId the id of the task whose status is to be updated
     * @param status the status to set the task to
     */
	public void setUpdateWorkerTaskStatus(long updateWorkerTaskId, UpdateWorkerTask.Status status);

	public ScheduleResult reScheduleUpdateTask(long updateWorkerTaskId, long time,
                                               boolean incrementRetries, UpdateWorkerTask.AuditTrailEntry auditTrailEntry);

    /**
     * Returns a list of all scheduled updates for the connector for the given user
     * @return a list of scheduled tasks
     */
	public List<UpdateWorkerTask> getScheduledOrInProgressUpdateTasks(ApiKey apiKey);

    public Collection<UpdateWorkerTask> getUpdatingUpdateTasks(ApiKey apiKey);

    List<String> getLiveServerUUIDs();

    public void flushUpdateWorkerTasks(ApiKey apiKey, boolean wipeOutHistory);

    public void flushUpdateWorkerTasks(ApiKey apiKey, int objectTypes, boolean wipeOutHistory);

	public long getTotalNumberOfUpdatesSince(Connector connector, long then);

	public long getNumberOfUpdatesSince(long guestId, int connectorValue, long then);

    public Collection<UpdateWorkerTask> getLastFinishedUpdateTasks(ApiKey apiKey);

    // Returns true if the task was claimed and false otherwise.  If returns false the caller
    // should not try to continue with task execution.
    public boolean claim(long taskId);

    public void addAuditTrail(long updateWorkerTaskId, UpdateWorkerTask.AuditTrailEntry auditTrailEntry);

    public void cleanupStaleData();

    List<UpdateWorkerTask> getAllSynchingUpdateWorkerTasks();

    List<UpdateWorkerTask> getAllScheduledUpdateWorkerTasks();

    List<UpdateWorkerTask> getScheduledUpdateWorkerTasksForConnectorNameBeforeTime(final String connectorName, long beforeTime);

    List<UpdateWorkerTask> getUpdateWorkerTasks(ApiKey apiKey, int objectTypes, int max);
}
