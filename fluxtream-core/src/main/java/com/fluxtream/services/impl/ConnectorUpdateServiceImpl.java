package com.fluxtream.services.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import com.fluxtream.aspects.FlxLogger;
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
import org.joda.time.DateTimeConstants;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Component
@Transactional(readOnly=true)
public class ConnectorUpdateServiceImpl implements ConnectorUpdateService, InitializingBean {

    static FlxLogger logger = FlxLogger.getLogger(ConnectorUpdateServiceImpl.class);

    private Map<Connector, AbstractUpdater> updaters = new Hashtable<Connector, AbstractUpdater>();

    @Autowired
    BeanFactory beanFactory;

    @Autowired
    ApiDataService apiDataService;

    @Autowired
    @Qualifier("updateWorkersExecutor")
    ThreadPoolTaskExecutor executor;

    @Autowired
    GuestService guestService;

    @Autowired
    SystemService systemService;

    @PersistenceContext
    EntityManager em;

    @Autowired
    MetadataService metadataService;

    @Override
    public void afterPropertiesSet() throws Exception {
        executor.setThreadGroupName("UpdateWorkers");
        executor.setThreadNamePrefix("UpdateWorker-");
    }

    /**
     * This makes sure that we are only executing Update Jobs that were
     * created while this server was alive
     */
    private final String SERVER_UUID = UUID.randomUUID().toString();

    /**
     * Update all the facet types for a given user and connector.
     * @param apiKey The apiKey for which we want to update facets
     * @param force force an update (sync now); if false, it means it was called by the background "cron" task
     * @return
     */
    @Override
    public List<ScheduleResult> updateConnector(final ApiKey apiKey, boolean force){
        List<ScheduleResult> scheduleResults = new ArrayList<ScheduleResult>();

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
        long guestId=0;
        if(apiKey !=null) {
            guestId = apiKey.getGuestId();
        }
        System.out.println("updateConnector: guestId=" + guestId + ", apiKey=" + apiKey);

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
        getUpdateWorkerTask(apiKey, objectTypes);
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
        UpdateWorkerTask updateWorkerTask = getUpdateWorkerTask(apiKey, objectTypes);
        if (updateWorkerTask != null)
            scheduleResults.add(new ScheduleResult(apiKey.getId(), apiKey.getConnector().getName(),
                                                   objectTypes, ScheduleResult.ResultType.ALREADY_SCHEDULED, updateWorkerTask.timeScheduled));
        else {
            final ScheduleResult scheduleResult = scheduleUpdate(apiKey, objectTypes, updateType, System.currentTimeMillis());
            scheduleResults.add(scheduleResult);
        }
    }

    @Override
    @Transactional(readOnly=false)
    public void cleanupStaleData() {
        long oneDayAgo = System.currentTimeMillis() - DateTimeConstants.MILLIS_PER_DAY;
        final Query cleanupUpdateWorkerTasks = em.createNativeQuery(String.format("DELETE FROM UpdateWorkerTask WHERE not(status=2 AND updateType=2) and timeScheduled<%s", oneDayAgo));
        final int updateWorkerTasksDeleted = cleanupUpdateWorkerTasks.executeUpdate();
        System.out.println("deleted " + updateWorkerTasksDeleted + " UpdateWorkerTasks");
        final Query cleanupApiUpdates = em.createNativeQuery(String.format("DELETE FROM ApiUpdates WHERE ts<%s", oneDayAgo));
        final int apiUpdatesDeleted = cleanupApiUpdates.executeUpdate();
        System.out.println("deleted " + apiUpdatesDeleted + " ApiUpdates");
    }

