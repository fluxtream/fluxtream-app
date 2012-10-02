package com.fluxtream.domain;

import java.util.List;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.services.ApiDataService;
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
    public List<AbstractFacet> findAll(final long guestId, final Connector connector, final ObjectType objectType, TimeInterval timeInterval) {
        return apiDataService.getApiDataFacets(guestId, connector, objectType, timeInterval);
    }

    @Override
    public List<AbstractFacet> findBefore(final long guestId, final Connector connector, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        return apiDataService.getApiDataFacetsBefore(guestId, connector, objectType, timeInMillis, desiredCount);
    }

    @Override
    public List<AbstractFacet> findAfter(final long guestId, final Connector connector, final ObjectType objectType, final long timeInMillis, final int desiredCount) {
        return apiDataService.getApiDataFacetsAfter(guestId, connector, objectType, timeInMillis, desiredCount);
    }

    @Override
    public AbstractFacet findOldest(final long guestId, final Connector connector, final ObjectType objectType) {
        return apiDataService.getOldestApiDataFacet(guestId, connector, objectType);
    }

    @Override
    public AbstractFacet findLatest(final long guestId, final Connector connector, final ObjectType objectType) {
        return apiDataService.getLatestApiDataFacet(guestId, connector, objectType);
    }
}
