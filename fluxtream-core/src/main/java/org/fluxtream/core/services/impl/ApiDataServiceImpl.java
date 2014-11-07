package org.fluxtream.core.services.impl;

import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.fluxtream.core.ApiData;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.dao.FacetDao;
import org.fluxtream.core.connectors.location.LocationFacet;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.connectors.vos.AbstractFacetVO;
import org.fluxtream.core.domain.*;
import org.fluxtream.core.events.DataReceivedEvent;
import org.fluxtream.core.facets.extractors.AbstractFacetExtractor;
import org.fluxtream.core.metadata.DayMetadata;
import org.fluxtream.core.services.*;
import org.fluxtream.core.utils.JPAUtils;
import org.fluxtream.core.utils.TimeUtils;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Component
public class ApiDataServiceImpl implements ApiDataService, DisposableBean {

	static FlxLogger logger = FlxLogger.getLogger(ApiDataServiceImpl.class);
    private static final FlxLogger LOG_DEBUG = FlxLogger.getLogger("Fluxtream");

	@Autowired
	Configuration env;

	@PersistenceContext
	EntityManager em;

	@Autowired
	DataSource dataSource;

	@Autowired
	GuestService guestService;

    @Autowired
	FacetDao jpaDao;

	@Autowired
	BeanFactory beanFactory;

    @Autowired
	BodyTrackStorageService bodyTrackStorageService;

    @Autowired
    MetadataService metadataService;

    @Autowired
    EventListenerService eventListenerService;

    @Autowired
    @Qualifier("AsyncWorker")
    ThreadPoolTaskExecutor executor;

