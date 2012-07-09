package com.fluxtream.services.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.ScheduleResult;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.connectors.updaters.UpdateInfo.UpdateType;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.ApiNotification;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.domain.UpdateWorkerTask;
import com.fluxtream.domain.UpdateWorkerTask.Status;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.utils.JPAUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Component
public class ConnectorUpdateServiceImpl implements ConnectorUpdateService {

	static Logger logger = Logger.getLogger(ConnectorUpdateServiceImpl.class);

	private Map<Connector, AbstractUpdater> updaters = new HashMap<Connector, AbstractUpdater>();

	@Autowired
	BeanFactory beanFactory;

	@Autowired
	ApiDataService apiDataService;

	@Autowired
	ThreadPoolTaskExecutor executor;

	@PersistenceContext
	EntityManager em;

	@Override
	@Transactional(readOnly = false)
	public void cleanupRunningUpdateTasks() {
		JPAUtils.execute(em, "updateWorkerTasks.delete.byStatus",
				Status.IN_PROGRESS);
	}

    @Transactional(readOnly = false)
	@Override
	public ScheduleResult reScheduleUpdateTask(UpdateWorkerTask updt, long time, boolean incrementRetries) {
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
		updt.status = Status.SCHEDULED;
		updt.timeScheduled = time;
		em.merge(updt);
		return new ScheduleResult(
				ScheduleResult.ResultType.SCHEDULED_UPDATE_DEFERRED);
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
			logger.error("action=bg_update stage=unknown error");
			throw exception;
		}
		updt.status = status;
	}

	@Override
	@Transactional(readOnly = false)
	public void pollScheduledUpdates() {
		logger.debug("looking for a job...");
		List<UpdateWorkerTask> updateWorkerTasks = JPAUtils.find(em,
				UpdateWorkerTask.class, "updateWorkerTasks.byStatus",
				UpdateWorkerTask.Status.SCHEDULED, System.currentTimeMillis());
		if (updateWorkerTasks.size() == 0) {
			logger.debug("nothing to do");
			return;
		}
		for (UpdateWorkerTask updateWorkerTask : updateWorkerTasks) {
			logger.debug("executing update: " + updateWorkerTask);
			setUpdateWorkerTaskStatus(updateWorkerTask.getId(), UpdateWorkerTask.Status.IN_PROGRESS);
			logger.info("guestId=" + updateWorkerTask.getGuestId() +
					"action=bg_update stage=launch");
			UpdateTask updateTask = beanFactory.getBean(UpdateTask.class);
			updateTask.su = updateWorkerTask;
			try {
				executor.execute(updateTask);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	@Override
	public void addUpdater(Connector connector, AbstractUpdater updater) {
		logger.info("adding updater for connector: " + connector + " : "
				+ updater.getClass());
		updaters.put(connector, updater);
	}

	@Override
	public AbstractUpdater getUpdater(Connector connector) {
		return updaters.get(connector);
	}

	@Override
	public ScheduleResult scheduleUpdate(long guestId, String connectorName,
			int objectTypes, UpdateType updateType, long timeScheduled,
			String... jsonParams) {
		boolean updateScheduled = isUpdateScheduled(guestId, connectorName,
				updateType, objectTypes);
		if (!updateScheduled) {
			UpdateWorkerTask updateWorkerTask = new UpdateWorkerTask();
			updateWorkerTask.guestId = guestId;
			updateWorkerTask.connectorName = connectorName;
			updateWorkerTask.objectTypes = objectTypes;
			updateWorkerTask.updateType = updateType;
			updateWorkerTask.status = UpdateWorkerTask.Status.SCHEDULED;
			updateWorkerTask.timeScheduled = timeScheduled;
			if (jsonParams!=null&&jsonParams.length>0)
				updateWorkerTask.jsonParams = jsonParams[0];
			em.persist(updateWorkerTask);
			long now = System.currentTimeMillis();
			return new ScheduleResult(
					timeScheduled <= now ? ScheduleResult.ResultType.SCHEDULED_UPDATE_IMMEDIATE
							: ScheduleResult.ResultType.SCHEDULED_UPDATE_DEFERRED);
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("guestId=");
			sb.append(guestId);
			sb.append(" action=bg_update stage=reject_scheduling ");
			sb.append("connectorName=");
			sb.append(connectorName);
			sb.append(" objectTypes=");
			sb.append(objectTypes);
			logger.info(sb.toString());
			return new ScheduleResult(
					ScheduleResult.ResultType.ALREADY_SCHEDULED);
		}
	}

	@Override
	public boolean isUpdateScheduled(long guestId, String connectorName,
			UpdateInfo.UpdateType updateType, int objectTypes) {
		List<UpdateWorkerTask> updateWorkerTasks = JPAUtils.find(em,
				UpdateWorkerTask.class, "updateWorkerTasks.exists",
				Status.SCHEDULED, Status.IN_PROGRESS, guestId, updateType,
				objectTypes, connectorName);
//		logger.info(guestId, "action=bg_update stage=check",
//				"scheduledOrInProgress=" + updateWorkerTasks);
		return updateWorkerTasks.size() != 0;
	}

	@Override
	public boolean isHistoryUpdateCompleted(long guestId, String connectorName,
			int objectTypes) {
		List<UpdateWorkerTask> updateWorkerTasks = JPAUtils.find(em,
				UpdateWorkerTask.class, "updateWorkerTasks.completed",
				Status.DONE, guestId,
				UpdateInfo.UpdateType.INITIAL_HISTORY_UPDATE, objectTypes,
				connectorName);
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
	public void addApiUpdate(long guestId, Connector api, int objectTypes,
			long ts, long elapsed, String query, boolean success, long lastSync) {
		ApiUpdate updt = new ApiUpdate();
		updt.guestId = guestId;
		updt.api = api.value();
		updt.ts = System.currentTimeMillis();
		updt.query = query;
		updt.objectTypes = objectTypes;
		updt.elapsed = elapsed;
		updt.success = success;
        updt.lastSync = lastSync;
		em.persist(updt);
	}

	@Override
	public ApiUpdate getLastUpdate(long guestId, Connector api) {
		ApiUpdate lastUpdate = JPAUtils.findUnique(em, ApiUpdate.class,
				"apiUpdates.last", guestId, api.value());
		return lastUpdate;
	}

	@Override
	public ApiUpdate getLastSuccessfulUpdate(long guestId, Connector api) {
		ApiUpdate lastUpdate = JPAUtils.findUnique(em, ApiUpdate.class,
				"apiUpdates.last.successful.byApi", guestId, api.value());
		return lastUpdate;
	}

    @Override
    public List<ApiUpdate> getUpdates(long guestId, final Connector connector, final int pageSize, final int page) {
        List<ApiUpdate> updates = JPAUtils.findPaged(em, ApiUpdate.class, "apiUpdates.last.paged", pageSize, page,
                                                     guestId, connector.value());
        return updates;
    }

    @Override
	public ApiUpdate getLastSuccessfulUpdate(long guestId, Connector api,
			int objectTypes) {
		if (objectTypes == -1)
			return getLastSuccessfulUpdate(guestId, api);
		ApiUpdate lastUpdate = JPAUtils.findUnique(em, ApiUpdate.class,
				"apiUpdates.last.successful.byApiAndObjectTypes", guestId,
				api.value(), objectTypes);
		return lastUpdate;
	}

    @Override
    public ApiUpdate getLastSuccessfulSync(long guestId, Connector api,
            int objectTypes) {
        if (objectTypes == -1)
            return getLastSuccessfulUpdate(guestId, api);
        ApiUpdate lastUpdate = JPAUtils.findUnique(em, ApiUpdate.class,
                "apiUpdates.lastSync", guestId,
                api.value(), objectTypes);
        return lastUpdate;
    }

	@Override
	public UpdateWorkerTask getNextScheduledUpdateTask(long guestId, Connector connector, int objectTypes) {
		UpdateWorkerTask updateWorkerTask = JPAUtils.findUnique(em,
				UpdateWorkerTask.class, "updateWorkerTasks.exists",
				Status.SCHEDULED, Status.IN_PROGRESS, guestId,
				UpdateInfo.UpdateType.INITIAL_HISTORY_UPDATE, objectTypes,
				connector.getName());
		return updateWorkerTask;
	}

	@Transactional(readOnly = false)
	@Override
	public void deleteScheduledUpdateTasks(long guestId, Connector connector) {
		JPAUtils.execute(em, "updateWorkerTasks.delete.byApi", guestId,
				connector.getName());
	}

	@Override
	public long getTotalNumberOfGuestsUsingConnector(Connector connector) {
		long n = JPAUtils.count(em, "apiKey.count.byApi",
				connector.value());
		return n;
	}

	@Override
	public long getTotalNumberOfUpdates(Connector connector) {
		long n = JPAUtils.count(em, "apiUpdates.count.all",
				connector.value());
		return n;
	}

	@Override
	public long getNumberOfUpdates(long guestId, Connector connector) {
		long n = JPAUtils.count(em,
				"apiUpdates.count.byGuest", guestId, connector.value());
		return n;
	}

	@Override
	public long getTotalNumberOfUpdatesSince(Connector connector, long then) {
		long n = JPAUtils.count(em,
				"apiUpdates.count.all.since", connector.value(), then);
		return n;
	}

	@Override
	public long getNumberOfUpdatesSince(long guestId, Connector connector,
			long then) {
		long n = JPAUtils.count(em,
				"apiUpdates.count.byGuest.since", guestId, connector.value(),
				then);
		return n;
	}

	@Override
	public Set<Long> getConnectorGuests(Connector connector) {
		List<ApiKey> keys = JPAUtils.find(em, ApiKey.class, "apiKeys.byConnector", connector.value());
		Set<Long> guestIds = new HashSet<Long>();
		for (ApiKey key : keys) {
			guestIds.add(key.getGuestId());
		}
		return guestIds;
	}

}
