package org.fluxtream.connectors.sms_backup;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.PhotoFacetFinderStrategy;
import org.fluxtream.core.domain.TagFilter;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class SmsBackupPhotoFacetFinderStrategy implements PhotoFacetFinderStrategy {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<AbstractFacet> findAll(final ApiKey apiKey, final ObjectType objectType, final TimeInterval timeInterval) {
        return findAll(apiKey, objectType, timeInterval, null);
    }

    @Override
    public List<AbstractFacet> findBefore(final ApiKey apiKey, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        return findBefore(apiKey, objectType, timeInMillis, desiredCount, null);
    }

    @Override
    public List<AbstractFacet> findAfter(final ApiKey apiKey, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        return findAfter(apiKey, objectType, timeInMillis, desiredCount, null);
    }

    @Override
    public List<AbstractFacet> findAll(final ApiKey apiKey, final ObjectType objectType, final TimeInterval timeInterval, @Nullable final TagFilter tagFilter) {
        Query query = getQuery(apiKey,objectType,timeInterval.getStart(),timeInterval.getEnd(),null,null,tagFilter);
        return query.getResultList();
    }

    @Override
    public List<AbstractFacet> findBefore(final ApiKey apiKey, final ObjectType objectType, final long timeInMillis, final int desiredCount, @Nullable final TagFilter tagFilter) {
        Query query = getQuery(apiKey,objectType,null,timeInMillis,desiredCount,"ORDER BY facet.start DESC",tagFilter);
        return query.getResultList();
    }

    @Override
    public List<AbstractFacet> findAfter(final ApiKey apiKey, final ObjectType objectType, final long timeInMillis, final int desiredCount, @Nullable final TagFilter tagFilter) {
        Query query = getQuery(apiKey,objectType,timeInMillis,null,desiredCount,"ORDER BY facet.start ASC",tagFilter);
        return query.getResultList();
    }

    @Override
    public AbstractFacet findOldest(final ApiKey apiKey, final ObjectType objectType) {
        Query query = getQuery(apiKey,objectType,null,null,1,"ORDER BY facet.start ASC",null);
        List facets = query.getResultList();
        if (facets.isEmpty())
            return null;
        return (AbstractFacet) facets.get(0);
    }

    @Override
    public AbstractFacet findLatest(final ApiKey apiKey, final ObjectType objectType) {
        Query query = getQuery(apiKey,objectType,null,null,1,"ORDER BY facet.start DESC",null);
        List facets = query.getResultList();
        if (facets.isEmpty())
            return null;
        return (AbstractFacet) facets.get(0);
    }

    private Query getQuery(final ApiKey apiKey, final ObjectType objectType, @Nullable final Long startTime, @Nullable final Long endTime, @Nullable Integer limit, @Nullable String sortStatement, @Nullable final TagFilter tagFilter){
        final Class<? extends AbstractFacet> facetClass = getFacetClass(apiKey.getConnector(), objectType);
        final Entity entity = facetClass.getAnnotation(Entity.class);
        String queryString = "SELECT facet FROM " + entity.name() + " facet WHERE facet.apiKeyId=" + apiKey.getId() +
                             " AND facet.hasAttachments=true AND facet.attachmentMimeTypes LIKE '%image%'";
        if (startTime != null){
            queryString += " AND facet.start>=" + startTime;
        }
        if (endTime != null)
            queryString +=  " AND facet.end<=" + endTime;

        if (tagFilter != null){
            queryString += " AND (" + tagFilter.getWhereClause() + ")";
        }
        if (sortStatement != null){
            queryString += " " + sortStatement;
        }
        if (limit != null){
            queryString += " LIMIT " + limit;
        }

        return em.createQuery(queryString);



    }

    private Class<? extends AbstractFacet> getFacetClass(final Connector connector, final ObjectType objectType) {
        return objectType != null ? objectType.facetClass() : connector.facetClass();
    }
}
