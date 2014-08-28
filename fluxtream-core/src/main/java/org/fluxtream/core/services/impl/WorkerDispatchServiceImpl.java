package org.fluxtream.core.services.impl;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.domain.UpdateWorkerTask;
import org.fluxtream.core.utils.JPAUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This is a helper service, hence part of the 'impl' package, that enforces local ('nested') persistence of entities
 * whose lifecycle spans the calling methods entire execution time.
 * User: candide
 * Date: 14/08/14
 * Time: 15:59
 */
@Service
@Transactional(readOnly=true)
public class WorkerDispatchServiceImpl implements WorkerDispatchService {

    static FlxLogger logger = FlxLogger.getLogger(WorkerDispatchServiceImpl.class);

    @PersistenceContext
    EntityManager em;

    @Autowired
    @Qualifier("updateWorkersExecutor")
    ThreadPoolTaskExecutor executor;

    @Override
    @Transactional(readOnly=false, propagation = Propagation.REQUIRES_NEW)
    public List<UpdateWorkerTask> claimTasksForDispatch(int availableThreads, String serverUUID) {
        List<UpdateWorkerTask> updateWorkerTasks = JPAUtils.findWithLimit(em, UpdateWorkerTask.class, "updateWorkerTasks.byStatus", 0, availableThreads, UpdateWorkerTask.Status.SCHEDULED, System.currentTimeMillis());
        if (updateWorkerTasks.size() == 0) {
            logger.debug("Nothing to do");
        } else {
            StringBuilder sb = new StringBuilder("claiming tasks for dispatch, ").append(" availableThreads=" + availableThreads).append(" message=\"adding " + updateWorkerTasks.size() + " update worker tasks\"").append(" activeCount=" + executor.getActiveCount() + " maxPoolSize=" + executor.getMaxPoolSize());
            logger.info(sb);

            for (int i = 0; i < updateWorkerTasks.size(); i++) {
                UpdateWorkerTask task = updateWorkerTasks.get(i);
                task.startTime = null;
                task.endTime = null;
                task.workerThreadName = null;
                task.serverUUID = serverUUID;
                task.status = UpdateWorkerTask.Status.IN_PROGRESS;
                task.addAuditTrailEntry(new UpdateWorkerTask.AuditTrailEntry(new java.util.Date(), serverUUID));
            }
        }
        return updateWorkerTasks;
    }

    @Override
    @Transactional(readOnly=false, propagation = Propagation.REQUIRES_NEW)
    public void unclaimTask(final long taskId) {
        UpdateWorkerTask task = em.find(UpdateWorkerTask.class, taskId);
        if (task!=null) {
            task.serverUUID = null;
            task.status = UpdateWorkerTask.Status.SCHEDULED;
        }
    }
}
