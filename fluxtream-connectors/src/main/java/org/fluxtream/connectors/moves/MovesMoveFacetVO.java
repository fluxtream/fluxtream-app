package org.fluxtream.connectors.moves;

import org.fluxtream.OutsideTimeBoundariesException;
import org.fluxtream.TimeInterval;
import org.fluxtream.domain.GuestSettings;

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
