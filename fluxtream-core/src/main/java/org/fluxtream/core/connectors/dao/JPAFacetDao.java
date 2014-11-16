package org.fluxtream.core.connectors.dao;

import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.connectors.location.LocationFacet;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.AbstractRepeatableFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.TagFilter;
import org.fluxtream.core.domain.metadata.VisitedCity;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.utils.JPAUtils;
import org.fluxtream.core.utils.TimeUtils;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.lang.reflect.Method;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Repository
@Component
public class JPAFacetDao implements FacetDao {

    private static final FlxLogger logger = FlxLogger.getLogger(JPAFacetDao.class);

    @Autowired
	GuestService guestService;

    @Autowired
	ConnectorUpdateService connectorUpdateService;

	@PersistenceContext
	private EntityManager em;

	public JPAFacetDao() {}

    @Override
    public List<AbstractFacet> getFacetsByDates(final ApiKey apiKey, ObjectType objectType, List<String> dates, Long updatedSince) {
        if (!objectType.isClientFacet())
            return new ArrayList<AbstractFacet>();
        ArrayList<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        if (!apiKey.getConnector().hasFacets()) return facets;
        Class<? extends AbstractFacet> facetClass = getFacetClass(apiKey.getConnector(), objectType);
        final String facetName = getEntityName(facetClass);
        StringBuilder additionalWhereClause = new StringBuilder();
        if (objectType.visibleClause()!=null) additionalWhereClause.append(" AND ").append(objectType.visibleClause()).append(" ");
        if (updatedSince != null){
            additionalWhereClause.append(" AND facet.timeUpdated > ").append(updatedSince);
        }
        String queryString = new StringBuilder("SELECT facet FROM ")
                .append(facetName)
                .append(" facet WHERE facet.apiKeyId=:apiKeyId AND facet.date IN :dates")
                .append(additionalWhereClause)
                .toString();
        final TypedQuery<? extends AbstractFacet> query = em.createQuery(queryString, AbstractFacet.class);
        query.setParameter("apiKeyId", apiKey.getId());
        query.setParameter("dates", dates);
        List<? extends AbstractFacet> found = query.getResultList();
        if (found!=null)
            facets.addAll(found);
        return facets;
    }

    @Override
    public List<AbstractRepeatableFacet> getFacetsBetweenDates(final ApiKey apiKey, final ObjectType objectType, final String startDateString, final String endDateString, Long updatedSince) {
        if (!objectType.isClientFacet())
            return new ArrayList<AbstractRepeatableFacet>();
        ArrayList<AbstractRepeatableFacet> facets = new ArrayList<AbstractRepeatableFacet>();
        if (!apiKey.getConnector().hasFacets()) return facets;
        Class<? extends AbstractRepeatableFacet> facetClass = (Class<? extends AbstractRepeatableFacet>)getFacetClass(apiKey.getConnector(), objectType);
        final String facetName = getEntityName(facetClass);
        StringBuilder additionalWhereClause = new StringBuilder();
        if (objectType.visibleClause()!=null) additionalWhereClause.append(" AND ").append(objectType.visibleClause()).append(" ");
        if (updatedSince != null){
            additionalWhereClause.append(" AND facet.timeUpdated > ").append(updatedSince);
        }
        StringBuilder queryBuilder = new StringBuilder("SELECT facet FROM ")
                .append(facetName)
                .append(" facet WHERE facet.apiKeyId=:apiKeyId AND NOT(facet.endDate<:startDate) AND NOT(facet.startDate>:endDate)")
                .append(additionalWhereClause);
        String orderBy = objectType.orderBy();
        if (orderBy!=null)
            queryBuilder.append(" ORDER BY ").append(orderBy);
        String queryString = queryBuilder.toString();
        final TypedQuery<? extends AbstractFacet> query = em.createQuery(queryString, AbstractFacet.class);
        query.setParameter("apiKeyId", apiKey.getId());
        final DateTime time = TimeUtils.dateFormatterUTC.parseDateTime(startDateString);
        Date startDate = new Date(time.getMillis());
        final DateTime time2 = TimeUtils.dateFormatterUTC.parseDateTime(endDateString);
        Date endDate = new Date(time2.getMillis());
        query.setParameter("startDate", startDate, TemporalType.DATE);
        query.setParameter("endDate", endDate, TemporalType.DATE);
        List<? extends AbstractRepeatableFacet> found = (List<? extends AbstractRepeatableFacet>)query.getResultList();
        if (found!=null)
            facets.addAll(found);
        return facets;
    }

    @Cacheable("facetClasses")
    private String getEntityName(Class<? extends AbstractFacet> facetClass) {
        try {
            return facetClass.getAnnotation(Entity.class).name();
        } catch (Throwable t) {
            final String message = "Could not get Facet class for connector for " + facetClass.getName();
            logger.error(message);
            throw new RuntimeException(message);
        }
    }

