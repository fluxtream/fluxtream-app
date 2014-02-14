package com.fluxtream.connectors;

import java.util.List;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.SharedConnector;

/**
 * User: candide
 * Date: 13/02/14
 * Time: 18:12
 */

public interface SharedConnectorFilter {

    List<AbstractFacet> filterFacets(SharedConnector sharedConnector, List<AbstractFacet> facets);

}
