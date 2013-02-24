package com.fluxtream.services.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.ScheduleResult;
import com.fluxtream.connectors.updaters.UpdateInfo.UpdateType;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.ApiNotification;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.domain.UpdateWorkerTask;
import com.fluxtream.domain.UpdateWorkerTask.Status;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.services.SystemService;
import com.fluxtream.utils.JPAUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Component
@Transactional(readOnly=true)
public class ConnectorUpdateServiceImpl implements ConnectorUpdateService {

    static Logger logger = Logger.getLogger(ConnectorUpdateServiceImpl.class);

    private Map<Connector, AbstractUpdater> updaters = new Hashtable<Connector, AbstractUpdater>();

    private boolean isShuttingDown;

    @Autowired
    BeanFactory beanFactory;

    @Autowired
    ApiDataService apiDataService;

    @Autowired
    ThreadPoolTaskExecutor executor;

    @Autowired
    GuestService guestService;

    @Autowired
    SystemService systemService;

    @PersistenceContext
    EntityManager em;

    @Autowired
    MetadataService metadataService;

    /**
     * Update all the facet types for a given user and connector.
     * @param apiKey The apiKey for which we want to update facets
     * @param force force an update (sync now); if false, it means it was called by the background "cron" task
     * @return
     */
    @Override
    public List<ScheduleResult> updateConnector(final ApiKey apiKey, boolean force){
        System.out.println("updateConnector");
        List<ScheduleResult> scheduleResults = new ArrayList<ScheduleResult>();
        StringBuilder messageRoot = new StringBuilder("module=updateQueue component=connectorUpdateService" +
                                                      " action=updateConnector");
        if (isShuttingDown) {
            logger.warn(messageRoot.append(" message=\"Service is shutting down... Refusing updates\""));
            return scheduleResults;
        }

        // if forcing an update (sync now), we actually want to flush the update requests
        // that have stacked up in the queue
        if (force)
            flushUpdateWorkerTasks(apiKey, false);

        // some connectors (e.g. the fitbit) need to decide what objectTypes to update by themselves;
        // for those, we pass 0 for the objectType parameter, which will be overridden by the connector's updater
        final boolean historyUpdateCompleted = isHistoryUpdateCompleted(apiKey);
        if (apiKey.getConnector().isAutonomous()) {
            scheduleObjectTypeUpdate(apiKey, 0, scheduleResults, historyUpdateCompleted
                                                                             ? UpdateType.INCREMENTAL_UPDATE
                                                                             : UpdateType.INITIAL_HISTORY_UPDATE);
        } else {
            int[] objectTypeValues = apiKey.getConnector().objectTypeValues();
            for (int objectTypes : objectTypeValues) {
                scheduleObjectTypeUpdate(apiKey, objectTypes, scheduleResults, historyUpdateCompleted
                                                                                           ? UpdateType.INCREMENTAL_UPDATE
                                                                                           : UpdateType.INITIAL_HISTORY_UPDATE);
            }
        }
        return scheduleResults;
    }

    private boolean isHistoryUpdateCompleted(ApiKey apiKey) {
        if (apiKey.getConnector().isAutonomous())
            return isHistoryUpdateCompleted(apiKey, 0);
        final int[] connectorObjectTypeValues = apiKey.getConnector().objectTypeValues();
        for (int connectorObjectTypeValue : connectorObjectTypeValues)
            if (!isHistoryUpdateCompleted(apiKey, connectorObjectTypeValue))
                return false;
        return true;
    }

    @Override
    public List<ScheduleResult> updateConnectorObjectType(ApiKey apiKey, int objectTypes, boolean force) {
        List<ScheduleResult> scheduleResults = new ArrayList<ScheduleResult>();
        if (isShuttingDown) {
            StringBuilder sb = new StringBuilder("module=updateQueue component=connectorUpdateService" +
                                                 " action=updateConnectorObjectType")
                    .append(" message=\"Service is shutting down... Refusing updates\"");
            logger.warn(sb.toString());
            return scheduleResults;
        }
        getScheduledUpdateTask(apiKey, objectTypes);
        // if forcing an update (sync now), we actually want to flush the update requests
        // that have stacked up in the queue
        if (force)
            flushUpdateWorkerTasks(apiKey, objectTypes, false);
        scheduleObjectTypeUpdate(apiKey, objectTypes, scheduleResults, UpdateType.INCREMENTAL_UPDATE);
        return scheduleResults;
    }

