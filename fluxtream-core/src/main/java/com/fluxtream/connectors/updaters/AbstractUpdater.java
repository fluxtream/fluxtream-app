package com.fluxtream.connectors.updaters;

import com.fluxtream.connectors.ApiClientSupport;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.dao.FacetDao;
import com.fluxtream.connectors.updaters.UpdateInfo.UpdateType;
import com.fluxtream.domain.AbstractUserProfile;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Notification;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.BodyTrackStorageService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.services.NotificationsService;
import com.fluxtream.utils.Utils;
import com.fluxtream.aspects.FlxLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static com.fluxtream.utils.Utils.stackTrace;

public abstract class AbstractUpdater extends ApiClientSupport {

	static FlxLogger logger = FlxLogger.getLogger(AbstractUpdater.class);

    @Autowired
	protected ApiDataService apiDataService;

    @Autowired
    protected ConnectorUpdateService connectorUpdateService;

	@Autowired
	protected GuestService guestService;

	@Autowired
	protected JPADaoService jpaDaoService;

    @Qualifier("JPAFacetDao")
    @Autowired
	protected FacetDao facetDao;

    @Autowired
	protected NotificationsService notificationsService;

    @Autowired
	protected BodyTrackStorageService bodyTrackStorageService;

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

	public final UpdateResult updateDataHistory(UpdateInfo updateInfo)
			throws Exception {

        try {
            logger.info("module=updateQueue component=updater action=updateDataHistory" +
                " guestId=" + updateInfo.getGuestId() + " connector=" + updateInfo.apiKey.getConnector().getName());

            updateConnectorDataHistory(updateInfo);
            bodyTrackStorageService.storeInitialHistory(updateInfo.apiKey);

            return UpdateResult.successResult();
        // TODO: in case of a problem here, we really should reset the connector's data
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
            sb = new StringBuilder("We were unable to import your ");
                    sb.append(updateInfo.apiKey.getConnector().prettyName())
                    .append(" data");
            if (t.getMessage()!=null) {
               sb .append(", error message: \"")
                        .append(t.getMessage()).append("\")").toString();
            }
            notificationsService.addNotification(updateInfo.apiKey.getGuestId(),
                                                 Notification.Type.WARNING,
                                                 sb.toString(), stackTrace);
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

		UpdateResult updateResult = new UpdateResult();
		try {
			if (updateInfo.getUpdateType() == UpdateType.TIME_INTERVAL_UPDATE)
				apiDataService.eraseApiData(updateInfo.apiKey, updateInfo.objectTypes,
						updateInfo.getTimeInterval());
            try {
                updateConnectorData(updateInfo);
                updateResult.type = UpdateResult.ResultType.UPDATE_SUCCEEDED;
            } catch (Exception e) {
                final String stackTrace = Utils.stackTrace(e);
                StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=updateData")
                        .append(" message=\"Unexpected exception\" connector=")
                        .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=").append(updateInfo.apiKey.getGuestId())
                        .append(" stackTrace=<![CDATA[").append(stackTrace).append("]]>")
                        .append(updateInfo.apiKey.getGuestId());
                logger.warn(sb.toString());
                updateResult = new UpdateResult(
                        UpdateResult.ResultType.UPDATE_FAILED);
                updateResult.stackTrace = stackTrace;
            }
		} catch (Throwable t) {
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=updateData")
                    .append(" message=\"Couldn't update data\" connector=")
                    .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=")
                    .append(updateInfo.apiKey.getGuestId())
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(t)).append("]]>");
            logger.warn(sb.toString());
        }

		return updateResult;
	}

	final protected void countSuccessfulApiCall(ApiKey apiKey, int objectTypes,
			long then, String query) {
        StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=countSuccessfulApiCall")
                .append(" connector=" + connector().getName())
                .append(" objectTypes=" + objectTypes)
                .append(" apiKeyId=").append(apiKey.getId())
                .append(" guestId=").append(apiKey.getGuestId())
                .append(" query=").append(query);
        logger.info(sb.toString());
		connectorUpdateService.addApiUpdate(apiKey.getGuestId(), apiKey.getConnector(), objectTypes, then, System.currentTimeMillis() - then, query, true);
	}

	final protected void countFailedApiCall(ApiKey apiKey, int objectTypes,
			long then, String query, String stackTrace) {
        StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=countFailedApiCall")
                .append(" connector=" + connector().getName())
                .append(" objectTypes=" + objectTypes)
                .append(" apiKeyId=").append(apiKey.getId())
                .append(" guestId=").append(apiKey.getGuestId())
                .append(" query=").append(query)
                .append(" stackTrace=<![CDATA[").append(stackTrace).append("]]>");
        logger.info(sb.toString());
		connectorUpdateService.addApiUpdate(apiKey.getGuestId(), apiKey.getConnector(), objectTypes, then, System.currentTimeMillis() - then, query, false);
	}

    /**
     * Performs and incremental update of the connector
     * @param updateInfo Update information
     * @throws Exception If update fails
     */
	protected abstract void updateConnectorData(UpdateInfo updateInfo)
			throws Exception;

}
