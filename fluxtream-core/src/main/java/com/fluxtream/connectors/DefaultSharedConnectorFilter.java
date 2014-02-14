package com.fluxtream.connectors;

import java.util.List;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.SharedConnector;

/**
 * User: candide
 * Date: 13/02/14
 * Time: 18:33
 */
public class DefaultSharedConnectorFilter implements SharedConnectorFilter {

    @Override
    public List<AbstractFacet> filterFacets(final SharedConnector sharedConnector, List<AbstractFacet> facets) {
        return facets;
    }
}
