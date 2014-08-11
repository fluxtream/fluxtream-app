package org.fluxtream.connectors.moves;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.domain.GuestSettings;

/**
 * User: candide
 * Date: 20/06/13
 * Time: 17:10
 */
public class MovesMoveFacetVO extends AbstractMovesFacetVO<MovesMoveFacet> {

    @Override
    protected void fromFacet(final MovesMoveFacet facet, final TimeInterval timeInterval, final GuestSettings settings) throws OutsideTimeBoundariesException {
        super.fromFacetBase(facet, timeInterval, settings);
    }
}
