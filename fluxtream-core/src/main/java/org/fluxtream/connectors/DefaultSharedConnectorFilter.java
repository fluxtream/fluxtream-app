package org.fluxtream.connectors;

import java.util.List;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.domain.SharedConnector;

/**
 * User: candide
 * Date: 13/02/14
 * Time: 18:33
 */
public class DefaultSharedConnectorFilter implements SharedConnectorFilter {

    @Override
    public <T extends AbstractFacet>  List<T> filterFacets(final SharedConnector sharedConnector, List<T> facets) {
        return facets;
    }
}
