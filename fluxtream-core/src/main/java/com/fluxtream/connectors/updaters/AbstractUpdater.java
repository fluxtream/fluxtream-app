package com.fluxtream.connectors.updaters;

import static com.fluxtream.utils.Utils.stackTrace;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.fluxtream.connectors.ApiClientSupport;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.dao.FacetDao;
import com.fluxtream.connectors.updaters.UpdateInfo.UpdateType;
import com.fluxtream.domain.AbstractUserProfile;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.BodyTrackStorageService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.services.NotificationsService;
import com.fluxtream.utils.Utils;
import org.springframework.beans.factory.annotation.Qualifier;

public abstract class AbstractUpdater extends ApiClientSupport {

	static Logger logger = Logger.getLogger(AbstractUpdater.class);

    @Qualifier("apiDataServiceImpl")
    @Autowired
	protected ApiDataService apiDataService;

	@Autowired
	protected GuestService guestService;

	@Autowired
	protected JPADaoService jpaDaoService;

    @Qualifier("JPAFacetDao")
    @Autowired
	protected FacetDao facetDao;

    @Qualifier("notificationsServiceImpl")
    @Autowired
	protected NotificationsService notificationsService;

    @Qualifier("bodyTrackStorageServiceImpl")
    @Autowired
	protected BodyTrackStorageService bodyTrackStorageService;

	private static Vector<RunningUpdate> runningUpdates = new Vector<RunningUpdate>();

	private String connectorName;

	final protected Connector connector() {
		if (connectorName == null)
			connectorName = Connector.getConnectorName(this.getClass().getName());
		return Connector.getConnector(connectorName);
	}

	public AbstractUpdater() {
	}

	@Autowired
	final protected void setConnectorUpdateService(@Qualifier("connectorUpdateServiceImpl") ConnectorUpdateService ads) {
		Connector connector = connector();
		ads.addUpdater(connector, this);
	}

	// TODO: this is not clusterizable -> should be done with redis
	private class RunningUpdate {
		Connector api;
		int objectTypes;
		UpdateInfo updateInfo;
		long guestId;

		public RunningUpdate(Connector api, int objectTypes,
				UpdateInfo updateInfo, long guestId) {
			super();
			this.api = api;
			this.objectTypes = objectTypes;
			this.updateInfo = updateInfo;
			this.guestId = guestId;
		}

		public boolean equals(Object o) {
            if(o instanceof  RunningUpdate)
            {
                RunningUpdate ru = (RunningUpdate) o;
                return ru.api == api && ru.objectTypes == objectTypes
                        && ru.updateInfo.isIdentical(updateInfo)
                        && ru.guestId == guestId;
            }
            else
                return false;
		}

	}

	public final UpdateResult updateDataHistory(UpdateInfo updateInfo)
			throws Exception {

        try {
            logger.info("module=updateQueue component=updater action=updateDataHistory" +
                " guestId=" + updateInfo.getGuestId() + " connector=" + updateInfo.apiKey.getConnector().getName());
            updateConnectorDataHistory(updateInfo);
            bodyTrackStorageService.storeInitialHistory(
                    updateInfo.getGuestId(), updateInfo.apiKey.getConnector()
                            .getName());

            return UpdateResult.successResult();
        } catch (RateLimitReachedException e) {
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=updateDataHistory")
                    .append(" message=\"rate limit was reached exception\" connector=")
                    .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=")
                    .append(updateInfo.apiKey.getGuestId());
            logger.warn(sb.toString());
            return UpdateResult.rateLimitReachedResult();
        } catch (Throwable t) {
            String stackTrace = stackTrace(t);
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=updateDataHistory")
                    .append(" message=\"Unexpected exception\" connector=")
                    .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=")
                    .append(updateInfo.apiKey.getGuestId())
                    .append(" stackTrace=<![CDATA[" + stackTrace + "]]>");
            logger.warn(sb.toString());
            return UpdateResult.failedResult(stackTrace);
        }
	}

