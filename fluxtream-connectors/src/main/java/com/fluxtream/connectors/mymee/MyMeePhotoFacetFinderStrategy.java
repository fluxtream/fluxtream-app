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
import com.fluxtream.domain.PhotoFacetFinderStrategy;
import com.fluxtream.utils.JPAUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
@Component
public class MyMeePhotoFacetFinderStrategy implements PhotoFacetFinderStrategy {

    private static final Logger LOG = Logger.getLogger("Fluxtream");

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<AbstractFacet> findAll(final long guestId, final Connector connector, final ObjectType objectType, final TimeInterval timeInterval) {
        LOG.debug("MyMeePhotoFacetFinderStrategy.findAll(" + guestId + ", " + connector.prettyName() + ", " + (objectType == null ? null : objectType.getName()) + ")");
        return (List<AbstractFacet>)JPAUtils.find(em, getFacetClass(connector, objectType), "mymee.photo.between", guestId, timeInterval.start, timeInterval.end);
    }

    @Override
    public List<AbstractFacet> findBefore(final long guestId, final Connector connector, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        LOG.debug("MyMeePhotoFacetFinderStrategy.findBefore(" + guestId + ", " + connector.prettyName() + ", " + (objectType == null ? null : objectType.getName()) + ", " + timeInMillis + ", " + desiredCount + ")");
        final Class<? extends AbstractFacet> facetClass = getFacetClass(connector, objectType);
        final Entity entity = facetClass.getAnnotation(Entity.class);
        final Query query = em.createQuery("SELECT facet FROM " + entity.name() + " facet WHERE facet.imageURL IS NOT NULL AND facet.guestId = " + guestId + " AND facet.start <= " + timeInMillis + " ORDER BY facet.start DESC LIMIT " + desiredCount);
        query.setMaxResults(desiredCount);
        return (List<AbstractFacet>)query.getResultList();
    }

    @Override
    public List<AbstractFacet> findAfter(final long guestId, final Connector connector, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        LOG.debug("MyMeePhotoFacetFinderStrategy.findAfter(" + guestId + ", " + connector.prettyName() + ", " + (objectType == null ? null : objectType.getName()) + ", " + timeInMillis + ", " + desiredCount + ")");
        final Class<? extends AbstractFacet> facetClass = getFacetClass(connector, objectType);
        final Entity entity = facetClass.getAnnotation(Entity.class);
        final Query query = em.createQuery("SELECT facet FROM " + entity.name() + " facet WHERE facet.imageURL IS NOT NULL AND facet.guestId = " + guestId + " AND facet.start >= " + timeInMillis + " ORDER BY facet.start ASC LIMIT " + desiredCount);
        query.setMaxResults(desiredCount);
        return (List<AbstractFacet>)query.getResultList();
    }

    @Override
    public AbstractFacet findOldest(final long guestId, final Connector connector, final ObjectType objectType) {
        LOG.debug("MyMeePhotoFacetFinderStrategy.findOldest(" + guestId + ", " + connector.prettyName() + ", " + (objectType == null ? null : objectType.getName()) + ")");
        return getOldestOrLatestFacet(em, guestId, connector, objectType, "asc");
    }

    @Override
    public AbstractFacet findLatest(final long guestId, final Connector connector, final ObjectType objectType) {
        LOG.debug("MyMeePhotoFacetFinderStrategy.findLatest(" + guestId + ", " + connector.prettyName() + ", " + (objectType == null ? null : objectType.getName()) + ")");
        return getOldestOrLatestFacet(em, guestId, connector, objectType, "desc");
    }

    private AbstractFacet getOldestOrLatestFacet(final EntityManager em, final long guestId, final Connector connector, final ObjectType objectType, final String sortOrder) {
        final Class<? extends AbstractFacet> facetClass = getFacetClass(connector, objectType);
        final Entity entity = facetClass.getAnnotation(Entity.class);
        final Query query = em.createQuery("SELECT facet FROM " + entity.name() + " facet WHERE facet.imageURL IS NOT NULL AND facet.guestId = " + guestId + " ORDER BY facet.end " + sortOrder + " LIMIT 1");
        query.setMaxResults(1);
        return (AbstractFacet)query.getResultList().get(0);
    }

    private Class<? extends AbstractFacet> getFacetClass(final Connector connector, final ObjectType objectType) {
        return objectType != null ? objectType.facetClass() : connector.facetClass();
    }
}
