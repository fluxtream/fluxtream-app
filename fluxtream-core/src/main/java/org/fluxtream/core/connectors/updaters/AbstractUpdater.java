package org.fluxtream.core.connectors.updaters;

import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.ApiClientSupport;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.dao.FacetDao;
import org.fluxtream.core.domain.*;
import org.fluxtream.core.services.*;
import org.fluxtream.core.utils.Utils;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.Arrays;
import java.util.List;

import static org.fluxtream.core.utils.Utils.stackTrace;

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

    @Autowired
	protected FacetDao facetDao;

    @PersistenceContext
    protected EntityManager em;

    @Autowired
	protected NotificationsService notificationsService;

    @Autowired
	protected BodyTrackStorageService bodyTrackStorageService;

    @Autowired
    protected DataUpdateService dataUpdateService;

	private String connectorName;

	final protected Connector connector() {
		if (connectorName == null)
			connectorName = Connector.getConnectorName(this.getClass().getName());
		return Connector.getConnector(connectorName);
	}

    public AbstractUpdater() {
	}

    protected void extractCommonFacetData(AbstractFacet facet, UpdateInfo updateInfo) {
        facet.apiKeyId = updateInfo.apiKey.getId();
        facet.guestId = updateInfo.apiKey.getGuestId();
        facet.api = updateInfo.apiKey.getConnector().value();
        facet.timeUpdated = System.currentTimeMillis();
    }

	@Autowired
	final protected void setConnectorUpdateService(@Qualifier("connectorUpdateServiceImpl") ConnectorUpdateService ads) {
		Connector connector = connector();
		ads.addUpdater(connector, this);
	}

	public final UpdateResult updateDataHistory(UpdateInfo updateInfo) {
        final long updateStartTime = System.currentTimeMillis();
        try {
            logger.info("module=updateQueue component=updater action=updateDataHistory" +
                " guestId=" + updateInfo.getGuestId() + " connector=" + updateInfo.apiKey.getConnector().getName());

            updateConnectorDataHistory(updateInfo);

            // At this point, versions prior to 5/25/13 called
            //    bodyTrackStorageService.storeInitialHistory(updateInfo.apiKey);
            // to flush all the facets created for this connector to the
            // datastore.  However, incremental updates of connectors are
            // expected to flush their new facets to the datastore
            // as they go, which in practice means that the initial
            // history updates for almost all the connectors are
            // sent to the datastore at least twice.  In fact,
            // it's worse than that because updateDataHistory is
            // potentially called by multiple different object types
            // for a given connector.  So in the old version
            // a given facet was potentially sent to the datastore
            // 2*<number of object types> times (for example,
            // 6 times for the Bodymedia connector).
            //
            // It's much better to require that each connector flushes its
            // own facets to the datstore for both the initial history and
            // incremental updates.
            //
            // If the connector consistently uses the
            //    apiDataService.cacheApiData
            // calls to store the data retrieved from API calls, then this
            // will happen automatically.  See the Bodymedia connector for
            // an example.
            //
            // Otherwise, the updater needs to
            // manually make sure to flush the data to the datastore.
            // See the Mymee updater for an example.

            return UpdateResult.successResult();
        } catch (RateLimitReachedException e) {
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=updateDataHistory")
                    .append(" message=\"rate limit was reached exception\" connector=")
                    .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=")
                    .append(updateInfo.apiKey.getGuestId());
            logger.warn(sb.toString());
            return UpdateResult.rateLimitReachedResult(e);
        } catch (AuthRevokedException e) {
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=updateDataHistory")
                    .append(" message=\"auth revoked\" connector=")
                    .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=")
                    .append(updateInfo.apiKey.getGuestId());
            logger.warn(sb.toString());
            return UpdateResult.authRevokedResult(e);
        } catch (AuthExpiredException e) {
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=updateDataHistory")
                    .append(" message=\"connector needs re-authorization\" connector=")
                    .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=")
                    .append(updateInfo.apiKey.getGuestId());
            logger.warn(sb.toString());
            return UpdateResult.needsReauth();
        } catch (UpdateFailedException e) {
            String stackTrace = stackTrace(e);
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=updateDataHistory")
                    .append(" message=\"update failed\" connector=")
                    .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=")
                    .append(updateInfo.apiKey.getGuestId())
                    .append(" stackTrace=<![CDATA[" + stackTrace + "]]>");
            logger.warn(sb.toString());
            return UpdateResult.failedResult(e);
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
                        .append(t.getMessage()).toString();
            }
            notificationsService.addNamedNotification(updateInfo.apiKey.getGuestId(), Notification.Type.WARNING,
                                                      connector().statusNotificationName(),
                                                      sb.toString());
            return UpdateResult.failedResult(stackTrace, ApiKey.PermanentFailReason.UNKNOWN);
        }
        finally {
            try {
                // Update the time bounds no matter how we exit the updater.
                recordModifiedTimeBounds(updateStartTime,updateInfo);
                updateTimeBounds(updateInfo);
            } catch(Throwable t) {
                final String stackTrace = Utils.stackTrace(t);
                StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=updateData")
                        .append(" message=\"Couldn't update time bounds\" connector=")
                        .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=").append(updateInfo.apiKey.getGuestId())
                        .append(" stackTrace=<![CDATA[").append(stackTrace).append("]]>")
                        .append(updateInfo.apiKey.getGuestId());
                logger.warn(sb.toString());
            }
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
        final long updateStartTime = System.currentTimeMillis();
		UpdateResult updateResult;
        try {
            updateConnectorData(updateInfo);
            updateResult = UpdateResult.successResult();
        } catch (AuthRevokedException e) {
            updateResult = UpdateResult.authRevokedResult(e);
        } catch (RateLimitReachedException e) {
            updateResult = UpdateResult.rateLimitReachedResult(e);
        }  catch (AuthExpiredException e) {
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=updateData").append(" message=\"connector needs re-authorization\" connector=").append(updateInfo.apiKey.getConnector().toString()).append(" guestId=").append(updateInfo.apiKey.getGuestId());
            logger.warn(sb.toString());
            return UpdateResult.needsReauth();
        } catch (UpdateFailedException e) {
            final String stackTrace = Utils.stackTrace(e);
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=updateData")
                    .append(" message=\"Update failed exception\" connector=")
                    .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=").append(updateInfo.apiKey.getGuestId())
                    .append(" isPermanent=").append(e.isPermanent())
                    .append(" stackTrace=<![CDATA[").append(stackTrace).append("]]>")
                    .append(updateInfo.apiKey.getGuestId());
            logger.warn(sb.toString());
            updateResult = UpdateResult.failedResult(e);
        } catch (Throwable e) {
            final String stackTrace = Utils.stackTrace(e);
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=updateData")
                    .append(" message=\"Unexpected exception\" connector=")
                    .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=").append(updateInfo.apiKey.getGuestId())
                    .append(" stackTrace=<![CDATA[").append(stackTrace).append("]]>")
                    .append(updateInfo.apiKey.getGuestId());
            logger.warn(sb.toString());
            updateResult = UpdateResult.failedResult(stackTrace, ApiKey.PermanentFailReason.UNKNOWN);
        }
        finally {
            // Update the time bounds no matter how we exit the updater.
            try {
                recordModifiedTimeBounds(updateStartTime,updateInfo);
                updateTimeBounds(updateInfo);
            } catch(Throwable t) {
                final String stackTrace = Utils.stackTrace(t);
                StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=updateData")
                        .append(" message=\"Couldn't update time bounds\" connector=")
                        .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=").append(updateInfo.apiKey.getGuestId())
                        .append(" stackTrace=<![CDATA[").append(stackTrace).append("]]>")
                        .append(updateInfo.apiKey.getGuestId());
                logger.warn(sb.toString());
            }
        }

		return updateResult;
	}

    private final void recordModifiedTimeBounds(final long startTime, final UpdateInfo updateInfo){
        List<ObjectType> objectTypes = updateInfo.objectTypes();
        if (objectTypes.size() == 0){
            objectTypes = Arrays.asList(updateInfo.apiKey.getConnector().objectTypes());
        }
        if (objectTypes.size() > 0){
            for (ObjectType objectType : objectTypes){
                try{
                    if (!objectType.isClientFacet())
                        continue;
                    if (!updateInfo.apiKey.getConnector().hasFacets())
                        continue;
                    Class<? extends AbstractFacet> facetClass = objectType.facetClass();
                    final String facetName = facetClass.getAnnotation(Entity.class).name();
                    String queryString = new StringBuilder("SELECT min(facet.start), max(facet.end) FROM ")
                            .append(facetName)
                            .append(" facet WHERE facet.apiKeyId=:apiKeyId AND facet.timeUpdated>=:since")
                            .toString();
                    final Query query = em.createQuery(queryString);
                    query.setParameter("apiKeyId", updateInfo.apiKey.getId());
                    query.setParameter("since", startTime);
                    Object[] result = (Object[]) query.getResultList().get(0);
                    if (result[0] == null)
                        continue;
                    dataUpdateService.logApiDataUpdate(updateInfo.apiKey.getGuestId(),updateInfo.apiKey.getId(),(long) objectType.value(),(Long) result[0],(Long) result[1]);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        else{        //TODO: determine if this is ever reached
            try{
                if (updateInfo.apiKey.getConnector().hasFacets()){
                    Class<? extends AbstractFacet> facetClass = updateInfo.apiKey.getConnector().facetClass();
                    final String facetName = facetClass.getAnnotation(Entity.class).name();
                    String queryString = new StringBuilder("SELECT min(facet.start), max(facet.end) FROM ")
                            .append(facetName)
                            .append(" facet WHERE facet.apiKeyId=:apiKeyId AND facet.timeUpdated>=:since")
                            .toString();
                    final Query query = em.createQuery(queryString);
                    query.setParameter("apiKeyId", updateInfo.apiKey.getId());
                    query.setParameter("since", startTime);
                    Object[] result = (Object[]) query.getResultList().get(0);
                    if (result[0] != null)
                        dataUpdateService.logApiDataUpdate(updateInfo.apiKey.getGuestId(),updateInfo.apiKey.getId(),null,(Long) result[0],(Long) result[1]);
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }


        }
    }

    private void updateTimeBounds(final UpdateInfo updateInfo) {
        final List<ObjectType> objectTypes = updateInfo.objectTypes();
        if (objectTypes==null||objectTypes.size()==0) {
            saveTimeBoundaries(updateInfo.apiKey, null);
        } else {
            for (ObjectType objectType : objectTypes) {
                saveTimeBoundaries(updateInfo.apiKey, objectType);
            }
        }
        // Consider the last sync time to be whenever the updater
        // completes
        guestService.setApiKeyAttribute(updateInfo.apiKey,
                                        ApiKeyAttribute.LAST_SYNC_TIME_KEY,
                                        ISODateTimeFormat.dateHourMinuteSecondFraction().
                                                withZoneUTC().print(System.currentTimeMillis()));
    }

    private void saveTimeBoundaries(final ApiKey apiKey, final ObjectType objectType) {
        final AbstractFacet oldestApiDataFacet = apiDataService.getOldestApiDataFacet(apiKey, objectType);
        if (oldestApiDataFacet!=null)
            guestService.setApiKeyAttribute(apiKey,
                                            objectType==null
                                                       ? ApiKeyAttribute.MIN_TIME_KEY
                                                       : objectType.getApiKeyAttributeName(ApiKeyAttribute.MIN_TIME_KEY),
                                            ISODateTimeFormat.dateHourMinuteSecondFraction().withZoneUTC().print(oldestApiDataFacet.start));
        final AbstractFacet latestApiDataFacet = apiDataService.getLatestApiDataFacet(apiKey, objectType);
        if (latestApiDataFacet!=null)
            guestService.setApiKeyAttribute(apiKey,
                                            objectType==null
                                                       ? ApiKeyAttribute.MAX_TIME_KEY
                                                       : objectType.getApiKeyAttributeName(ApiKeyAttribute.MAX_TIME_KEY),
                                            ISODateTimeFormat.dateHourMinuteSecondFraction().withZoneUTC().print(Math.max(latestApiDataFacet.end, latestApiDataFacet.start)));
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
		connectorUpdateService.addApiUpdate(apiKey, objectTypes, then, System.currentTimeMillis() - then, query,
                                            true, 200, null);
	}

	final protected void countFailedApiCall(ApiKey apiKey, int objectTypes,
			long then, String query, String stackTrace,
            Integer httpResponseCode, String reason) {
        StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=countFailedApiCall")
                .append(" connector=" + connector().getName())
                .append(" objectTypes=" + objectTypes)
                .append(" apiKeyId=").append(apiKey.getId())
                .append(" guestId=").append(apiKey.getGuestId())
                .append(" query=").append(query)
                .append(" httpResponseCode=").append(httpResponseCode)
                .append(" reason=\"").append(reason).append("\"")
                .append(" stackTrace=<![CDATA[").append(stackTrace).append("]]>");
        logger.info(sb.toString());
		connectorUpdateService.addApiUpdate(apiKey, objectTypes, then, System.currentTimeMillis() - then, query,
                                            false, httpResponseCode, reason);
	}

    final protected void reportFailedApiCall(ApiKey apiKey, int objectTypes,
                                            long then, String query, String stackTrace, String reason) {
        StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=countFailedApiCall")
                .append(" connector=" + connector().getName())
                .append(" objectTypes=" + objectTypes)
                .append(" apiKeyId=").append(apiKey.getId())
                .append(" guestId=").append(apiKey.getGuestId())
                .append(" time=").append(ISODateTimeFormat.dateTimeNoMillis().print(then))
                .append(" query=").append(query)
                .append(" reason=\"").append(reason).append("\"")
                .append(" stackTrace=<![CDATA[").append(stackTrace).append("]]>");
        logger.info(sb.toString());
    }

    /**
     * Performs and incremental update of the connector
     * @param updateInfo Update information
     * @throws Exception If update fails
     */
	protected abstract void updateConnectorData(UpdateInfo updateInfo)
			throws Exception;

}
