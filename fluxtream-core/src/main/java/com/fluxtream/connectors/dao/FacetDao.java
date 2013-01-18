package com.fluxtream.connectors.dao;

import java.util.List;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;

public interface FacetDao {

    public List<AbstractFacet> getFacetsByDates(ApiKey apiKey, ObjectType objectType, List<String> dates);

    public List<AbstractFacet> getFacetsBetween(ApiKey apiKey, ObjectType objectType, TimeInterval timeInterval);

    public AbstractFacet getOldestFacet(ApiKey apiKey, ObjectType objectType);

    public AbstractFacet getLatestFacet(ApiKey apiKey, ObjectType objectType);

    List<AbstractFacet> getFacetsBefore(ApiKey apiKey, ObjectType objectType, long timeInMillis, int desiredCount);

    List<AbstractFacet> getFacetsAfter(ApiKey apiKey, ObjectType objectType, long timeInMillis, int desiredCount);

    public void deleteAllFacets(ApiKey apiKey);

    public void deleteAllFacets(ApiKey apiKey, ObjectType objectType);

    public void persist(Object o);

    public void merge(Object o);
}
