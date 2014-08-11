package org.fluxtream.connectors.mymee;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.PhotoFacetFinderStrategy;
import org.fluxtream.core.domain.TagFilter;
import org.fluxtream.core.utils.JPAUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
@Component
public class MyMeePhotoFacetFinderStrategy implements PhotoFacetFinderStrategy {
    private static final FlxLogger LOG_DEBUG = FlxLogger.getLogger("Fluxtream");

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<AbstractFacet> findAll(ApiKey apiKey, final ObjectType objectType, final TimeInterval timeInterval) {
        return findAll(apiKey, objectType, timeInterval, null);
    }

    @Override
    public List<AbstractFacet> findBefore(ApiKey apiKey, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        return findBefore(apiKey, objectType, timeInMillis, desiredCount, null);
    }

    @Override
    public List<AbstractFacet> findAfter(ApiKey apiKey, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        return findAfter(apiKey, objectType, timeInMillis, desiredCount, null);
    }

    @Override
    public List<AbstractFacet> findAll(final ApiKey apiKey,
                                       final ObjectType objectType,
                                       final TimeInterval timeInterval,
                                       @Nullable final TagFilter tagFilter) {
        if (tagFilter == null) {
            return (List<AbstractFacet>)JPAUtils.find(em, getFacetClass(apiKey.getConnector(), objectType), "mymee.photo.between", apiKey.getGuestId(), timeInterval.getStart(), timeInterval.getEnd());
        }

        final Class<? extends AbstractFacet> facetClass = getFacetClass(apiKey.getConnector(), objectType);
        final Entity entity = facetClass.getAnnotation(Entity.class);
        final String queryStr = "SELECT facet FROM " + entity.name() + " facet" +
                                " WHERE" +
                                " facet.imageURL IS NOT NULL" +
                                " AND" +
                                " facet.guestId = " + apiKey.getGuestId() +
                                " AND" +
                                " facet.start >= " + timeInterval.getStart() +
                                " AND" +
                                " facet.end <= " + timeInterval.getEnd() +
                                " AND" +
                                " (" + tagFilter.getWhereClause() + ")";
        final Query query = em.createQuery(queryStr);
        return (List<AbstractFacet>)query.getResultList();
    }

    @Override
    public List<AbstractFacet> findBefore(final ApiKey apiKey,
                                          final ObjectType objectType,
                                          final long timeInMillis,
                                          final int desiredCount,
                                          @Nullable final TagFilter tagFilter) {
        final Class<? extends AbstractFacet> facetClass = getFacetClass(apiKey.getConnector(), objectType);
        final Entity entity = facetClass.getAnnotation(Entity.class);
        final String additionalWhereClause = (tagFilter == null) ? "" : " AND (" + tagFilter.getWhereClause() + ")";
        final String queryStr = "SELECT facet FROM " + entity.name() + " facet" +
                                " WHERE" +
                                " facet.imageURL IS NOT NULL" +
                                " AND" +
                                " facet.guestId = " + apiKey.getGuestId() +
                                " AND" +
                                " facet.start <= " + timeInMillis +
                                additionalWhereClause +
                                " ORDER BY facet.start DESC LIMIT " + desiredCount;
        final Query query = em.createQuery(queryStr);
        query.setMaxResults(desiredCount);
        return (List<AbstractFacet>)query.getResultList();
    }

    @Override
    public List<AbstractFacet> findAfter(final ApiKey apiKey,
                                         final ObjectType objectType,
                                         final long timeInMillis,
                                         final int desiredCount,
                                         @Nullable final TagFilter tagFilter) {
        final Class<? extends AbstractFacet> facetClass = getFacetClass(apiKey.getConnector(), objectType);
        final Entity entity = facetClass.getAnnotation(Entity.class);
        final String additionalWhereClause = (tagFilter == null) ? "" : " AND (" + tagFilter.getWhereClause() + ")";
        final String queryStr = "SELECT facet FROM " + entity.name() + " facet" +
                                " WHERE" +
                                " facet.imageURL IS NOT NULL" +
                                " AND" +
                                " facet.guestId = " + apiKey.getGuestId() +
                                " AND" +
                                " facet.start >= " + timeInMillis +
                                additionalWhereClause +
                                " ORDER BY facet.start ASC LIMIT " + desiredCount;
        final Query query = em.createQuery(queryStr);
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