    /**
     * Schedules a new update if there is no update for the user for this ObjectType and <code>force</code> is false
     * @param apiKey The apiKey for which we want to update a specific facet/object type
     * @param objectTypes the integer bitmask of object types to be updated
     * @param scheduleResults The result of adding the update will be added to the list. \result.type will be of type
     *                        ScheduleResult.ResultType. If there was a previously existing \result.type will be
     *                        ALREADY_SCHEDULED
     */
    private void scheduleObjectTypeUpdate(final ApiKey apiKey, int objectTypes,
                                          List<ScheduleResult> scheduleResults,
                                          UpdateType updateType) {
        UpdateWorkerTask updateWorkerTask = getScheduledUpdateTask(apiKey, objectTypes);
        if (updateWorkerTask != null)
            scheduleResults.add(new ScheduleResult(apiKey.getId(), apiKey.getConnector().getName(),
                                                   objectTypes, ScheduleResult.ResultType.ALREADY_SCHEDULED, updateWorkerTask.timeScheduled));
        else {
            final ScheduleResult scheduleResult = scheduleUpdate(apiKey, objectTypes, updateType, System.currentTimeMillis());
            scheduleResults.add(scheduleResult);
        }
    }

    /**
     * Calls updateConnector(...) for all of a guest's connector
     * @param guestId
     * @return a list of objects that describe worker tasks that have been either modified or added
     * to the update queue
     */
    @Override
    public List<ScheduleResult> updateAllConnectors(final long guestId) {
        List<ScheduleResult> scheduleResults = new ArrayList<ScheduleResult>();
        if (isShuttingDown) {
            StringBuilder sb = new StringBuilder("module=updateQueue component=updateAllConnectors" +
                                                 " action=updateConnector")
                    .append(" message=\"Service is shutting down... Refusing updates\"");
            logger.warn(sb.toString());
            return scheduleResults;
        }
        final List<ApiKey> connectors = guestService.getApiKeys(guestId);
        for (ApiKey key : connectors) {
            if (key!=null && key.getConnector()!=null) {
                List<ScheduleResult> updateRes = updateConnector(key, false);
                scheduleResults.addAll(updateRes);
            }
        }
        return scheduleResults;
    }

    @Transactional(readOnly = false)
    @Override
    public ScheduleResult reScheduleUpdateTask(UpdateWorkerTask updt, long time, boolean incrementRetries,
                                               UpdateWorkerTask.AuditTrailEntry auditTrailEntry) {
        if (isShuttingDown) {
            StringBuilder sb = new StringBuilder("module=updateQueue component=updateAllConnectors" +
                                                 " action=updateConnector")
                    .append(" message=\"Service is shutting down... Refusing updates\"");
            logger.warn(sb.toString());
            return new ScheduleResult(
                    updt.apiKeyId,
                    updt.connectorName,
                    updt.getObjectTypes(),
                    ScheduleResult.ResultType.SYSTEM_IS_SHUTTING_DOWN,
                    time);
        }
        if (!incrementRetries) {
            UpdateWorkerTask failed = new UpdateWorkerTask(updt);
            failed.retries = updt.retries;
            failed.connectorName = updt.connectorName;
            failed.status = Status.FAILED;
            failed.guestId = updt.guestId;
            failed.timeScheduled = updt.timeScheduled;
            em.persist(failed);
            updt.retries = 0;
        } else
            updt.retries += 1;
        updt.addAuditTrailEntry(auditTrailEntry);
        updt.status = Status.SCHEDULED;
        updt.timeScheduled = time;
        em.merge(updt);
        return new ScheduleResult(
                updt.apiKeyId,
                updt.connectorName,
                updt.getObjectTypes(),
                ScheduleResult.ResultType.SCHEDULED_UPDATE_DEFERRED,
                time);
    }

    @Override
    @Transactional(readOnly = false)
    public void setUpdateWorkerTaskStatus(long updateWorkerTaskId, Status status)
            throws RuntimeException {
        UpdateWorkerTask updt = em
                .find(UpdateWorkerTask.class, updateWorkerTaskId);
        if (updt == null) {
            RuntimeException exception = new RuntimeException(
                    "null UpdateWorkerTask trying to set its status: "
                    + updateWorkerTaskId);
            logger.error("module=updateQueue component=connectorUpdateService action=setUpdateWorkerTaskStatus");
            throw exception;
        }
        updt.status = status;
    }

