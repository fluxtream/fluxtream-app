package org.fluxtream.connectors.dao;

import java.util.List;
import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.ObjectType;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.domain.AbstractRepeatableFacet;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.domain.TagFilter;
import org.jetbrains.annotations.Nullable;

public interface FacetDao {

    public List<AbstractFacet> getFacetsByDates(ApiKey apiKey, ObjectType objectType, List<String> dates);

    public List<AbstractRepeatableFacet> getFacetsBetweenDates(ApiKey apiKey, ObjectType objectType, String startDate, String endDate);

    public List<AbstractFacet> getFacetsBetween(ApiKey apiKey, ObjectType objectType, TimeInterval timeInterval);

    public List<AbstractFacet> getFacetsBetween(ApiKey apiKey, ObjectType objectType, TimeInterval timeInterval, @Nullable TagFilter tagFilter);

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
}
