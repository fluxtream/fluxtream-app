package com.fluxtream.services.impl;

import java.util.ArrayList;
import java.util.Date;
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
import com.fluxtream.domain.AbstractUserProfile;
import com.fluxtream.domain.metadata.City;
import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.domain.metadata.WeatherInfo;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.BodyTrackStorageService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.thirdparty.helpers.WWOHelper;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
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

	@Autowired
	FacetDao jpaDao;

	@Autowired
	BeanFactory beanFactory;

	@Autowired
	BodyTrackStorageService bodyTrackStorageService;

    @Autowired
    MetadataService metadataService;

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

		em.persist(payload);
	}

	/**
	 * start and end parameters allow to specify time boundaries that are not
	 * contained in the connector data itself
	 */
	@Override
	@Transactional(readOnly = true)
	public void cacheApiDataJSON(UpdateInfo updateInfo, JSONObject jsonObject,
			long start, long end) throws Exception {
		ApiData apiData = new ApiData(updateInfo, start, end);
		apiData.jsonObject = jsonObject;
		extractFacets(apiData, updateInfo.objectTypes, updateInfo);
	}

	/**
	 * start and end parameters allow to specify time boundaries that are not
	 * contained in the connector data itself
	 */
	@Override
	@Transactional(readOnly = true)
	public void cacheApiDataJSON(UpdateInfo updateInfo, String json,
			long start, long end) throws Exception {
		ApiData apiData = new ApiData(updateInfo, start, end);
		apiData.json = json;
		extractFacets(apiData, updateInfo.objectTypes, updateInfo);
	}

	/**
	 * start and end parameters allow to specify time boundaries that are not
	 * contained in the connector data itself
	 */
	@Override
	@Transactional(readOnly = true)
	public void cacheApiDataXML(UpdateInfo updateInfo, Document xmlDocument,
			long start, long end) throws Exception {
		ApiData apiData = new ApiData(updateInfo, start, end);
		apiData.xmlDocument = xmlDocument;
		extractFacets(apiData, updateInfo.objectTypes, null);
	}

	/**
	 * start and end parameters allow to specify time boundaries that are not
	 * contained in the connector data itself
	 */
	@Override
	@Transactional(readOnly = true)
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
			int objectTypes, TimeInterval timeInterval) {
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
			TimeInterval timeInterval) {
		List<AbstractFacet> facets = jpaDao.getFacetsBetween(connector,
				guestId, objectType, timeInterval);
		return facets;
	}

    @Override
    public AbstractFacet getLatestApiDataFacet(long guestId, Connector connector, ObjectType objectType){
        return jpaDao.getLatestFacet(connector,guestId,objectType);
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

	private AbstractFacet persistFacet(AbstractFacet facet) {
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
			em.persist(facet);
			return facet;
		}
	}

	private void logDuplicateFacet(AbstractFacet facet) {
		try {
			Connector connector = Connector.fromValue(facet.api);
			StringBuilder sb = new StringBuilder(" action=persistFacet connector=");
			sb.append(connector.getName());
			if (facet.objectType!=-1) {
				ObjectType[] objectType = connector.getObjectTypesForValue(facet.objectType);
				if (objectType!=null&&objectType.length!=0)
					sb.append(" objectType=").append(objectType[0].getName());
				else if (objectType.length>1) {
					sb.append(" objectType=[");
					for (int i = 0; i < objectType.length; i++) {
						ObjectType type = objectType[i];
						if (i>0) sb.append(",");
						sb.append(type.getName());
					}
					sb.append("]");
				}
			}
			logger.warn("guestId=" + facet.guestId + sb.toString());
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
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
	public void setFacetComment(long guestId, ObjectType objectType,
			long facetId, String text) {
		AbstractFacet facet = em.find(objectType.facetClass(), facetId);
		facet.comment = text;
		em.merge(facet);
	}

	@Override
	public void setFacetComment(long guestId, Connector connector,
			long facetId, String text) {
		Class<? extends AbstractFacet> facetClass = connector.facetClass();
		AbstractFacet facet = em.find(facetClass, facetId);
		facet.comment = text;
		em.merge(facet);
	}

    @Override
    @Transactional(readOnly = false)
    public void addGuestLocation(final long guestId, LocationFacet locationResource,
                                 final LocationFacet.Source source) {
        LocationFacet payload = new LocationFacet();
        payload.source = source;
        payload.latitude = locationResource.latitude;
        payload.longitude = locationResource.longitude;
        payload.start = locationResource.timestampMs;
        payload.end = locationResource.timestampMs;
        payload.api = Connector.getConnector("google_latitude").value();
        payload.guestId = guestId;
        payload.timestampMs = locationResource.timestampMs;
        payload.accuracy = locationResource.accuracy;
        payload.timeUpdated = System.currentTimeMillis();

        updateDayMetadata(guestId, locationResource.timestampMs, locationResource.latitude, locationResource.longitude);

        em.persist(payload);
    }

    @Transactional(readOnly = false)
    private void updateDayMetadata(long guestId, long time, float latitude,
                                  float longitude) {
        City city = metadataService.getClosestCity((double)latitude, (double)longitude);
        String date = formatter.withZone(DateTimeZone.forID(city.geo_timezone))
                .print(time);

        DayMetadataFacet info = metadataService.getDayMetadata(guestId, date, true);
        servicesHelper.addCity(info, city);
        boolean timeZoneWasSet = servicesHelper.setTimeZone(info, city.geo_timezone);
        if (timeZoneWasSet)
            updateFloatingTimeZoneFacets(guestId, time);

        TimeZone tz = TimeZone.getTimeZone(info.timeZone);
        List<WeatherInfo> weatherInfo = metadataService.getWeatherInfo(city.geo_latitude,
                                                                       city.geo_longitude, info.date,
                                                                       AbstractFacetVO.toMinuteOfDay(new Date(info.start), tz),
                                                                       AbstractFacetVO.toMinuteOfDay(new Date(info.end), tz));
        wwoHelper.setWeatherInfo(info, weatherInfo);

        em.merge(info);
    }

    private void updateFloatingTimeZoneFacets(long guestId, long time) {
        // TODO Auto-generated method stub

    }


    @Override
	public long getNumberOfDays(long guestId) {
		Query query = em.createQuery("select count(md) from ContextualInfo md WHERE md.guestId=" + guestId);
		Object singleResult = query.getSingleResult();
		Long count = (Long) singleResult;
		return count;
	}

}