	@SuppressWarnings({"unchecked","unused"})
	protected final <T extends AbstractUserProfile> T saveUserProfile(
			UpdateInfo updateInfo, Class<T> clazz) throws Exception {
		AbstractUserProfile loadUserProfile = loadUserProfile(updateInfo, clazz);
		guestService.saveUserProfile(updateInfo.apiKey.getGuestId(),
				loadUserProfile);
		return (T) loadUserProfile;
	}

    @SuppressWarnings("unused")
	protected <T extends AbstractUserProfile> T loadUserProfile(
			UpdateInfo updateInfo, Class<T> clazz) throws Exception {
		throw new RuntimeException("Not Implemented");
	}

    /**
     * Updates all connector information
     * @param updateInfo update information for the connector
     * @throws Exception If an api's limit has been reached or if an update fails for another reason
     */
	protected abstract void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws Exception;

	public final UpdateResult updateData(UpdateInfo updateInfo) {
		if (hasReachedRateLimit(connector(), updateInfo.apiKey.getGuestId())) {
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=updateData")
                               .append(" message=\"rate limit was reached\" connector=")
                               .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=")
                               .append(updateInfo.apiKey.getGuestId());
			logger.warn(sb.toString());
			return new UpdateResult(
					UpdateResult.ResultType.HAS_REACHED_RATE_LIMIT);
		}

		// prevent two equivalent updates of running at the same time
		RunningUpdate runningUpdate = new RunningUpdate(
				updateInfo.apiKey.getConnector(), updateInfo.objectTypes,
				updateInfo, updateInfo.apiKey.getGuestId());

		if (runningUpdates.contains(runningUpdate))
			return new UpdateResult(UpdateResult.ResultType.DUPLICATE_UPDATE);
		runningUpdates.add(runningUpdate);

		UpdateResult updateResult = new UpdateResult();
		try {
			if (updateInfo.getUpdateType() == UpdateType.TIME_INTERVAL_UPDATE)
				apiDataService.eraseApiData(updateInfo.apiKey.getGuestId(),
						connector(), updateInfo.objectTypes,
						updateInfo.getTimeInterval());
            try {
                updateConnectorData(updateInfo);
                updateResult.type = UpdateResult.ResultType.UPDATE_SUCCEEDED;
            } catch (Exception e) {
                updateResult = new UpdateResult(
                        UpdateResult.ResultType.UPDATE_FAILED);
                updateResult.stackTrace = Utils.stackTrace(e);
                e.printStackTrace();
            }
		} finally {
			runningUpdates.remove(runningUpdate);
		}
		return updateResult;
	}

	final protected void countSuccessfulApiCall(long guestId, int objectTypes,
			long then, String query) {
        StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=countSuccessfulApiCall")
                .append(" connector=" + connector().getName())
                .append(" objectTypes=" + objectTypes)
                .append(" guestId=").append(guestId)
                .append(" query=").append(query);
        logger.info(sb.toString());
		connectorUpdateService.addApiUpdate(guestId, connector(), objectTypes,
				then, System.currentTimeMillis() - then, query, true);
	}

	final protected void countFailedApiCall(long guestId, int objectTypes,
			long then, String query, String stackTrace) {
        StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=countFailedApiCall")
                .append(" connector=" + connector().getName())
                .append(" objectTypes=" + objectTypes)
                .append(" guestId=").append(guestId)
                .append(" query=").append(query)
                .append(" stackTrace=<![CDATA[").append(stackTrace).append("]]>");
        logger.info(sb.toString());
		connectorUpdateService.addApiUpdate(guestId, connector(), objectTypes,
				then, System.currentTimeMillis() - then, query, false);
	}

    /**
     * Performs and incremental update of the connector
     * @param updateInfo Update information
     * @throws Exception If update fails
     */
	protected abstract void updateConnectorData(UpdateInfo updateInfo)
			throws Exception;

}
