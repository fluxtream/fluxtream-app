package org.fluxtream.connectors.withings;

import org.fluxtream.OutsideTimeBoundariesException;
import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.vos.AbstractLocalTimeInstantFacetVO;
import org.fluxtream.domain.GuestSettings;

/**
 * User: candide
 * Date: 25/11/13
 * Time: 14:08
 */
public class WithingsActivityFacetVO extends AbstractLocalTimeInstantFacetVO<WithingsActivityFacet> {

    public float calories;
    public int steps;
    public float elevation;
    public float distance;

    @Override
    protected void fromFacet(final WithingsActivityFacet facet, final TimeInterval timeInterval, final GuestSettings settings) throws OutsideTimeBoundariesException {
        this.date = facet.date;
        this.calories = facet.calories;
        this.steps = facet.steps;
        this.elevation = facet.elevation;
        this.distance = facet.distance;
    }

}
