package com.fluxtream.connectors.mymee;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.PhotoFacetFinderStrategy;
import com.fluxtream.utils.JPAUtils;
import org.springframework.stereotype.Component;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
@Component
public class MyMeePhotoFacetFinderStrategy implements PhotoFacetFinderStrategy {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<AbstractFacet> findAll(ApiKey apiKey, final ObjectType objectType, final TimeInterval timeInterval) {
        return (List<AbstractFacet>)JPAUtils.find(em, getFacetClass(apiKey.getConnector(), objectType), "mymee.photo.between", apiKey.getGuestId(), timeInterval.start, timeInterval.end);
    }

    @Override
    public List<AbstractFacet> findBefore(ApiKey apiKey, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        final Class<? extends AbstractFacet> facetClass = getFacetClass(apiKey.getConnector(), objectType);
        final Entity entity = facetClass.getAnnotation(Entity.class);
        final Query query = em.createQuery("SELECT facet FROM " + entity.name() + " facet WHERE facet.imageURL IS NOT NULL AND facet.guestId = " + apiKey.getGuestId() + " AND facet.start <= " + timeInMillis + " ORDER BY facet.start DESC LIMIT " + desiredCount);
        query.setMaxResults(desiredCount);
        return (List<AbstractFacet>)query.getResultList();
    }

    @Override
    public List<AbstractFacet> findAfter(ApiKey apiKey, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        final Class<? extends AbstractFacet> facetClass = getFacetClass(apiKey.getConnector(), objectType);
        final Entity entity = facetClass.getAnnotation(Entity.class);
        final Query query = em.createQuery("SELECT facet FROM " + entity.name() + " facet WHERE facet.imageURL IS NOT NULL AND facet.guestId = " + apiKey.getGuestId() + " AND facet.start >= " + timeInMillis + " ORDER BY facet.start ASC LIMIT " + desiredCount);
        query.setMaxResults(desiredCount);
        return (List<AbstractFacet>)query.getResultList();
    }

    @Override
    public AbstractFacet findOldest(ApiKey apiKey, final ObjectType objectType) {
        return getOldestOrLatestFacet(em, apiKey, objectType, "asc");
    }

    @Override
    public AbstractFacet findLatest(ApiKey apiKey, final ObjectType objectType) {
        return getOldestOrLatestFacet(em, apiKey, objectType, "desc");
    }

    private AbstractFacet getOldestOrLatestFacet(final EntityManager em, ApiKey apiKey, final ObjectType objectType, final String sortOrder) {
        final Class<? extends AbstractFacet> facetClass = getFacetClass(apiKey.getConnector(), objectType);
        final Entity entity = facetClass.getAnnotation(Entity.class);
        final Query query = em.createQuery("SELECT facet FROM " + entity.name() + " facet WHERE facet.imageURL IS NOT NULL AND facet.guestId = " + apiKey.getGuestId() + " ORDER BY facet.end " + sortOrder + " LIMIT 1");
        query.setMaxResults(1);
        final List resultList = query.getResultList();
        if (resultList != null && resultList.size() > 0) {
            return (AbstractFacet)resultList.get(0);
        }
        return null;
    }

    private Class<? extends AbstractFacet> getFacetClass(final Connector connector, final ObjectType objectType) {
        return objectType != null ? objectType.facetClass() : connector.facetClass();
    }
}
