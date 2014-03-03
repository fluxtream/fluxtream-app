package org.fluxtream.domain;

import java.util.List;
import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.ObjectType;
import org.fluxtream.services.ApiDataService;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
@Component
public class DefaultPhotoFacetFinderStrategy implements PhotoFacetFinderStrategy {
    @Autowired
    private ApiDataService apiDataService;

    @Override
    public List<AbstractFacet> findAll(final ApiKey apiKey, final ObjectType objectType, TimeInterval timeInterval) {
        return apiDataService.getApiDataFacets(apiKey, objectType, timeInterval);
    }

    @Override
    public List<AbstractFacet> findBefore(final ApiKey apiKey, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        return apiDataService.getApiDataFacetsBefore(apiKey, objectType, timeInMillis, desiredCount);
    }

    @Override
    public List<AbstractFacet> findAfter(final ApiKey apiKey, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        return apiDataService.getApiDataFacetsAfter(apiKey, objectType, timeInMillis, desiredCount);
    }

    @Override
    public List<AbstractFacet> findAll(final ApiKey apiKey,
                                       final ObjectType objectType,
                                       final TimeInterval timeInterval,
                                       @Nullable final TagFilter tagFilter) {
        return apiDataService.getApiDataFacets(apiKey, objectType, timeInterval, tagFilter);
    }

    @Override
    public List<AbstractFacet> findBefore(final ApiKey apiKey,
                                          final ObjectType objectType,
                                          final long timeInMillis,
                                          final int desiredCount,
                                          @Nullable final TagFilter tagFilter) {
        return apiDataService.getApiDataFacetsBefore(apiKey, objectType, timeInMillis, desiredCount, tagFilter);
    }

    @Override
    public List<AbstractFacet> findAfter(final ApiKey apiKey,
                                         final ObjectType objectType,
                                         final long timeInMillis,
                                         final int desiredCount,
                                         @Nullable final TagFilter tagFilter) {
        return apiDataService.getApiDataFacetsAfter(apiKey, objectType, timeInMillis, desiredCount, tagFilter);
    }

    @Override
    public AbstractFacet findOldest(final ApiKey apiKey, final ObjectType objectType) {
        return apiDataService.getOldestApiDataFacet(apiKey, objectType);
    }

    @Override
    public AbstractFacet findLatest(final ApiKey apiKey, final ObjectType objectType) {
        return apiDataService.getLatestApiDataFacet(apiKey, objectType);
    }
}