    private Class<? extends AbstractFacet> getFacetClass(final Connector connector, final ObjectType objectType) {
        return objectType!=null
                        ? objectType.facetClass()
                        : connector.facetClass();
    }

	@Override
	public List<AbstractFacet> getFacetsBetween(final ApiKey apiKey, ObjectType objectType, TimeInterval timeInterval, Long updatedSince) {
        return getFacetsBetween(apiKey, objectType, timeInterval, null, null, updatedSince);
    }

    @Override
    public List<AbstractFacet> getFacetsBetween(ApiKey apiKey, ObjectType objectType, TimeInterval timeInterval, @Nullable TagFilter tagFilter, Long updatedSince) {
        return getFacetsBetween(apiKey, objectType, timeInterval, tagFilter, null, updatedSince);
    }

    @Override
    public List<AbstractFacet> getFacetsBetween(final ApiKey apiKey,
                                                final ObjectType objectType,
                                                final TimeInterval timeInterval,
                                                @Nullable final TagFilter tagFilter,
                                                @Nullable final String orderByString,
                                                Long updatedSince) {
        if (objectType==null) {
            return getFacetsBetween(apiKey, timeInterval, tagFilter, updatedSince);
        } else {
            if (!objectType.isClientFacet())
                return new ArrayList<AbstractFacet>();
            if (!apiKey.getConnector().hasFacets()) return new ArrayList<AbstractFacet>();
            Class<? extends AbstractFacet> facetClass = getFacetClass(apiKey.getConnector(), objectType);
            final String facetName = getEntityName(facetClass);
            StringBuilder additionalWhereClause = new StringBuilder();
            if (tagFilter != null) additionalWhereClause.append(" AND (").append(tagFilter.getWhereClause()).append(")");
            if (objectType.isMixedType()) additionalWhereClause.append(" AND facet.allDayEvent=false ");
            if (objectType.visibleClause()!=null) additionalWhereClause.append(" AND ").append(objectType.visibleClause()).append(" ");
            if (updatedSince != null){
                additionalWhereClause.append(" AND facet.timeUpdated > ").append(updatedSince);
            }
            StringBuilder queryStringBuilder = new StringBuilder("SELECT facet FROM ")
                    .append(facetName)
                    .append(" facet WHERE facet.apiKeyId=? AND facet.end>=? AND facet.start<=?")
                    .append(additionalWhereClause);
            if (orderByString != null){
                queryStringBuilder.append(" ORDER BY ").append(orderByString);
            }
            String queryString= queryStringBuilder.toString();
            final TypedQuery<AbstractFacet> query = em.createQuery(queryString, AbstractFacet.class);
            query.setParameter(1, apiKey.getId());
            query.setParameter(2, timeInterval.getStart());
            query.setParameter(3, timeInterval.getEnd());
            List<AbstractFacet> facets = query.getResultList();
            return facets;
        }
    }

    private List<AbstractFacet> getFacetsBetween(final ApiKey apiKey, TimeInterval timeInterval, @Nullable final TagFilter tagFilter, Long updatedSince) {
        final ObjectType[] objectTypes = apiKey.getConnector().objectTypes();
        List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        for (ObjectType type : objectTypes) {
            if (!type.isClientFacet())
                continue;
            facets.addAll(getFacetsBetween(apiKey, type, timeInterval, tagFilter, updatedSince));
        }
        return facets;
    }

    @Override
    public AbstractFacet getOldestFacet(final ApiKey apiKey, final ObjectType objectType) {
        return getFacet(apiKey, objectType, "getOldestFacet");
    }

    @Override
    public AbstractFacet getLatestFacet(final ApiKey apiKey, final ObjectType objectType) {
        return getFacet(apiKey, objectType, "getLatestFacet");
    }

    @Override
    public List<AbstractFacet> getFacetsBefore(final ApiKey apiKey, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        return getFacetsBefore(apiKey, objectType, timeInMillis, desiredCount, null);
    }

    @Override
    public List<AbstractFacet> getFacetsAfter(final ApiKey apiKey, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        return getFacetsAfter(apiKey, objectType, timeInMillis, desiredCount, null);
    }

    @Override
    public List<AbstractFacet> getFacetsBefore(final ApiKey apiKey,
                                               final ObjectType objectType,
                                               final long timeInMillis,
                                               final int desiredCount,
                                               @Nullable final TagFilter tagFilter) {
        return getFacets(apiKey, objectType, timeInMillis, desiredCount, "getFacetsBefore", tagFilter);
    }

    @Override
    public List<AbstractFacet> getFacetsAfter(final ApiKey apiKey,
                                              final ObjectType objectType,
                                              final long timeInMillis,
                                              final int desiredCount,
                                              @Nullable final TagFilter tagFilter) {
        return getFacets(apiKey, objectType, timeInMillis, desiredCount, "getFacetsAfter", tagFilter);
    }

