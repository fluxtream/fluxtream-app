package com.fluxtream.services.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;
import com.fluxtream.ApiData;
import com.fluxtream.Configuration;
import com.fluxtream.TimeInterval;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.dao.FacetDao;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.AbstractUserProfile;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Tag;
import com.fluxtream.domain.TagFilter;
import com.fluxtream.domain.metadata.City;
import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.domain.metadata.WeatherInfo;
import com.fluxtream.events.DataReceivedEvent;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.BodyTrackStorageService;
import com.fluxtream.services.EventListenerService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.thirdparty.helpers.WWOHelper;
import com.fluxtream.utils.JPAUtils;
import com.fluxtream.utils.Utils;
import net.sf.json.JSONObject;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Component
public class ApiDataServiceImpl implements ApiDataService {

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
    WWOHelper wwoHelper;

    @Autowired
    ServicesHelper servicesHelper;

    @Autowired
    @Qualifier("AsyncWorker")
    ThreadPoolTaskExecutor executor;

    private static final DateTimeFormatter formatter = DateTimeFormat
            .forPattern("yyyy-MM-dd");

    @Override
	@Transactional(readOnly = false)
	public void cacheApiDataObject(UpdateInfo updateInfo, long start, long end,
			AbstractFacet payload) {
		payload.api = updateInfo.apiKey.getConnector().value();
		payload.objectType = updateInfo.objectTypes;
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
        JPAUtils.execute(em, "apiUpdates.delete.byApiKey", apiKey.getGuestId(), apiKey.getConnector().value(), apiKey.getId());
		if (!apiKey.getConnector().hasFacets())
			return;
		jpaDao.deleteAllFacets(apiKey);
		Class<? extends AbstractUserProfile> userProfileClass = apiKey.getConnector()
				.userProfileClass();
		if (userProfileClass != null
				&& userProfileClass != AbstractUserProfile.class) {
			Query deleteProfileQuery = em.createQuery("DELETE FROM "
					+ userProfileClass.getName());
			deleteProfileQuery.executeUpdate();
		}
	}

    @Override
    public List<AbstractFacet> getApiDataFacets(ApiKey apiKey, ObjectType objectType,
                                                List<String> dates) {
        return jpaDao.getFacetsByDates(apiKey, objectType, dates);
    }