    @Autowired
    SettingsService settingsService;

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public AbstractFacetVO<AbstractFacet> getFacet(final int api, final int objectType, final long facetId) {
        Connector connector = Connector.fromValue(api);
        final ObjectType ot = ObjectType.getObjectType(connector, objectType);
        final AbstractFacet facet = em.find(ot.facetClass(), facetId);
        final GuestSettings guestSettings = settingsService.getSettings(facet.guestId);
        final TimeZone timeZone = metadataService.getTimeZone(facet.guestId, facet.start);
        final String date = TimeUtils.dateFormatter.withZone(DateTimeZone.forTimeZone(timeZone)).print(facet.start);
        final DayMetadata dayMetadata = metadataService.getDayMetadata(facet.guestId, date);
        try {
            final AbstractFacetVO<AbstractFacet> vo = AbstractFacetVO.getFacetVOClass((AbstractFacet)facet).newInstance();
            vo.extractValues(facet, dayMetadata.getTimeInterval(), guestSettings);
            return vo;
        }
        catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }
    }

    @Override
	@Transactional(readOnly = false)
	public void cacheApiDataObject(UpdateInfo updateInfo, long start, long end,
			AbstractFacet payload) {
		payload.api = updateInfo.apiKey.getConnector().value();
		payload.guestId = updateInfo.apiKey.getGuestId();
		payload.timeUpdated = System.currentTimeMillis();

        final AbstractFacet facet = persistFacet(payload);
        List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        facets.add(facet);
        fireDataReceivedEvent(updateInfo, updateInfo.objectTypes(), start, end, facets);
	}

	/**
	 * start and end parameters allow to specify time boundaries that are not
	 * contained in the connector data itself
	 */
	@Override
	@Transactional(readOnly = false)
	public void cacheApiDataJSON(UpdateInfo updateInfo, JSONObject jsonObject,
			long start, long end) throws Exception {
		ApiData apiData = new ApiData(updateInfo, start, end);
		apiData.jsonObject = jsonObject;
        final List<AbstractFacet> facets = extractFacets(apiData, updateInfo.objectTypes, updateInfo);
        fireDataReceivedEvent(updateInfo, updateInfo.objectTypes(), start, end, facets);
	}

    /**
     * start and end parameters allow to specify time boundaries that are not
     * contained in the connector data itself
     */
    @Override
    @Transactional(readOnly = false)
    public void cacheApiDataJSON(UpdateInfo updateInfo, String json,
                                 long start, long end, int objectTypes) throws Exception {
        ApiData apiData = new ApiData(updateInfo, start, end);
        apiData.json = json;
        final List<AbstractFacet> facets = extractFacets(apiData, objectTypes, updateInfo);
        final List<ObjectType> types = ObjectType.getObjectTypes(updateInfo.apiKey.getConnector(), objectTypes);
        fireDataReceivedEvent(updateInfo, types, start, end, facets);
    }

	/**
	 * start and end parameters allow to specify time boundaries that are not
	 * contained in the connector data itself
	 */
	@Override
	@Transactional(readOnly = false)
	public void cacheApiDataJSON(UpdateInfo updateInfo, String json,
			long start, long end) throws Exception {
		ApiData apiData = new ApiData(updateInfo, start, end);
		apiData.json = json;
        if (updateInfo.objectTypes==0)
            throw new RuntimeException("ObjectType=0! cacheApiDataJSON is called from an 'Autonomous' " +
                                       "Updater -> you need to call the cacheApiDataJSON method with the " +
                                       "extra 'objectTypes' parameter!");
        final List<AbstractFacet> facets = extractFacets(apiData, updateInfo.objectTypes, updateInfo);
        fireDataReceivedEvent(updateInfo, updateInfo.objectTypes(), start, end, facets);
	}

    private void fireDataReceivedEvent(final UpdateInfo updateInfo, List<ObjectType> objectTypes,
                                       final long start, final long end, List<AbstractFacet> facets) {
        DataReceivedEvent dataReceivedEvent = new DataReceivedEvent(updateInfo,
                                                                    objectTypes,
                                                                    start, end, facets);
        // date-based connectors may attach a "date" String (yyyy-MM-dd) to the updateInfo object
        if (updateInfo.getContext("date")!=null)
            dataReceivedEvent.date = (String) updateInfo.getContext("date");
        eventListenerService.fireEvent(dataReceivedEvent);
    }

	/**
	 * start and end parameters allow to specify time boundaries that are not
	 * contained in the connector data itself
	 */
	@Override
	@Transactional(readOnly = false)
	public void cacheApiDataXML(UpdateInfo updateInfo, String xml, long start,
			long end) throws Exception {
		ApiData apiData = new ApiData(updateInfo, start, end);
		apiData.xml = xml;
		extractFacets(apiData, updateInfo.objectTypes, null);
	}

	@Override
	@Transactional(readOnly = false)
	public void eraseApiData(ApiKey apiKey, int objectTypes) {
		if (!apiKey.getConnector().hasFacets())
			return;
		if (objectTypes == -1)
			eraseApiData(apiKey);
		else {
			List<ObjectType> connectorTypes = ObjectType.getObjectTypes(
					apiKey.getConnector(), objectTypes);
			for (ObjectType connectorType : connectorTypes) {
				jpaDao.deleteAllFacets(apiKey, connectorType);
			}
		}
	}

	@Override
	@Transactional(readOnly = false)
	public void eraseApiData(ApiKey apiKey,
			ObjectType objectType) {
		if (objectType == null)
			eraseApiData(apiKey);
		else
			jpaDao.deleteAllFacets(apiKey, objectType);
	}

    @Override
    @Transactional(readOnly = false)
    public void eraseApiData(ApiKey apiKey,
                             final int objectTypes, final TimeInterval timeInterval) {
        List<ObjectType> connectorTypes = ObjectType.getObjectTypes(apiKey.getConnector(),
                                                                    objectTypes);
        if (connectorTypes!=null) {
            for (ObjectType objectType : connectorTypes) {
                eraseApiData(apiKey, objectType, timeInterval);
            }
        } else
            eraseApiData(apiKey, null, timeInterval);
    }

    @Override
	@Transactional(readOnly = false)
	public void eraseApiData(ApiKey apiKey,
			ObjectType objectType, TimeInterval timeInterval) {
		List<AbstractFacet> facets = getApiDataFacets(apiKey, objectType,
				timeInterval);
		if (facets != null) {
			for (AbstractFacet facet : facets)
				em.remove(facet);
		}
	}

    @Override
    @Transactional(readOnly = false)
    public void eraseApiData(ApiKey apiKey,
                             ObjectType objectType, List<String> dates) {
        final List<AbstractFacet> facets = jpaDao.getFacetsByDates(apiKey, objectType, dates);
        if (facets != null) {
            for (AbstractFacet facet : facets)
                em.remove(facet);
        }
    }

	@Override
	@Transactional(readOnly = false)
	public void eraseApiData(ApiKey apiKey) {
        JPAUtils.execute(em, "apiUpdates.delete.byApiKey", apiKey.getId());
		if (!apiKey.getConnector().hasFacets())
			return;
		jpaDao.deleteAllFacets(apiKey);
        guestService.deleteConnectorProfile(apiKey);
        // remove directory <connectorData.location>/<connectorName>/<apiKeyId>
        final String devKvsLocation = env.get("btdatastore.db.location");
        // let's not assume that everyone has set this value
        if (devKvsLocation!=null) {
            if (apiKey.getConnector()!=null&&apiKey.getConnector().getName()!=null) {
                final String connectorName = apiKey.getConnector().getPrettyName();
                StringBuilder path = new StringBuilder(devKvsLocation)
                        .append(File.separator).append(apiKey.getGuestId())
                        .append(File.separator).append(connectorName)
                        .append(File.separator).append(apiKey.getId());
                File dataDir = new File(path.toString());
                if (dataDir.exists()) {
                    try {
                        FileUtils.deleteDirectory(dataDir);
                    }
                    catch (IOException e) {
                        logger.warn("Couldn't delete connector data directory at [" + dataDir.getAbsolutePath() + "]");
                    }
                }
            }
        }
        try {
            JPAUtils.execute(em, "channelMapping.delete.byApiKeyId", apiKey.getId());
        } catch(Exception e) {logger.warn("Couldn't delete Channel Mappings for apiKeyId="
                                          + apiKey.getId()
                                          + "\n" + ExceptionUtils.getStackTrace(e));}
        if (apiKey.getConnector()!=null) {
            bodyTrackHelper.deleteStyle(apiKey.getGuestId(), apiKey.getConnector().prettyName());
        }
    }

    @Override
    public List<AbstractFacet> getApiDataFacets(ApiKey apiKey, ObjectType objectType,
                                                List<String> dates) {
        return jpaDao.getFacetsByDates(apiKey, objectType, dates);
    }

    @Override
    public List<AbstractRepeatableFacet> getApiDataFacets(final ApiKey apiKey, final ObjectType objectType, final String startDate, final String endDate) {
        return jpaDao.getFacetsBetweenDates(apiKey, objectType, startDate, endDate);
    }

    @Override
    public List<AbstractFacet> getApiDataFacets(ApiKey apiKey, ObjectType objectType, TimeInterval timeInterval) {
        return getApiDataFacets(apiKey, objectType, timeInterval, (TagFilter)null);
    }

    @Override
    public List<AbstractFacet> getApiDataFacets(final ApiKey apiKey,
                                                final ObjectType objectType,
                                                final TimeInterval timeInterval,
                                                @Nullable final TagFilter tagFilter) {
        return jpaDao.getFacetsBetween(apiKey, objectType, timeInterval, tagFilter);
    }

    @Override
    public List<AbstractFacet> getApiDataFacets(ApiKey apiKey,
                                                ObjectType objectType,
                                                TimeInterval timeInterval,
                                                @Nullable TagFilter tagFilter,
                                                @Nullable String orderByString) {
        return jpaDao.getFacetsBetween(apiKey,objectType,timeInterval,tagFilter,orderByString);
    }

    @Override
    public AbstractFacet getOldestApiDataFacet(ApiKey apiKey, ObjectType objectType){
        return jpaDao.getOldestFacet(apiKey, objectType);
    }

    @Override
    public AbstractFacet getLatestApiDataFacet(ApiKey apiKey, ObjectType objectType){
        return jpaDao.getLatestFacet(apiKey, objectType);
    }

    @Override
    public List<AbstractFacet> getApiDataFacetsBefore(ApiKey apiKey, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        return getApiDataFacetsBefore(apiKey, objectType, timeInMillis, desiredCount, null);
    }

    @Override
    public List<AbstractFacet> getApiDataFacetsBefore(final ApiKey apiKey,
                                                      final ObjectType objectType,
                                                      final long timeInMillis,
                                                      final int desiredCount,
                                                      @Nullable final TagFilter tagFilter) {
        return jpaDao.getFacetsBefore(apiKey, objectType, timeInMillis, desiredCount, tagFilter);
    }

    @Override
    public List<AbstractFacet> getApiDataFacetsAfter(ApiKey apiKey, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        return getApiDataFacetsAfter(apiKey, objectType, timeInMillis, desiredCount, null);
    }

    @Override
    public List<AbstractFacet> getApiDataFacetsAfter(final ApiKey apiKey,
                                                     final ObjectType objectType,
                                                     final long timeInMillis,
                                                     final int desiredCount,
                                                     @Nullable final TagFilter tagFilter) {
        return jpaDao.getFacetsAfter(apiKey, objectType, timeInMillis, desiredCount, tagFilter);
    }

    public AbstractFacet getFacetById(ApiKey apiKey, final ObjectType objectType, final long facetId) {
        return jpaDao.getFacetById(apiKey, objectType, facetId);
    }

	@Transactional(readOnly = false)
	private List<AbstractFacet> extractFacets(ApiData apiData, int objectTypes,
			UpdateInfo updateInfo) throws Exception {
        List<AbstractFacet> newFacets = new ArrayList<AbstractFacet>();
		AbstractFacetExtractor facetExtractor = apiData.updateInfo.apiKey
				.getConnector().extractor(objectTypes, beanFactory);
        if (facetExtractor==null)
            return newFacets;

		List<ObjectType> connectorTypes = ObjectType.getObjectTypes(
				apiData.updateInfo.apiKey.getConnector(), objectTypes);
		if (connectorTypes != null) {
			for (ObjectType objectType : connectorTypes) {
				List<AbstractFacet> facets = facetExtractor.extractFacets(updateInfo,
						apiData, objectType);
				for (AbstractFacet facet : facets) {
					AbstractFacet newFacet = persistFacet(facet);
					if (newFacet!=null)
						newFacets.add(newFacet);
				}
			}
		} else {
			List<AbstractFacet> facets = facetExtractor.extractFacets(updateInfo, apiData,
					null);
			for (AbstractFacet facet : facets) {
				AbstractFacet newFacet = persistFacet(facet);
				if (newFacet!=null)
					newFacets.add(newFacet);
			}
		}
		bodyTrackStorageService.storeApiData(updateInfo.getGuestId(), newFacets);
        return newFacets;
	}

	private static Map<String, String> facetEntityNames = new ConcurrentHashMap<String,String>();

    @Transactional(readOnly = false)
	public AbstractFacet persistFacet(AbstractFacet facet) {
		String entityName = facetEntityNames.get(facet.getClass().getName());
		if (entityName==null) {
			Entity entityAnnotation = facet.getClass().getAnnotation(Entity.class);
			entityName = entityAnnotation.name();
			facetEntityNames.put(facet.getClass().getName(), entityName);
		}
        Query query = em.createQuery("SELECT e FROM " + entityName + " e WHERE e.guestId=? AND e.start=? AND e.end=? AND e.apiKeyId=?");
		query.setParameter(1, facet.guestId);
		query.setParameter(2, facet.start);
		query.setParameter(3, facet.end);
        query.setParameter(4, facet.apiKeyId);
        List existing = null;
        try {
            existing = query.getResultList();
        } catch (Throwable t) {
            t.printStackTrace();
        }

		if (existing.size()>0) {
			logDuplicateFacet(facet);
			return null;
		} else {
            if (facet.hasTags()) {
                persistTags(facet);
            }
            try {
                facet.timeUpdated = DateTimeUtils.currentTimeMillis();
                em.persist(facet);
                StringBuilder sb = new StringBuilder("module=updateQueue component=apiDataServiceImpl action=persistFacet")
                        .append(" connector=").append(Connector.fromValue(facet.api).getName())
                        .append(" objectType=").append(facet.objectType)
                        .append(" guestId=").append(facet.guestId);
                logger.debug(sb.toString());
            } catch (Throwable t) {
                StringBuilder sb = new StringBuilder("module=updateQueue component=apiDataServiceImpl action=persistFacet")
                        .append(" connector=").append(Connector.fromValue(facet.api).getName())
                        .append(" objectType=").append(facet.objectType)
                        .append(" guestId=").append(facet.guestId)
                        .append(" message=\"" + t.getMessage() + "\"");
                logger.warn(sb.toString());
            }
            persistExistingFacet(facet);
            StringBuilder sb = new StringBuilder("module=updateQueue component=apiDataServiceImpl action=persistFacet")
                    .append(" connector=").append(Connector.fromValue(facet.api).getName())
                    .append(" objectType=").append(facet.objectType)
                    .append(" guestId=").append(facet.guestId);
            logger.info(sb.toString());
			return facet;
		}
	}

    public void persistExistingFacet(final AbstractFacet facet) {
        if (facet != null) {
            if (facet.hasTags()) {
                persistTags(facet);
            }
			em.persist(facet);
        }
    }

    String getTableName(Class<? extends AbstractFacet> cls) {
        String entityName = facetEntityNames.get(cls.getName());
        if (entityName==null) {
            Entity entityAnnotation = cls.getAnnotation(Entity.class);
            entityName = entityAnnotation.name();
            facetEntityNames.put(cls.getName(), entityName);
        }
        return entityName;
    }

    // Takes existing or new facet and updates or inserts into database, respectively.
    //
    // Also adjusts time of facet to match inferred timezone, if the facet only knows local time (floating time zone)
    // Also saves tags
    //
    // Workflow for using mergeFacet:
    // Check for existing facet using getEntityManager().query()
    // If facet already exists, modify
    // If no facet already exists, create a new one and fill its fields
    // Call mergeFacet with the updated or new facet
    //

    @Override
    @Transactional(readOnly = false)
    public <T extends AbstractFacet> T createOrReadModifyWrite (
            Class<? extends AbstractFacet> facetClass, FacetQuery query, FacetModifier<T> modifier, Long apiKeyId) throws Exception {
        //System.out.println("========================================");
        // TODO(rsargent): do we need @Transactional again on class?

        String tableName=getTableName(facetClass);
        final String qlString = "SELECT e FROM " + tableName + " e WHERE " + query.query;
        Query q = em.createQuery(qlString);
        for (int i = 0; i < query.args.length; i++) {
            q.setParameter(i+1, query.args[i]);
        }
        T orig = null;
        try {
            @SuppressWarnings("unchecked")
            T x = orig = (T) q.getSingleResult();
            //System.out.println("====== Facet found, id=" + orig.getId());
            // orig is managed by the session and any changes will be written at the end of the transaction
            // (which is the end of this member function)
            assert(em.contains(orig));
        } catch (javax.persistence.NoResultException ignored) {
            orig = null;
            //System.out.println("====== Didn't find facet;  need to create new one");
        } catch (NonUniqueResultException nonUnique) {
            // pretend everything's OK
            orig = (T) q.getResultList().get(0);
            logger.info("WARNING: non unique exception here, query: " + qlString);
        }

        T modified = modifier.createOrModify(orig, apiKeyId);
        // createOrModify must return passed argument if it is not null
        // If the passed argument is null and the attempt to parse the
        // new data into a valid facet fails, createOrModify may return null.
        // In that case, just don't try to persist it.
        assert(orig == null || orig == modified);
        if(modified == null)
           return null;

        //System.out.println("====== after modify, contained?: " + em.contains(modified));
        if (orig == null) {
            // Persist the newly-created facet (and its tags, if any)
            persistExistingFacet(modified);
            //System.out.println("====== after persist, contained?: " + em.contains(modified));
        } else {
            if (modified.hasTags()) {
                persistTags(modified);
            }
        }
        assert(em.contains(modified));
        //System.out.println("========================================");
        return modified;
    }

    // Each user has a set of all tags.  persistTags makes sure this set of all tags includes the tags
    // from this facet
    @Transactional(readOnly=false)
    private void persistTags(final AbstractFacet facet) {
        for (Tag tag : facet.getTags()) {
            Tag guestTag = JPAUtils.findUnique(em, Tag.class, "tags.byName", facet.guestId, tag.name);
            if (guestTag==null) {
                guestTag = new Tag();
                guestTag.guestId = facet.guestId;
                guestTag.name = tag.name;
                StringBuilder sb = new StringBuilder("module=updateQueue component=apiDataServiceImpl action=persistTags")
                        .append(" connector=").append(Connector.fromValue(facet.api).getName())
                        .append(" objectType=").append(facet.objectType)
                        .append(" guestId=").append(facet.guestId)
                        .append(" tag=").append(tag.name);
                logger.info(sb.toString());
                em.persist(guestTag);
            }
        }
    }

    private void logDuplicateFacet(AbstractFacet facet) {
		try {
			Connector connector = Connector.fromValue(facet.api);
			StringBuilder sb = new StringBuilder("module=updateQueue component=apiDataServiceImpl action=logDuplicateFacet")
                    .append(" connector=" + connector.getName())
                    .append(" objectType=").append(facet.objectType)
                    .append(" guestId=" + facet.guestId);
			if (facet.objectType!=-1) {
				ObjectType[] objectType = connector.getObjectTypesForValue(facet.objectType);
				if (objectType!=null&&objectType.length!=0)
					sb.append(" objectType=").append(objectType[0].getName());
				else if (objectType!=null && objectType.length>1) {
					sb.append(" objectType=[");
					for (int i = 0; i < objectType.length; i++) {
						ObjectType type = objectType[i];
						if (i>0) sb.append(",");
						sb.append(type.getName());
					}
					sb.append("]");
				}
			}
			logger.info(sb.toString());
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
    @Transactional(readOnly = false)
	public void cacheEmptyData(UpdateInfo updateInfo, long fromMidnight,
			long toMidnight) {
		Connector connector = updateInfo.apiKey.getConnector();
		List<ObjectType> objectTypes = ObjectType.getObjectTypes(connector,
				updateInfo.objectTypes);
		for (ObjectType objectType : objectTypes) {
			try {
				AbstractFacet facet = objectType.facetClass().newInstance();
				facet.isEmpty = true;
				facet.start = fromMidnight;
				facet.end = toMidnight;
				em.persist(facet);
			} catch (Exception e) {
				// this should really never happen
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

    // addGuestLocation persists the location and adds it to the visited cities
    // table.  The persistence does duplicate detection by checking for locations matching the time, source,
    // and apiKeyId.  In the case of a duplicate the new locationFacet is not persisted.
    @Override
    @Transactional(readOnly = false)
    public void addGuestLocations(final long guestId, List<LocationFacet> locationResources) {
        // Only call updateLocationMetadata for the subset of locations which we did not already know about.
        // Keep track of which locations are new in newLocations.
        List<LocationFacet> newLocations = new ArrayList<LocationFacet>();
        for (LocationFacet locationResource : locationResources) {
            locationResource.guestId = guestId;
            if(persistLocation(locationResource)) {
                newLocations.add(locationResource);
            }
        }
        metadataService.updateLocationMetadata(guestId, newLocations);
    }

    // addGuestLocations persists a list of locations and adds them to the visited cities
    // table.  The persistence does duplicate detection by checking for locations matching the time, source,
    // and apiKeyId.  In the case of a duplicate the new locationFacet is not persisted.

    @Override
    @Transactional(readOnly = false)
    public void addGuestLocation(final long guestId, LocationFacet locationResource) {
        locationResource.guestId = guestId;
        // Persist the location and find out if it is new or not.  Only call updateLocationMetadata
        // if we did not already know about this location
        if(persistLocation(locationResource)) {
            List<LocationFacet> locationResources = new ArrayList<LocationFacet>();
            locationResources.add(locationResource);
            metadataService.updateLocationMetadata(guestId, locationResources);
        }
    }

    @Transactional(readOnly = false)
    public boolean persistLocation(LocationFacet locationResource) {
        // Create query to check for duplicate, return true if this is a new
        // location; false otherwise
        Query query = em.createQuery("SELECT e FROM Facet_Location e WHERE e.guestId=? AND e.start=? AND e.source=? AND e.apiKeyId=?");
        query.setParameter(1, locationResource.guestId);
		query.setParameter(2, locationResource.timestampMs);
        query.setParameter(3, locationResource.source);
        query.setParameter(4, locationResource.apiKeyId);
		@SuppressWarnings("rawtypes")
		List existing = query.getResultList();
        // Only persist if this location datapoint is not yet present in the DB table
		if (existing.size()==0) {
            // This is a new location, persist it
            em.persist(locationResource);
            return true;
       } else {
            // This is a duplicate location, ignore and print a message.
            // TODO: consider what we should do if the new one differs from the
            // stored location.  Should we do a merge?
            //StringBuilder sb = new StringBuilder("module=updateQueue component=apiDataServiceImpl action=addGuestLocation")
            //        .append(" guestId=").append(locationResource.guestId)
            //        .append(" source=").append(locationResource.source.toString())
            //        .append(" apiKeyId=").append(locationResource.apiKeyId)
            //        .append(" start=").append(locationResource.start)
            //        .append(" latitude=").append(locationResource.latitude)
            //        .append(" longitude=").append(locationResource.longitude)
            //        .append(" message=\"ignoring duplicate locationFacet\"");
            //logger.info(sb.toString());
            return false;
        }
    }

    @Override
    @Transactional(readOnly=false)
    public void setComment(final String connectorName, final String objectTypeName, final long guestId, final long facetId,
                           final String comment) {
        final AbstractFacet facet = getFacet(connectorName, objectTypeName, facetId);
        if (facet==null)
            throw new RuntimeException("No such facet (connectorName: " + connectorName + ", objectTypeName: " + objectTypeName + ", guestId: " + guestId + ", facetId: " + facetId);
        if (facet.guestId!=guestId)
            throw new RuntimeException("Facet doesn't have the expected guestId (expected: " + guestId + ", actual: " + facet.guestId + ")");
        facet.comment = comment;
        em.persist(facet);
    }

    private AbstractFacet getFacet(final String connectorName, final String objectTypeName, final long facetId) {
        final Connector connector = Connector.getConnector(connectorName);
        Class<? extends AbstractFacet> facetClass = connector.facetClass();
        if (objectTypeName!=null) {
            final ObjectType objectType = ObjectType.getObjectType(connector, objectTypeName);
            facetClass = objectType.facetClass();
        }

        final TypedQuery<AbstractFacet> query = em.createQuery("SELECT facet FROM " + facetClass.getName() + " facet WHERE id=?", AbstractFacet.class);
        query.setParameter(1, facetId);
        final List<AbstractFacet> resultList = query.getResultList();
        if (resultList.size()>0)
            return resultList.get(0);
        else return null;
    }

    @Override
    @Transactional(readOnly=false)
    public void deleteComment(final String connectorName, final String objectTypeName, final long guestId, final long facetId) {
        final AbstractFacet facet = getFacet(connectorName, objectTypeName, facetId);
        if (facet==null)
            throw new RuntimeException("No such facet (connectorName: " + connectorName + ", objectTypeName: " + objectTypeName + ", guestId: " + guestId + ", facetId: " + facetId);
        if (facet.guestId!=guestId)
            throw new RuntimeException("Facet doesn't have the expected guestId (expected: " + guestId + ", actual: " + facet.guestId + ")");
        facet.comment = null;
        em.persist(facet);
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteStaleData() throws ClassNotFoundException {
        StaleDataCleanupWorker worker = beanFactory.getBean(StaleDataCleanupWorker.class);
        executor.execute(worker);
    }

    @Override
    public void destroy() throws Exception {
        executor.shutdown();
    }
}
