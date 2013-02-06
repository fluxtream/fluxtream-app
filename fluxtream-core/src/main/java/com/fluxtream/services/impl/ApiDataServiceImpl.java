package com.fluxtream.services.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
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
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.dao.FacetDao;
import com.fluxtream.connectors.google_latitude.LocationFacet;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.AbstractFloatingTimeZoneFacet;
import com.fluxtream.domain.AbstractUserProfile;
import com.fluxtream.domain.Tag;
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
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Component
public class ApiDataServiceImpl implements ApiDataService {

	static Logger logger = Logger.getLogger(ApiDataServiceImpl.class);

	@Autowired
	Configuration env;

	@PersistenceContext
	EntityManager em;

	@Autowired
	DataSource dataSource;

	@Autowired
	GuestService guestService;

    @Qualifier("JPAFacetDao")
    @Autowired
	FacetDao jpaDao;

	@Autowired
	BeanFactory beanFactory;

    @Qualifier("bodyTrackStorageServiceImpl")
    @Autowired
	BodyTrackStorageService bodyTrackStorageService;

    @Qualifier("metadataServiceImpl")
    @Autowired
    MetadataService metadataService;

    @Autowired
    EventListenerService eventListenerService;

    @Autowired
    WWOHelper wwoHelper;

    @Autowired
    ServicesHelper servicesHelper;

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

