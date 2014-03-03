package org.fluxtream.connectors;

import java.util.List;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.domain.SharedConnector;

/**
 * User: candide
 * Date: 13/02/14
 * Time: 18:12
 */

public interface SharedConnectorFilter {

    <T extends AbstractFacet> List<T> filterFacets(SharedConnector sharedConnector, List<T> facets);

}