    @Override
    public AbstractFacet getFacetById(ApiKey apiKey, final ObjectType objectType, final long facetId) {
        final Class<? extends AbstractFacet> facetClass = objectType.facetClass();
        final Entity entity = facetClass.getAnnotation(Entity.class);
        final TypedQuery<? extends AbstractFacet> query = em.createQuery("SELECT facet FROM " + entity.name() + " facet WHERE facet.id = " + facetId + " AND facet.guestId = " + apiKey.getGuestId(), facetClass);
        query.setMaxResults(1);

        final List resultList = query.getResultList();
        if (resultList != null && resultList.size() > 0) {
            return (AbstractFacet)resultList.get(0);
        }
        return null;
    }

    private AbstractFacet getFacet(final ApiKey apiKey, final ObjectType objectType, final String methodName) {
        if (!apiKey.getConnector().hasFacets()) {
            return null;
        }

        AbstractFacet facet = null;
        if (objectType != null) {
            try {
                Class c = objectType.facetClass();
                Method m = c.getMethod(methodName, EntityManager.class, ApiKey.class, ObjectType.class);
                facet = (AbstractFacet)m.invoke(null, em, apiKey, objectType);
            }
            catch (Exception ignored) {
                if (logger.isInfoEnabled()) {
                    logger.info("JPAFacetDao.getFacet(): ignoring exception '" + ignored.getClass() + "' while trying to invoke method '" + methodName + "'");
                }
            }
        }
        else {
            if (apiKey.getConnector().objectTypes() != null) {
                for (ObjectType type : apiKey.getConnector().objectTypes()) {
                    AbstractFacet fac = null;
                    try {
                        Class c = type.facetClass();
                        Method m = c.getMethod(methodName, EntityManager.class, ApiKey.class, ObjectType.class);
                        fac = (AbstractFacet)m.invoke(null, em, apiKey, type);
                    }
                    catch (Exception ignored) {
                        if (logger.isInfoEnabled()) {
                            logger.info("JPAFacetDao.getFacet(): ignoring exception '" + ignored.getClass() + "' while trying to invoke method '" + methodName + "'");
                        }
                    }
                    if (facet == null || (fac != null && fac.end > facet.end)) {
                        facet = fac;
                    }
                }
            }
            else {
                try {
                    Class c = apiKey.getConnector().facetClass();
                    Method m = c.getMethod(methodName, EntityManager.class, ApiKey.class, ObjectType.class);
                    facet = (AbstractFacet)m.invoke(null, em, apiKey, null);
                }
                catch (Exception ignored) {
                    if (logger.isInfoEnabled()) {
                        logger.info("JPAFacetDao.getFacet(): ignoring exception '" + ignored.getClass() + "' while trying to invoke method '" + methodName + "'");
                    }
                }
            }
        }
        return facet;
    }

    private List<AbstractFacet> getFacets(final ApiKey apiKey,
                                          final ObjectType objectType,
                                          final long timeInMillis,
                                          final int desiredCount,
                                          final String methodName,
                                          @Nullable final TagFilter tagFilter) {
        if (!apiKey.getConnector().hasFacets()) {
            return null;
        }

        List<AbstractFacet> facets = null;
        if (objectType != null) {
            try {
                Class c = objectType.facetClass();
                Method m = c.getMethod(methodName, EntityManager.class, ApiKey.class, ObjectType.class, Long.class, Integer.class, TagFilter.class);
                facets = (List<AbstractFacet>)m.invoke(null, em, apiKey, objectType, timeInMillis, desiredCount, tagFilter);
            }
            catch (Exception ignored) {
                if (logger.isInfoEnabled()) {
                    logger.info("JPAFacetDao.getFacets(): ignoring exception '" + ignored.getClass() + "' while trying to invoke method '" + methodName + "'");
                }
            }
        }
        else {
            if (apiKey.getConnector().objectTypes() != null) {
                for (ObjectType type : apiKey.getConnector().objectTypes()) {
                    try {
                        Class c = type.facetClass();
                        Method m = c.getMethod(methodName, EntityManager.class, ApiKey.class, ObjectType.class, Long.class, Integer.class, TagFilter.class);
                        facets = (List<AbstractFacet>)m.invoke(null, em, apiKey, type, timeInMillis, desiredCount, tagFilter);
                    }
                    catch (Exception ignored) {
                        if (logger.isInfoEnabled()) {
                            logger.info("JPAFacetDao.getFacets(): ignoring exception '" + ignored.getClass() + "' while trying to invoke method '" + methodName + "'");
                        }
                    }
                }
            }
            else {
                try {
                    Class c = apiKey.getConnector().facetClass();
                    Method m = c.getMethod(methodName, EntityManager.class, ApiKey.class, ObjectType.class, Long.class, Integer.class, TagFilter.class);
                    facets = (List<AbstractFacet>)m.invoke(null, em, apiKey, null, timeInMillis, desiredCount, tagFilter);
                }
                catch (Exception ignored) {
                    if (logger.isInfoEnabled()) {
                        logger.info("JPAFacetDao.getFacets(): ignoring exception '" + ignored.getClass() + "' while trying to invoke method '" + methodName + "'");
                    }
                }
            }
        }
        return facets;
    }