        persistFacet(payload);
        fireDataReceivedEvent(updateInfo, updateInfo.objectTypes(), start, end);
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
		extractFacets(apiData, updateInfo.objectTypes, updateInfo);
        fireDataReceivedEvent(updateInfo, updateInfo.objectTypes(), start, end);
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
        extractFacets(apiData, objectTypes, updateInfo);
        final List<ObjectType> types = ObjectType.getObjectTypes(updateInfo.apiKey.getConnector(), objectTypes);
        fireDataReceivedEvent(updateInfo, types, start, end);
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
		extractFacets(apiData, updateInfo.objectTypes, updateInfo);
        fireDataReceivedEvent(updateInfo, updateInfo.objectTypes(), start, end);
	}

    private void fireDataReceivedEvent(final UpdateInfo updateInfo, List<ObjectType> objectTypes,
                                       final long start, final long end) {
        DataReceivedEvent dataReceivedEvent = new DataReceivedEvent(updateInfo.getGuestId(),
                                                                    updateInfo.apiKey.getConnector(),
                                                                    objectTypes,
                                                                    start, end);
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
	public void cacheApiDataXML(UpdateInfo updateInfo, Document xmlDocument,
			long start, long end) throws Exception {
		ApiData apiData = new ApiData(updateInfo, start, end);
		apiData.xmlDocument = xmlDocument;
		extractFacets(apiData, updateInfo.objectTypes, null);
        fireDataReceivedEvent(updateInfo, updateInfo.objectTypes(), start, end);
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
	public void eraseApiData(long guestId, Connector connector, int objectTypes) {
		if (!connector.hasFacets())
			return;
		if (objectTypes == -1)
			eraseApiData(guestId, connector);
		else {
			List<ObjectType> connectorTypes = ObjectType.getObjectTypes(
					connector, objectTypes);
			for (ObjectType connectorType : connectorTypes) {
				jpaDao.deleteAllFacets(connector, connectorType, guestId);
			}
		}
	}

	@Override
	@Transactional(readOnly = false)
	public void eraseApiData(long guestId, Connector connector,
			ObjectType objectType) {
		if (objectType == null)
			eraseApiData(guestId, connector);
		else
			jpaDao.deleteAllFacets(connector, objectType, guestId);
	}

    @Override
    public void eraseApiData(final long guestId, final Connector connector,
                             final int objectTypes, final TimeInterval timeInterval) {
        List<ObjectType> connectorTypes = ObjectType.getObjectTypes(connector,
                                                                    objectTypes);
        if (connectorTypes!=null) {
            for (ObjectType objectType : connectorTypes) {
                eraseApiData(guestId, connector, objectType, timeInterval);
            }
        } else
            eraseApiData(guestId, connector, null, timeInterval);
    }

    @Override
	@Transactional(readOnly = false)
	// TODO: make a named query that works for all api objects
	public void eraseApiData(long guestId, Connector api,
			ObjectType objectType, TimeInterval timeInterval) {
		List<AbstractFacet> facets = getApiDataFacets(guestId, api, objectType,
				timeInterval);
		if (facets != null) {
			for (AbstractFacet facet : facets)
				em.remove(facet);
		}
	}

    @Override
    @Transactional(readOnly = false)
    public void eraseApiData(long guestId, Connector connector,
                             ObjectType objectType, List<String> dates) {
        final List<AbstractFacet> facets = jpaDao.getFacetsByDates(connector, guestId, objectType, dates);
        if (facets != null) {
            for (AbstractFacet facet : facets)
                em.remove(facet);
        }
    }

	@Override
	@Transactional(readOnly = false)
	public void eraseApiData(long guestId, Connector connector) {
		if (!connector.hasFacets())
			return;
		jpaDao.deleteAllFacets(connector, guestId);
		Class<? extends AbstractUserProfile> userProfileClass = connector
				.userProfileClass();
		if (userProfileClass != null
				&& userProfileClass != AbstractUserProfile.class) {
			Query deleteProfileQuery = em.createQuery("DELETE FROM "
					+ userProfileClass.getName());
			deleteProfileQuery.executeUpdate();
		}
	}

    @Override
    public List<AbstractFacet> getApiDataFacets(long guestId,
                                                Connector connector, ObjectType objectType,
                                                List<String> dates) {
        return jpaDao.getFacetsByDates(connector,
                                       guestId, objectType, dates);
    }

	@Override
	public List<AbstractFacet> getApiDataFacets(long guestId,
			Connector connector, ObjectType objectType,
			TimeInterval timeInterval) {
        return jpaDao.getFacetsBetween(connector,
                guestId, objectType, timeInterval);
	}

    @Override
    public AbstractFacet getOldestApiDataFacet(long guestId, Connector connector, ObjectType objectType){
        return jpaDao.getOldestFacet(connector,guestId,objectType);
    }

    @Override
    public AbstractFacet getLatestApiDataFacet(long guestId, Connector connector, ObjectType objectType){
        return jpaDao.getLatestFacet(connector,guestId,objectType);
    }

    @Override
    public List<AbstractFacet> getApiDataFacetsBefore(final long guestId, final Connector connector, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        return jpaDao.getFacetsBefore(guestId, connector, objectType, timeInMillis, desiredCount);
    }

    @Override
    public List<AbstractFacet> getApiDataFacetsAfter(final long guestId, final Connector connector, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        return jpaDao.getFacetsAfter(guestId, connector, objectType, timeInMillis, desiredCount);
    }


	@Transactional(readOnly = false)
	private void extractFacets(ApiData apiData, int objectTypes,
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
        if (facet instanceof AbstractFloatingTimeZoneFacet) {
            final AbstractFloatingTimeZoneFacet aftzFacet = (AbstractFloatingTimeZoneFacet)facet;
            final TimeZone localTimeZone = metadataService.getTimeZone(facet.guestId, aftzFacet.date);
            try {
                aftzFacet.updateTimeInfo(localTimeZone);
            } catch (Throwable e) {
                final String message = new StringBuilder("Could not parse floating " +
                                                         "timezone facet's time storage: (startTimeStorage=")
                        .append(aftzFacet.startTimeStorage)
                        .append(", endTimeStorage=")
                        .append(aftzFacet.endTimeStorage)
                        .append(")").toString();
                StringBuilder sb = new StringBuilder("module=updateQueue component=apiDataServiceImpl action=persistFacet")
                        .append(" connector=").append(Connector.fromValue(facet.api).getName())
                        .append(" objectType=").append(facet.objectType)
                        .append(" guestId=").append(facet.guestId)
                        .append(" message=\"Couldn't update updateTimeInfo\"")
                        .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
                logger.warn(sb.toString());
                throw new RuntimeException(message);
            }
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
			em.persist(facet);
            StringBuilder sb = new StringBuilder("module=updateQueue component=apiDataServiceImpl action=persistFacet")
                    .append(" connector=").append(Connector.fromValue(facet.api).getName())
                    .append(" objectType=").append(facet.objectType)
                    .append(" guestId=").append(facet.guestId);
            logger.info(sb.toString());
			return facet;
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
            Class<? extends AbstractFacet> facetClass, FacetQuery query, FacetModifier<T> modifier) {
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

        T modified = modifier.createOrModify(orig);
        // createOrModify must return passed argument if it is not null
        assert(orig == null || orig == modified);
        assert(false);
        assert (modified != null);
        //System.out.println("====== after modify, contained?: " + em.contains(modified));
        if (orig == null) {
            // Persist the newly-created facet
            em.persist(modified);
            //System.out.println("====== after persist, contained?: " + em.contains(modified));
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
	public void setFacetComment(long guestId, ObjectType objectType,
			long facetId, String text) {
		AbstractFacet facet = em.find(objectType.facetClass(), facetId);
		facet.comment = text;
		em.merge(facet);
	}

	@Override
    @Transactional(readOnly = false)
	public void setFacetComment(long guestId, Connector connector,
			long facetId, String text) {
		Class<? extends AbstractFacet> facetClass = connector.facetClass();
		AbstractFacet facet = em.find(facetClass, facetId);
		facet.comment = text;
		em.merge(facet);
	}

    @Override
    @Transactional(readOnly = false)
    public void addGuestLocation(final long guestId, LocationFacet locationResource) {
        // Create query to check for duplicate
        Query query = em.createQuery("SELECT e FROM Facet_GoogleLatitudeLocation e WHERE e.guestId=? AND e.start=?");
		query.setParameter(1, guestId);
		query.setParameter(2, locationResource.timestampMs);
		@SuppressWarnings("rawtypes")
		List existing = query.getResultList();
        // Only update Day metadata and persist if this location datapoint is not yet present in the DB table
		if (existing.size()==0) {
            // Fill in guestId, timeUpdated, and source.  These were removed in commit
            // 2c8539c76acbbcd2576458c9ea0144d910a5d836 and location data tables using versions between
            // there and here will have all location data assigned to guestId=0.  Connectors updated with
            // those versions need to be removed and re-added or they will have data gaps
            locationResource.guestId=guestId;
            locationResource.timeUpdated=System.currentTimeMillis();
            if(locationResource.source==LocationFacet.Source.GOOGLE_LATITUDE) {
                locationResource.api=Connector.getConnector("google_latitude").value();
            }
            else {
                locationResource.api=0;// This happens when GuestServiceImpl CheckIn function uses ip lookup
            }

            // Put updateDayMetadata in a try/catch block because we don't want to fail update or
            // fail to persist this datapoint due to some problem in the timezone detection, etc.
            try {
                updateDayMetadata(guestId, locationResource.timestampMs, locationResource.latitude, locationResource.longitude);
            } catch(Throwable e) {
                StringBuilder sb = new StringBuilder("module=updateQueue component=apiDataServiceImpl action=addGuestLocation")
                                    .append(" latitude=").append(locationResource.latitude)
                                    .append(" longitude=").append(locationResource.longitude)
                                    .append(" guestId=").append(guestId)
                                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
                logger.warn(sb.toString());
            }

            // Persist the location
            em.persist(locationResource);
        }
    }

    @Transactional(readOnly = false)
    private void updateDayMetadata(long guestId, long time, float latitude,
                                  float longitude) {
        City city = metadataService.getClosestCity((double)latitude, (double)longitude);
        String date = formatter.withZone(DateTimeZone.forID(city.geo_timezone))
                .print(time);

        DayMetadataFacet info = metadataService.getDayMetadata(guestId, date, true);
        TimeZone origTz = info.getTimeInterval().timeZone;
        servicesHelper.addCity(info, city);
        boolean timeZoneWasSet = servicesHelper.setTimeZone(info, city.geo_timezone);
        if (timeZoneWasSet) {
            TimeZone newTz = info.getTimeInterval().timeZone;
            updateFloatingTimeZoneFacets(guestId, time, origTz, newTz);
        }

        TimeZone tz = TimeZone.getTimeZone(info.timeZone);
        List<WeatherInfo> weatherInfo = metadataService.getWeatherInfo(city.geo_latitude,
                                                                       city.geo_longitude, info.date,
                                                                       AbstractFacetVO.toMinuteOfDay(new Date(info.start), tz),
                                                                       AbstractFacetVO.toMinuteOfDay(new Date(info.end), tz));
        wwoHelper.setWeatherInfo(info, weatherInfo);

        em.merge(info);
    }

    private void updateFloatingTimeZoneFacets(long guestId, long time, TimeZone origTz, TimeZone newTz) {
        // TODO Auto-generated method stub
        // Find the connectors that use floating time zone
        //   For each find the call that we should make to update the timezone
        //   Pass time, origTz, and newTz to that

        // The calls to update the timzeon for a given connector should in general:
        //    Compute the unixtime corresponding to 'time' as computed in origTz for finding affected facets
        //    Most likely way to do this is compute delta in millis of origTz-newTz
        //    and either add or subtract that from time (would need to think for a while about which)
        //    and do whatever's appropriate to update them
    }


    @Override
	public long getNumberOfDays(long guestId) {
		Query query = em.createQuery("select count(md) from ContextualInfo md WHERE md.guestId=" + guestId);
		Object singleResult = query.getSingleResult();
        return (Long) singleResult;
	}

}
