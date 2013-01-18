package com.fluxtream.domain;

import java.util.List;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface PhotoFacetFinderStrategy {

    List<AbstractFacet> findAll(ApiKey apiKey, ObjectType objectType, TimeInterval timeInterval);

    List<AbstractFacet> findBefore(ApiKey apiKey, ObjectType objectType, long timeInMillis, int desiredCount);

    List<AbstractFacet> findAfter(ApiKey apiKey, ObjectType objectType, long timeInMillis, int desiredCount);

    AbstractFacet findOldest(ApiKey apiKey, ObjectType objectType);

    AbstractFacet findLatest(ApiKey apiKey, ObjectType objectType);
}