    /**
     * Calls updateConnector(...) for all of a guest's connector
     * @param guestId
     * @return a list of objects that describe worker tasks that have been either modified or added
     * to the update queue
     */
    public void cleanupStaleData(final long guestId) {
        final List<ApiKey> connectors = guestService.getApiKeys(guestId);
        for (ApiKey key : connectors) {
            if (key!=null && key.getConnector()!=null) {
                // cleanup previously executed tasks
                cleanupUpdateWorkerTasks(key);
            }
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
    public ScheduleResult reScheduleUpdateTask(long updateWorkerTaskId, long time, boolean incrementRetries,
                                               UpdateWorkerTask.AuditTrailEntry auditTrailEntry) {
        UpdateWorkerTask updt = em
                .find(UpdateWorkerTask.class, updateWorkerTaskId);

        if (!incrementRetries) {
            UpdateWorkerTask failed = new UpdateWorkerTask(updt);
            failed.auditTrail = updt.auditTrail;
            failed.retries = updt.retries;
            failed.connectorName = updt.connectorName;
            failed.status = Status.FAILED;
            failed.guestId = updt.guestId;
            failed.timeScheduled = updt.timeScheduled;
            updt.retries = 0;
            em.persist(failed);
        } else
            updt.retries += 1;

        updt.addAuditTrailEntry(auditTrailEntry);
        updt.status = Status.SCHEDULED;
        updt.timeScheduled = time;
        em.persist(updt);

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
    public void pollScheduledUpdateWorkerTasks() {
        List<UpdateWorkerTask> updateWorkerTasks = JPAUtils.find(em, UpdateWorkerTask.class, "updateWorkerTasks.byStatus", Status.SCHEDULED, getLiveOrUnclaimedServerUUIDs(), System.currentTimeMillis());
        if (updateWorkerTasks.size() == 0) {
            logger.debug("module=updateQueue component=connectorUpdateService action=pollScheduledUpdateWorkerTasks message=\"Nothing to do\"");
            return;
        }

        int maxThreads = executor.getMaxPoolSize();
        int activeThreads = executor.getActiveCount();
        int availableThreads = maxThreads - activeThreads;

        StringBuilder sb = new StringBuilder("module=updateQueue component=connectorUpdateService action=pollScheduleUpdates")
            .append(" availableThreads="+availableThreads);

        logger.info(sb);
        if (availableThreads<updateWorkerTasks.size()) {
            sb.append(" message=\"tasks overflow!\" nTasks=" + updateWorkerTasks.size());
            logger.warn(sb);
        }

        int nWorkers = Math.min(updateWorkerTasks.size(), availableThreads);

        for (int i=0; i<nWorkers; i++) {
            UpdateWorkerTask updateWorkerTask = updateWorkerTasks.get(i);
            logger.info("module=updateQueue component=connectorUpdateService action=pollScheduledUpdateWorkerTasks" +
                        " message=\"Executing update: " +
                        " \"" + updateWorkerTask);

            claim(updateWorkerTask.getId());

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
        UpdateWorkerTask updateScheduled = getUpdateWorkerTask(apiKey, objectTypes);
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
            updateWorkerTask.serverUUID = UNCLAIMED;
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
        List<UpdateWorkerTask> updateWorkerTasks = JPAUtils.find(em, UpdateWorkerTask.class, "updateWorkerTasks.completed", Status.DONE, UpdateType.INITIAL_HISTORY_UPDATE, objectTypes, apiKey.getId());
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
    public void addApiUpdate(final ApiKey apiKey, int objectTypes, long ts, long elapsed, String query, boolean success) {
        ApiUpdate updt = new ApiUpdate();
        updt.guestId = apiKey.getGuestId();
        updt.api = apiKey.getConnector().value();
        updt.apiKeyId = apiKey.getId();
        updt.ts = System.currentTimeMillis();
        updt.query = query;
        updt.objectTypes = objectTypes;
        updt.elapsed = elapsed;
        updt.success = success;
        em.persist(updt);
    }

    @Override
    public ApiUpdate getLastUpdate(ApiKey apiKey) {
        return JPAUtils.findUnique(em, ApiUpdate.class, "apiUpdates.last", apiKey.getId());
    }

    @Override
    public ApiUpdate getLastSuccessfulUpdate(ApiKey apiKey) {
        return JPAUtils.findUnique(em, ApiUpdate.class, "apiUpdates.last.successful.byApi", apiKey.getId());
    }

    @Override
    public List<ApiUpdate> getUpdates(ApiKey apiKey, final int pageSize, final int page) {
        return JPAUtils.findPaged(em, ApiUpdate.class, "apiUpdates.last.paged", pageSize, page, apiKey.getId());
    }

    @Override
    public ApiUpdate getLastSuccessfulUpdate(ApiKey apiKey,
                                             int objectTypes) {
        if (objectTypes == -1)
            return getLastSuccessfulUpdate(apiKey);
        return JPAUtils.findUnique(em, ApiUpdate.class, "apiUpdates.last.successful.byApiAndObjectTypes", objectTypes, apiKey.getId());
    }

    @Override
    @Transactional(readOnly = false)
    public UpdateWorkerTask getUpdateWorkerTask(final ApiKey apiKey, int objectTypes) {
        UpdateWorkerTask updateWorkerTask = JPAUtils.findUnique(em,
                                                                UpdateWorkerTask.class, "updateWorkerTasks.withObjectTypes.isScheduled",
                                                                Status.SCHEDULED, Status.IN_PROGRESS,
                                                                objectTypes,
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
        List<UpdateWorkerTask> updateWorkerTask = JPAUtils.find(em, UpdateWorkerTask.class, "updateWorkerTasks.isScheduledOrInProgress", apiKey.getId());
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
                                                     System.currentTimeMillis(), getLiveServerUUIDs(),
                                                     apiKey.getId());
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

    private List<String> getLiveOrUnclaimedServerUUIDs() {
        List<String> list = new ArrayList<String>();
        list.add(SERVER_UUID);
        list.add(UNCLAIMED);
        return list;
    }

    private List<String> getLiveServerUUIDs() {
        List<String> list = new ArrayList<String>();
        list.add(SERVER_UUID);
        return list;
    }

    private boolean hasStalled(UpdateWorkerTask updateWorkerTask) {
        return System.currentTimeMillis()-updateWorkerTask.timeScheduled>3600000;
    }

    /**
     * cleanup done tasks for a guest's connector
     * @param apiKey The apiKey for which we want to update facets
     */
    @Transactional(readOnly = false)
    public void cleanupUpdateWorkerTasks(final ApiKey apiKey) {
        final int tasksDeleted = JPAUtils.execute(em, "updateWorkerTasks.cleanup.byApi", apiKey.getId(), UpdateType.INITIAL_HISTORY_UPDATE);
        logger.info("module=updateQueue component=connectorUpdateService action=cleanupUpdateWorkerTasks" +
                    " deleted=" + tasksDeleted + " connector=" + apiKey.getConnector().getName());
        em.flush();
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
        if (!wipeOutHistory)
            JPAUtils.execute(em, "updateWorkerTasks.delete.byApi",
                             apiKey.getId(),
                             UpdateType.INITIAL_HISTORY_UPDATE);
        else
            JPAUtils.execute(em, "updateWorkerTasks.deleteAll.byApi", apiKey.getId());
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
        if (!wipeOutHistory)
            JPAUtils.execute(em, "updateWorkerTasks.delete.byApiAndObjectType",
                             apiKey.getId(),
                             objectTypes,
                             UpdateType.INITIAL_HISTORY_UPDATE);
        else
            JPAUtils.execute(em, "updateWorkerTasks.deleteAll.byApiAndObjectType", apiKey.getId(),
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
    @Transactional(readOnly=false)
    public void claim(final long taskId) {
        UpdateWorkerTask task = em.find(UpdateWorkerTask.class, taskId);
        task.serverUUID = SERVER_UUID;
        task.status = Status.IN_PROGRESS;
        task.addAuditTrailEntry(new UpdateWorkerTask.AuditTrailEntry(new java.util.Date(), SERVER_UUID));
        em.persist(task);
    }

    @Override
    @Transactional(readOnly=false)
    public void addAuditTrail(final long updateWorkerTaskId, final UpdateWorkerTask.AuditTrailEntry auditTrailEntry) {
        UpdateWorkerTask task = em.find(UpdateWorkerTask.class, updateWorkerTaskId);
        task.addAuditTrailEntry(auditTrailEntry);
        em.persist(task);
    }
}