    @Override
    public <T> List<T> getApiDataFacets(final ApiKey apiKey, final ObjectType objectType, final List<String> dates, final Class<T> clazz) {
        return (List<T>) jpaDao.getFacetsByDates(apiKey, objectType, dates);
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
    public <T> List<T> getApiDataFacets(ApiKey apiKey,
                                        ObjectType objectType,
                                        TimeInterval timeInterval,
                                        Class<T> clazz) {
        return (List<T>)jpaDao.getFacetsBetween(apiKey, objectType, timeInterval);
    }

    @Override
    public AbstractFacet getOldestApiDataFacet(ApiKey apiKey, ObjectType objectType){
        return jpaDao.getOldestFacet(apiKey,objectType);
    }

    @Override
    public AbstractFacet getLatestApiDataFacet(ApiKey apiKey, ObjectType objectType){
        return jpaDao.getLatestFacet(apiKey,objectType);
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
		AbstractFacetExtractor facetExtractor = apiData.updateInfo.apiKey
				.getConnector().extractor(objectTypes, beanFactory);
		facetExtractor.setUpdateInfo(updateInfo);
		List<ObjectType> connectorTypes = ObjectType.getObjectTypes(
				apiData.updateInfo.apiKey.getConnector(), objectTypes);
		List<AbstractFacet> newFacets = new ArrayList<AbstractFacet>();
		if (connectorTypes != null) {
			for (ObjectType objectType : connectorTypes) {
				List<AbstractFacet> facets = facetExtractor.extractFacets(
						apiData, objectType);
				for (AbstractFacet facet : facets) {
					AbstractFacet newFacet = persistFacet(facet);
					if (newFacet!=null)
						newFacets.add(newFacet);
				}
			}
		} else {
			List<AbstractFacet> facets = facetExtractor.extractFacets(apiData,
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
        Query query = em.createQuery("SELECT e FROM " + entityName + " e WHERE e.guestId=? AND e.start=? AND e.end=?");
		query.setParameter(1, facet.guestId);
		query.setParameter(2, facet.start);
		query.setParameter(3, facet.end);
		@SuppressWarnings("rawtypes")
		List existing = query.getResultList();
		if (existing.size()>0) {
			logDuplicateFacet(facet);
			return null;
		} else {
            if (facet.hasTags()) {
                persistTags(facet);
            }
            try {
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
    public <T extends AbstractFacet> T createOrReadModifyWrite(
            Class<? extends AbstractFacet> facetClass, FacetQuery query, FacetModifier<T> modifier, Long apiKeyId) {
        //System.out.println("========================================");
        // TODO(rsargent): do we need @Transactional again on class?

        String tableName=getTableName(facetClass);
        Query q = em.createQuery("SELECT e FROM " + tableName + " e WHERE " + query.query);
        for (int i = 0; i < query.args.length; i++) {
            q.setParameter(i+1, query.args[i]);
        }
        T orig;
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
        }

        T modified = modifier.createOrModify(orig, apiKeyId);
        // createOrModify must return passed argument if it is not null
        assert(orig == null || orig == modified);
        assert(false);
        assert (modified != null);
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
			logger.warn(sb.toString());
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

    @Override
    @Transactional(readOnly = false)
    public void addGuestLocations(final long guestId, List<LocationFacet> locationResources) {
        for (LocationFacet locationResource : locationResources) {
            locationResource.guestId = guestId;
            // making the apiKeyId negative will turn the facet into a zombie
            // and make it recognizable for a separate background task to pick it up and update daymetadata,
            // which takes time
            locationResource.apiKeyId = -locationResource.apiKeyId;
            em.persist(locationResource);
        }
    }

    @Override
    @Transactional(readOnly = false)
    public void addGuestLocation(final long guestId, LocationFacet locationResource) {
        locationResource.guestId = guestId;
        // making the apiKeyId negative will turn the facet into a zombie
        // and make it recognizable for a separate background task to pick it up and update daymetadata,
        // which takes time
        locationResource.apiKeyId = -locationResource.apiKeyId;
        locationResource.processed = false;
        em.persist(locationResource);
    }

    @Transactional(readOnly = false)
    public void processLocation(LocationFacet locationResource) {
        // Create query to check for duplicate
        Query query = em.createQuery("SELECT e FROM Facet_Location e WHERE e.guestId=? AND e.start=? AND e.apiKeyId>0");
        query.setParameter(1, locationResource.guestId);
		query.setParameter(2, locationResource.timestampMs);
		@SuppressWarnings("rawtypes")
		List existing = query.getResultList();
        // Only update Day metadata and persist if this location datapoint is not yet present in the DB table
		if (existing.size()==0) {
            // Fill in guestId, timeUpdated, and source.  These were removed in commit
            // 2c8539c76acbbcd2576458c9ea0144d910a5d836 and location data tables using versions between
            // there and here will have all location data assigned to guestId=0.  Connectors updated with
            // those versions need to be removed and re-added or they will have data gaps
            locationResource.timeUpdated=System.currentTimeMillis();
            if(locationResource.source==LocationFacet.Source.GOOGLE_LATITUDE) {
                locationResource.api=Connector.getConnector("google_latitude").value();
            }
            else if(locationResource.source==LocationFacet.Source.RUNKEEPER) {
                locationResource.api=Connector.getConnector("runkeeper").value();
            }
            else {
                locationResource.api=0;// This happens when GuestServiceImpl CheckIn function uses ip lookup
            }

            // Put updateDayMetadata in a try/catch block because we don't want to fail update or
            // fail to persist this datapoint due to some problem in the timezone detection, etc.
            try {
                updateDayMetadata(locationResource.guestId, locationResource.timestampMs, locationResource.latitude, locationResource.longitude);
            } catch(Throwable e) {
                StringBuilder sb = new StringBuilder("module=updateQueue component=apiDataServiceImpl action=addGuestLocation")
                                    .append(" latitude=").append(locationResource.latitude)
                                    .append(" longitude=").append(locationResource.longitude)
                                    .append(" guestId=").append(locationResource.guestId)
                                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
                logger.warn(sb.toString());
            }

            // make apiKeyId "legit" again
            locationResource.apiKeyId = -locationResource.apiKeyId;
            locationResource.processed = true;
            // Persist the location
            // avoid persisting a facet that was just deleted (as a result of an interrupted update for example)
            if (em.find(LocationFacet.class, locationResource.getId())!=null)
                em.merge(locationResource);
        } else {
            locationResource = em.merge(locationResource);
            em.remove(locationResource);
            StringBuilder sb = new StringBuilder("module=updateQueue component=apiDataServiceImpl action=addGuestLocation")
                    .append(" latitude=").append(locationResource.latitude)
                    .append(" longitude=").append(locationResource.longitude)
                    .append(" guestId=").append(locationResource.guestId)
                    .append(" message=\"deleted duplicate locationFacet\"");
            logger.info(sb.toString());
        }
    }

    @Transactional(readOnly = false)
    private void updateDayMetadata(long guestId, long time, float latitude,
                                  float longitude) {
        City city = metadataService.getClosestCity((double)latitude, (double)longitude);
        String date = formatter.withZone(DateTimeZone.forID(city.geo_timezone))
                .print(time);

        DayMetadataFacet info = metadataService.getDayMetadata(guestId, date, true);
        servicesHelper.addCity(info, city);

        TimeZone tz = TimeZone.getTimeZone(info.timeZone);
        List<WeatherInfo> weatherInfo = metadataService.getWeatherInfo(city.geo_latitude,
                                                                       city.geo_longitude, info.date,
                                                                       AbstractFacetVO.toMinuteOfDay(new Date(info.start), tz),
                                                                       AbstractFacetVO.toMinuteOfDay(new Date(info.end), tz));
        wwoHelper.setWeatherInfo(info, weatherInfo);

        em.merge(info);
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteStaleData() throws ClassNotFoundException {
        StaleDataCleanupWorker worker = beanFactory.getBean(StaleDataCleanupWorker.class);
        executor.execute(worker);
    }

    @Override
    @Transactional(readOnly = false)
    public void cleanupStaleData() throws Exception {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
        provider.addIncludeFilter(new AssignableTypeFilter(AbstractFacet.class));
        Set<BeanDefinition> components = provider.findCandidateComponents("com.fluxtream");
        for (BeanDefinition component : components)
        {
            Class cls = Class.forName(component.getBeanClassName());
            if (AbstractFacet.class.isAssignableFrom(cls)) {
                final String entityName = JPAUtils.getEntityName(cls);
                System.out.println("cleaning up " + entityName + "...");
                if (entityName.startsWith("Facet_")) {
                    if (!JPAUtils.hasRelation(cls)) {
                        Query query = em
                                .createNativeQuery("DELETE FROM " + entityName + " WHERE apiKeyId NOT IN (SELECT DISTINCT id from ApiKey);");
                        final int i = query.executeUpdate();
                        StringBuilder sb = new StringBuilder("module=updateQueue component=apiDataServiceImpl action=deleteStaleData")
                                .append(" facetTable=").append(entityName).append(" facetsDeleted=").append(i);
                        logger.info(sb.toString());
                    } else {
                        Query query = em
                                .createNativeQuery("SELECT * FROM " + entityName + " WHERE apiKeyId NOT IN (SELECT DISTINCT id from ApiKey);", cls);
                        final List<?extends AbstractFacet> facetsToDelete = query.getResultList();
                        final int i = facetsToDelete.size();
                        if (i>0) {
                            for (AbstractFacet facet : facetsToDelete) {
                                em.remove(facet);
                            }
                            StringBuilder sb = new StringBuilder("module=updateQueue component=apiDataServiceImpl action=deleteStaleData")
                                    .append(" facetTable=").append(entityName).append(" facetsDeleted=").append(i);
                            logger.info(sb.toString());
                        }
                    }
                    em.flush();
                }
            }
        }
        Query query = em
                .createNativeQuery("DELETE FROM ApiUpdates WHERE apiKeyId NOT IN (SELECT DISTINCT id from ApiKey);");
        final int i = query.executeUpdate();
        StringBuilder sb = new StringBuilder("module=updateQueue component=apiDataServiceImpl action=deleteStaleData")
                .append(" facetTable=ApiUpdates").append(" facetsDeleted=").append(i);
        logger.info(sb.toString());
    }

}
