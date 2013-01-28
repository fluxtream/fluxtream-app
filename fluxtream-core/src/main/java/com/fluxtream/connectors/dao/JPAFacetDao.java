package com.fluxtream.connectors.dao;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Component
public class JPAFacetDao implements FacetDao {

    private static final Logger LOG = Logger.getLogger(JPAFacetDao.class);

    @Autowired
	GuestService guestService;

    @Qualifier("connectorUpdateServiceImpl")
    @Autowired
	ConnectorUpdateService connectorUpdateService;

	@PersistenceContext
	private EntityManager em;

	public JPAFacetDao() {}

    @Override
    public List<AbstractFacet> getFacetsByDates(final ApiKey apiKey, ObjectType objectType, List<String> dates) {
        ArrayList<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        if (!apiKey.getConnector().hasFacets()) return facets;
        final String facetName = getEntityName(apiKey.getConnector(), objectType);
        String queryString = "SELECT facet FROM " + facetName + " facet WHERE facet.guestId=? AND (facet.apiKeyId=? OR facet.apiKeyId IS NULL) AND facet.date IN ?";
        final TypedQuery<? extends AbstractFacet> query = em.createQuery(queryString, AbstractFacet.class);
        query.setParameter(1, apiKey.getGuestId());
        query.setParameter(2, apiKey.getId());
        query.setParameter(3, StringUtils.join(dates, ","));
        List<? extends AbstractFacet> found = query.getResultList();
        if (found!=null)
            facets.addAll(found);
        return facets;
    }

    @Cacheable("facetClasses")
    private String getEntityName(Connector connector, ObjectType objectType) {
        try {
            Class<? extends AbstractFacet> facetClass = objectType!=null
                    ? objectType.facetClass()
                    : connector.facetClass();
            return facetClass.getAnnotation(Entity.class).name();
        } catch (Throwable t) {
            final String message = "Could not get Facet class for connector " + connector + " / " + objectType;
            LOG.error(message);
            throw new RuntimeException(message);
        }
    }

    public List<AbstractFacet> getFacetsBetween(final ApiKey apiKey, TimeInterval timeInterval) {
        final ObjectType[] objectTypes = apiKey.getConnector().objectTypes();
        List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        for (ObjectType type : objectTypes) {
            facets.addAll(getFacetsBetween(apiKey, type, timeInterval));
        }
        return facets;
    }

	@Override
	public List<AbstractFacet> getFacetsBetween(final ApiKey apiKey, ObjectType objectType, TimeInterval timeInterval) {
        if (objectType==null) {
            return getFacetsBetween(apiKey, timeInterval);
        } else {
            if (!apiKey.getConnector().hasFacets()) return new ArrayList<AbstractFacet>();
            final String facetName = getEntityName(apiKey.getConnector(), objectType);
            String queryString = "SELECT facet FROM " + facetName  + " facet WHERE facet.guestId=? AND (facet.apiKeyId=? OR facet.apiKeyId IS NULL) AND facet.start>=? AND facet.end<=?";
            final TypedQuery<AbstractFacet> query = em.createQuery(queryString, AbstractFacet.class);
            query.setParameter(1, apiKey.getGuestId());
            query.setParameter(2, apiKey.getId());
            query.setParameter(3, timeInterval.start);
            query.setParameter(4, timeInterval.end);
            List<AbstractFacet> facets = query.getResultList();
            return facets;
        }
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
        return getFacets(apiKey, objectType, timeInMillis, desiredCount, "getFacetsBefore");
    }

    @Override
    public List<AbstractFacet> getFacetsAfter(final ApiKey apiKey, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        return getFacets(apiKey, objectType, timeInMillis, desiredCount, "getFacetsAfter");
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
                if (LOG.isInfoEnabled()) {
                    LOG.info("JPAFacetDao.getFacet(): ignoring exception '" + ignored.getClass() + "' while trying to invoke method '" + methodName + "'");
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
                        if (LOG.isInfoEnabled()) {
                            LOG.info("JPAFacetDao.getFacet(): ignoring exception '" + ignored.getClass() + "' while trying to invoke method '" + methodName + "'");
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
                    if (LOG.isInfoEnabled()) {
                        LOG.info("JPAFacetDao.getFacet(): ignoring exception '" + ignored.getClass() + "' while trying to invoke method '" + methodName + "'");
                    }
                }
            }
        }
        return facet;
    }

    private List<AbstractFacet> getFacets(final ApiKey apiKey, final ObjectType objectType, final long timeInMillis, final int desiredCount, final String methodName) {
        if (!apiKey.getConnector().hasFacets()) {
            return null;
        }

        List<AbstractFacet> facets = null;
        if (objectType != null) {
            try {
                Class c = objectType.facetClass();
                Method m = c.getMethod(methodName, EntityManager.class, ApiKey.class, ObjectType.class, Long.class, Integer.class );
                facets = (List<AbstractFacet>)m.invoke(null, em, apiKey, objectType, timeInMillis, desiredCount);
            }
            catch (Exception ignored) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("JPAFacetDao.getFacets(): ignoring exception '" + ignored.getClass() + "' while trying to invoke method '" + methodName + "'");
                }
            }
        }
        else {
            if (apiKey.getConnector().objectTypes() != null) {
                for (ObjectType type : apiKey.getConnector().objectTypes()) {
                    try {
                        Class c = type.facetClass();
                        Method m = c.getMethod(methodName, EntityManager.class, ApiKey.class, ObjectType.class, Long.class, Integer.class);
                        facets = (List<AbstractFacet>)m.invoke(null, em, apiKey, type, timeInMillis, desiredCount);
                    }
                    catch (Exception ignored) {
                        if (LOG.isInfoEnabled()) {
                            LOG.info("JPAFacetDao.getFacets(): ignoring exception '" + ignored.getClass() + "' while trying to invoke method '" + methodName + "'");
                        }
                    }
                }
            }
            else {
                try {
                    Class c = apiKey.getConnector().facetClass();
                    Method m = c.getMethod(methodName, EntityManager.class, ApiKey.class, ObjectType.class, Long.class, Integer.class);
                    facets = (List<AbstractFacet>)m.invoke(null, em, apiKey, null, timeInMillis, desiredCount);
                }
                catch (Exception ignored) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("JPAFacetDao.getFacets(): ignoring exception '" + ignored.getClass() + "' while trying to invoke method '" + methodName + "'");
                    }
                }
            }
        }
        return facets;
    }

    @Override
	public void deleteAllFacets(ApiKey apiKey) {
        final ObjectType[] objectTypes = apiKey.getConnector().objectTypes();
        for (ObjectType objectType : objectTypes) {
            deleteAllFacets(apiKey, objectType);
        }
	}

	@Override
	public void deleteAllFacets(ApiKey apiKey, ObjectType objectType) {
        if (objectType==null) {
            deleteAllFacets(apiKey);
        } else {
            final String facetName = getEntityName(apiKey.getConnector(), objectType);
            String stmtString = "DELETE FROM " + facetName + " facet WHERE facet.guestId=? AND (facet.apiKeyId=? OR facet.apiKeyId IS NULL)";
            final Query query = em.createQuery(stmtString);
            query.setParameter(1, apiKey.getGuestId());
            query.setParameter(2, apiKey.getId());
            query.executeUpdate();
        }
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

}