    @Override
	public void deleteAllFacets(ApiKey apiKey) {
        final Connector connector = apiKey.getConnector();
        if (connector.hasDeleteOrder()){
            final int[] deleteOrder = connector.getDeleteOrder();
            for (int ot : deleteOrder) {
                ObjectType objectType = ObjectType.getObjectType(connector, ot);
                deleteAllFacets(apiKey, objectType);
            }
        } else {
            final ObjectType[] objectTypes = connector.objectTypes();
            for (ObjectType objectType : objectTypes) {
                deleteAllFacets(apiKey, objectType);
            }
        }
	}

	@Override
	public void deleteAllFacets(ApiKey apiKey, ObjectType objectType) {
        if (objectType==null) {
            deleteAllFacets(apiKey);
        } else {
            // if facet has joins delete each facet one-by-one (this is a limitation of JPA)
            Class<? extends AbstractFacet> facetClass = getFacetClass(apiKey.getConnector(), objectType);
            if (JPAUtils.hasRelation(facetClass)) {
               deleteFacetsOneByOne(apiKey, facetClass);
            } else {
                bulkDeleteFacets(apiKey, facetClass);
            }
            final LocationFacet.Source locationFacetSource = getLocationFacetSource(facetClass);
            if (locationFacetSource != LocationFacet.Source.NONE) {
                deleteLocationData(apiKey);
                deleteVisitedCitiesData(apiKey);
            }
        }
	}

    @Transactional(readOnly=false)
    private void deleteVisitedCitiesData(final ApiKey apiKey) {
        final String facetName = getEntityName(VisitedCity.class);
        String stmtString = "DELETE FROM " + facetName + " facet WHERE facet.apiKeyId=?";
        final Query query = em.createQuery(stmtString);
        query.setParameter(1, apiKey.getId());
        query.executeUpdate();
    }

    @Transactional(readOnly=false)
    private void deleteLocationData(final ApiKey apiKey) {
        final String facetName = getEntityName(LocationFacet.class);
        String stmtString = "DELETE FROM " + facetName + " facet WHERE facet.apiKeyId=?";
        final Query query = em.createQuery(stmtString);
        query.setParameter(1, apiKey.getId());
        query.executeUpdate();
    }

    private LocationFacet.Source getLocationFacetSource(final Class<? extends AbstractFacet> facetClass) {
        final ObjectTypeSpec objectTypeSpec = facetClass.getAnnotation(ObjectTypeSpec.class);
        final LocationFacet.Source locationFacetSource = objectTypeSpec.locationFacetSource();
        return locationFacetSource;
    }

    private void deleteFacetsOneByOne(final ApiKey apiKey, final Class<? extends AbstractFacet> facetClass) {
        List<? extends AbstractFacet> facets = getAllFacets(apiKey, facetClass);
        for (AbstractFacet facet : facets) {
            final AbstractFacet merged = em.merge(facet);
            em.remove(merged);
        }
    }

    private List<? extends AbstractFacet> getAllFacets(final ApiKey apiKey, final Class<? extends AbstractFacet> facetClass) {
        final String facetName = getEntityName(facetClass);
        String queryString = "SELECT facet FROM " + facetName + " facet WHERE facet.apiKeyId=?";
        final TypedQuery<? extends AbstractFacet> query = em.createQuery(queryString, AbstractFacet.class);
        query.setParameter(1, apiKey.getId());
        List<? extends AbstractFacet> found = query.getResultList();
        return found;
    }

    private void bulkDeleteFacets(final ApiKey apiKey, final Class<? extends AbstractFacet> facetClass) {
        final String facetName = getEntityName(facetClass);
        String stmtString = "DELETE FROM " + facetName + " facet WHERE facet.apiKeyId=?";
        final Query query = em.createQuery(stmtString);
        query.setParameter(1, apiKey.getId());
        query.executeUpdate();
    }

    @Override
	@Transactional(readOnly=false)
	public void persist(Object o) {
		em.persist(o);
	}

	@Override
	@Transactional(readOnly=false)
	public void merge(Object o) {
		em.merge(o);
	}

    @Override
    @Transactional(readOnly=false)
    public void delete(AbstractFacet facet) {
        AbstractFacet merged = em.merge(facet);
        em.remove(merged);
    }

}
