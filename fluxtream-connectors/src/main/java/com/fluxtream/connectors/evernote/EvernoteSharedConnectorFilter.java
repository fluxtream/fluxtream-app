package com.fluxtream.connectors.evernote;

import java.util.List;
import com.fluxtream.connectors.SharedConnectorFilter;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.SharedConnector;

/**
 * User: candide
 * Date: 19/02/14
 * Time: 16:42
 */
public class EvernoteSharedConnectorFilter implements SharedConnectorFilter {

    @Override
    public <T extends AbstractFacet> List<T> filterFacets(final SharedConnector sharedConnector, final List<T> facets) {
        return facets;
    }

}
