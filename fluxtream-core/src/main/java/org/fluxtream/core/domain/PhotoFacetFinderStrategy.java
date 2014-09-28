package org.fluxtream.core.domain;

import java.util.List;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.ObjectType;
import org.jetbrains.annotations.Nullable;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface PhotoFacetFinderStrategy {

    List<AbstractFacet> findAll(ApiKey apiKey, ObjectType objectType, TimeInterval timeInterval);

    List<AbstractFacet> findBefore(ApiKey apiKey, ObjectType objectType, long timeInMillis, int desiredCount);

    List<AbstractFacet> findAfter(ApiKey apiKey, ObjectType objectType, long timeInMillis, int desiredCount);

    List<AbstractFacet> findAll(ApiKey apiKey,
                                ObjectType objectType,
                                TimeInterval timeInterval,
                                @Nullable TagFilter tagFilter);

    List<AbstractFacet> findBefore(ApiKey apiKey,
                                   ObjectType objectType,
                                   long timeInMillis,
                                   int desiredCount,
                                   @Nullable TagFilter tagFilter);

    List findAfter(ApiKey apiKey, ObjectType objectType, long timeInMillis, int desiredCount, @Nullable TagFilter tagFilter);

    AbstractFacet findOldest(ApiKey apiKey, ObjectType objectType);

    AbstractFacet findLatest(ApiKey apiKey, ObjectType objectType);
}