    @Override
    @Transactional(readOnly = false)
    public void pollScheduledUpdates() {
        if (isShuttingDown) {
            StringBuilder sb = new StringBuilder("module=updateQueue component=pollScheduledUpdates" +
                                                 " action=updateConnector")
                    .append(" message=\"Service is shutting down... Stopping Task Queue polling...\"");
            logger.warn(sb.toString());
            return;
        }
        List<UpdateWorkerTask> updateWorkerTasks = JPAUtils.find(em,
                                                                 UpdateWorkerTask.class, "updateWorkerTasks.byStatus",
                                                                 Status.SCHEDULED, System.currentTimeMillis());
        if (updateWorkerTasks.size() == 0) {
            logger.debug("module=updateQueue component=connectorUpdateService action=pollScheduledUpdates message=\"Nothing to do\"");
            return;
        }
        for (UpdateWorkerTask updateWorkerTask : updateWorkerTasks) {
            logger.info("module=updateQueue component=connectorUpdateService action=pollScheduledUpdates" +
                        " message=\"Executing update: " +
                        " \"" + updateWorkerTask);
            setUpdateWorkerTaskStatus(updateWorkerTask.getId(), Status.IN_PROGRESS);

            UpdateWorker updateWorker = beanFactory.getBean(UpdateWorker.class);
            updateWorker.task = updateWorkerTask;
            try {
                executor.execute(updateWorker);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    @Override
    public void addUpdater(Connector connector, AbstractUpdater updater) {
        updaters.put(connector, updater);
    }

    @Override
    public AbstractUpdater getUpdater(Connector connector) {
        return beanFactory.getBean(connector.getUpdaterClass());
    }

    @Override
    @Transactional(readOnly = false)
    public ScheduleResult scheduleUpdate(final ApiKey apiKey,
                                         int objectTypes, UpdateType updateType, long timeScheduled,
                                         String... jsonParams) {
        if (isShuttingDown) {
            StringBuilder sb = new StringBuilder("module=updateQueue component=updateAllConnectors" +
                                                 " action=scheduleUpdate")
                    .append(" message=\"Service is shutting down... Refusing updates\"");
            logger.warn(sb.toString());
            return new ScheduleResult(
                    apiKey.getId(),
                    apiKey.getConnector().getName(),
                    objectTypes,
                    ScheduleResult.ResultType.SYSTEM_IS_SHUTTING_DOWN,
                    System.currentTimeMillis());
        }
        UpdateWorkerTask updateScheduled = getScheduledUpdateTask(apiKey, objectTypes);
        ScheduleResult scheduleResult = null;
        if (updateScheduled==null) {
            UpdateWorkerTask updateWorkerTask = new UpdateWorkerTask();
            updateWorkerTask.guestId = apiKey.getGuestId();
            updateWorkerTask.connectorName = apiKey.getConnector().getName();
            updateWorkerTask.apiKeyId = apiKey.getId();
            updateWorkerTask.objectTypes = objectTypes;
            updateWorkerTask.updateType = updateType;
            updateWorkerTask.status = Status.SCHEDULED;
            updateWorkerTask.timeScheduled = timeScheduled;
            if (jsonParams!=null&&jsonParams.length>0)
                updateWorkerTask.jsonParams = jsonParams[0];
            em.persist(updateWorkerTask);
            long now = System.currentTimeMillis();
            scheduleResult = new ScheduleResult(apiKey.getId(), apiKey.getConnector().getName(), objectTypes,
                                                timeScheduled <= now
                                                ? ScheduleResult.ResultType.SCHEDULED_UPDATE_IMMEDIATE
                                                : ScheduleResult.ResultType.SCHEDULED_UPDATE_DEFERRED,
                                                timeScheduled);
        } else {
            scheduleResult = new ScheduleResult(apiKey.getId(), apiKey.getConnector().getName(), objectTypes,
                                                ScheduleResult.ResultType.ALREADY_SCHEDULED,
                                                updateScheduled.timeScheduled);
        }
        StringBuilder sb = new StringBuilder("module=updateQueue component=connectorUpdateService action=scheduleUpdate")
                .append(" guestId=").append(apiKey.getGuestId())
                .append(" connectorName=").append(apiKey.getConnector().getName())
                .append(" objectTypes=").append(objectTypes)
                .append(" resultType=").append(scheduleResult.type.toString());
        logger.info(sb.toString());
        return scheduleResult;
    }

    @Override
    public boolean isHistoryUpdateCompleted(final ApiKey apiKey,
                                            int objectTypes) {
        List<UpdateWorkerTask> updateWorkerTasks = JPAUtils.find(em,
                                                                 UpdateWorkerTask.class, "updateWorkerTasks.completed",
                                                                 Status.DONE, apiKey.getGuestId(),
                                                                 UpdateType.INITIAL_HISTORY_UPDATE, objectTypes,
                                                                 apiKey.getConnector().getName(),
                                                                 apiKey.getId());
        return updateWorkerTasks.size() > 0;
    }

    @Override
    @Transactional(readOnly = false)
    public void addApiNotification(Connector connector, long guestId, String content) {
        ApiNotification notification = new ApiNotification();
        notification.api = connector.value();
        notification.guestId = guestId;
        notification.ts = System.currentTimeMillis();
        notification.content = content;
        em.persist(notification);
    }

    @Override
    @Transactional(readOnly = false)
    public void addApiUpdate(final long guestId, final Connector connector, int objectTypes, long ts, long elapsed, String query, boolean success) {
        ApiUpdate updt = new ApiUpdate();
        updt.guestId = guestId;
        updt.api = connector.value();
        updt.ts = System.currentTimeMillis();
        updt.query = query;
        updt.objectTypes = objectTypes;
        updt.elapsed = elapsed;
        updt.success = success;
        em.persist(updt);
    }

    @Override
    public ApiUpdate getLastUpdate(ApiKey apiKey) {
        return JPAUtils.findUnique(em, ApiUpdate.class, "apiUpdates.last", apiKey.getGuestId(), apiKey.getConnector().value(), apiKey.getId());
    }

    @Override
    public ApiUpdate getLastSuccessfulUpdate(ApiKey apiKey) {
        return JPAUtils.findUnique(em, ApiUpdate.class, "apiUpdates.last.successful.byApi", apiKey.getGuestId(), apiKey.getConnector().value(), apiKey.getId());
    }

    @Override
    public List<ApiUpdate> getUpdates(ApiKey apiKey, final int pageSize, final int page) {
        return JPAUtils.findPaged(em, ApiUpdate.class, "apiUpdates.last.paged", pageSize, page, apiKey.getGuestId(), apiKey.getConnector().value(), apiKey.getId());
    }

    @Override
    public ApiUpdate getLastSuccessfulUpdate(ApiKey apiKey,
                                             int objectTypes) {
        if (objectTypes == -1)
            return getLastSuccessfulUpdate(apiKey);
        return JPAUtils.findUnique(em, ApiUpdate.class, "apiUpdates.last.successful.byApiAndObjectTypes", apiKey.getGuestId(), apiKey.getConnector().value(), objectTypes, apiKey.getId());
    }

    @Override
    @Transactional(readOnly = false)
    public UpdateWorkerTask getScheduledUpdateTask(final ApiKey apiKey, int objectTypes) {
        UpdateWorkerTask updateWorkerTask = JPAUtils.findUnique(em,
                                                                UpdateWorkerTask.class, "updateWorkerTasks.withObjectTypes.isScheduled",
                                                                Status.SCHEDULED, Status.IN_PROGRESS, apiKey.getGuestId(),
                                                                objectTypes, apiKey.getConnector().getName(),
                                                                apiKey.getId());
        if (updateWorkerTask!=null&&hasStalled(updateWorkerTask)) {
            updateWorkerTask.status = Status.STALLED;
            em.merge(updateWorkerTask);
            return null;
        }
        return updateWorkerTask;
    }

    @Override
    @Transactional(readOnly = false)
    public List<UpdateWorkerTask> getScheduledOrInProgressUpdateTasks(final ApiKey apiKey) {
        List<UpdateWorkerTask> updateWorkerTask = JPAUtils.find(em, UpdateWorkerTask.class,
                                                                "updateWorkerTasks.isScheduledOrInProgress",
                                                                apiKey.getGuestId(), apiKey.getConnector().getName(),
                                                                apiKey.getId());
        for (UpdateWorkerTask workerTask : updateWorkerTask) {
            if (hasStalled(workerTask)) {
                workerTask.status = Status.STALLED;
                em.merge(workerTask);
            }
        }
        return updateWorkerTask;
    }

    @Override
    public Collection<UpdateWorkerTask> getUpdatingUpdateTasks(final ApiKey apiKey) {
        List<UpdateWorkerTask> tasks = JPAUtils.find(em, UpdateWorkerTask.class, "updateWorkerTasks.isInProgressOrScheduledBefore",
                                                     System.currentTimeMillis(), apiKey.getGuestId(),
                                                     apiKey.getConnector().getName(), apiKey.getId());
        HashMap<Integer, UpdateWorkerTask> seen = new HashMap<Integer, UpdateWorkerTask>();
        for(UpdateWorkerTask task : tasks)
        {
            if(hasStalled(task))
            {
                task.status = Status.STALLED;
                em.merge(task);
            }
            else
            {
                if(seen.containsKey(task.objectTypes))
                {
                    if(seen.get(task.objectTypes).timeScheduled < task.timeScheduled)
                        seen.put(task.objectTypes, task);
                }
                else
                {
                    seen.put(task.objectTypes, task);
                }
            }
        }
        return seen.values();
    }

    private boolean hasStalled(UpdateWorkerTask updateWorkerTask) {
        return System.currentTimeMillis()-updateWorkerTask.timeScheduled>3600000;
    }

    /**
     * delete pending tasks for a guest's connector
     * @param apiKey The apiKey for which we want to update facets
     * @param wipeOutHistory wether to delete everything including the initial history update that
     *                       we use to track wether we need to everything from scratch or just do so
     *                       incrementally
     */
    @Transactional(readOnly = false)
    @Override
    public void flushUpdateWorkerTasks(final ApiKey apiKey, boolean wipeOutHistory) {
        List<AbstractUpdater> toStop = new ArrayList<AbstractUpdater>();
        if (!wipeOutHistory)
            JPAUtils.execute(em, "updateWorkerTasks.delete.byApi", apiKey.getGuestId(),
                             apiKey.getConnector().getName(),
                             apiKey.getId(),
                             UpdateType.INITIAL_HISTORY_UPDATE);
        else
            JPAUtils.execute(em, "updateWorkerTasks.deleteAll.byApi", apiKey.getGuestId(),
                             apiKey.getConnector().getName(), apiKey.getId());
    }

    /**
     * delete pending tasks for a guest's connector
     * @param apiKey The apiKey for which we want to update a specific facet/object type
     * @param wipeOutHistory wether to delete everything including the initial history update that
     *                       we use to track wether we need to everything from scratch or just do so
     *                       incrementally
     */
    @Transactional(readOnly = false)
    @Override
    public void flushUpdateWorkerTasks(final ApiKey apiKey, int objectTypes, boolean wipeOutHistory) {
        List<AbstractUpdater> toStop = new ArrayList<AbstractUpdater>();
        if (!wipeOutHistory)
            JPAUtils.execute(em, "updateWorkerTasks.delete.byApiAndObjectType", apiKey.getGuestId(),
                             apiKey.getConnector().getName(),
                             apiKey.getId(),
                             objectTypes,
                             UpdateType.INITIAL_HISTORY_UPDATE);
        else
            JPAUtils.execute(em, "updateWorkerTasks.deleteAll.byApiAndObjectType", apiKey.getGuestId(),
                             apiKey.getConnector().getName(), apiKey.getId(),
                             objectTypes);
    }

    @Override
    public long getTotalNumberOfUpdatesSince(Connector connector, long then) {
        return JPAUtils.count(em,
                              "apiUpdates.count.all.since", connector.value(), then);
    }

    @Override
    public long getNumberOfUpdatesSince(final long guestId, int connectorValue, long then) {
        return JPAUtils.count(em,
                              "apiUpdates.count.byGuest.since", guestId, connectorValue,
                              then);
    }

    @Override
    public Collection<UpdateWorkerTask> getLastFinishedUpdateTasks(ApiKey apiKey) {
        List<UpdateWorkerTask> tasks = JPAUtils.find(em, UpdateWorkerTask.class,
                                                     "updateWorkerTasks.getLastFinishedTask",
                                                     System.currentTimeMillis(),
                                                     apiKey.getGuestId(),
                                                     apiKey.getConnector().getName(),
                                                     apiKey.getId());
        HashMap<Integer, UpdateWorkerTask> seen = new HashMap<Integer, UpdateWorkerTask>();
        for(UpdateWorkerTask task : tasks)
        {
            if(seen.containsKey(task.objectTypes))
            {
                if(seen.get(task.objectTypes).timeScheduled < task.timeScheduled)
                    seen.put(task.objectTypes, task);
            }
            else
            {
                seen.put(task.objectTypes, task);
            }
        }
        return seen.values();
    }

    @Override
    public void resumeInterruptedUpdates() {
        // find all UpdateWorkerTasks (/scheduledUpdates) whose status is IN_PROGRESS
        // for more than an hour
        List<UpdateWorkerTask> interruptedUpdates = getInterruptedUpdates();
        for (UpdateWorkerTask interruptedUpdate : interruptedUpdates) {
            final ApiKey apiKey = em.find(ApiKey.class, interruptedUpdate.apiKeyId);
            flushUpdateWorkerTasks(apiKey, true);
            // delete all facets with that apiKeyId
            updateConnector(apiKey, true);
        }
    }

    public List<UpdateWorkerTask> getInterruptedUpdates() {
        return new ArrayList<UpdateWorkerTask>();
    }
}
