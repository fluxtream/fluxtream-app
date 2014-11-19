package org.fluxtream.core.connectors.dao;

import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.AbstractRepeatableFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.TagFilter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface FacetDao {

    public List<AbstractFacet> getFacetsByDates(ApiKey apiKey, ObjectType objectType, List<String> dates, Long updatedSince);

    public List<AbstractRepeatableFacet> getFacetsBetweenDates(ApiKey apiKey, ObjectType objectType, String startDate, String endDate, Long updatedSince);

    public List<AbstractFacet> getFacetsBetween(ApiKey apiKey, ObjectType objectType, TimeInterval timeInterval, Long updatedSince);

    public List<AbstractFacet> getFacetsBetween(ApiKey apiKey, ObjectType objectType, TimeInterval timeInterval, @Nullable TagFilter tagFilter, Long updatedSince);

    public List<AbstractFacet> getFacetsBetween(ApiKey apiKey, ObjectType objectType, TimeInterval timeInterval, @Nullable TagFilter tagFilter, @Nullable String orderByString, Long updatedSince);

    public AbstractFacet getOldestFacet(ApiKey apiKey, ObjectType objectType);

    public AbstractFacet getLatestFacet(ApiKey apiKey, ObjectType objectType);

    List<AbstractFacet> getFacetsBefore(ApiKey apiKey, ObjectType objectType, long timeInMillis, int desiredCount);

    List<AbstractFacet> getFacetsAfter(ApiKey apiKey, ObjectType objectType, long timeInMillis, int desiredCount);

    List<AbstractFacet> getFacetsBefore(ApiKey apiKey, ObjectType objectType, long timeInMillis, int desiredCount, @Nullable TagFilter tagFilter);

    List<AbstractFacet> getFacetsAfter(ApiKey apiKey, ObjectType objectType, long timeInMillis, int desiredCount, @Nullable TagFilter tagFilter);

    public void deleteAllFacets(ApiKey apiKey);

    AbstractFacet getFacetById(ApiKey apiKey, final ObjectType objectType, final long facetId);

    public void deleteAllFacets(ApiKey apiKey, ObjectType objectType);

    public void persist(Object o);

    public void merge(Object o);

    public void delete(AbstractFacet facet);
}
