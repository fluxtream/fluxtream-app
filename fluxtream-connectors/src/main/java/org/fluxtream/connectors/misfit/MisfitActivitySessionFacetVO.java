package org.fluxtream.connectors.misfit;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractTimedFacetVO;
import org.fluxtream.core.domain.GuestSettings;

/**
 * Created by candide on 24/02/15.
 */
public class MisfitActivitySessionFacetVO extends AbstractTimedFacetVO<MisfitActivitySessionFacet> {

    public float points;
    public int steps;
    public float calories;
    public float activityCalories;
    public float distance;
    public String activityType;

    @Override
    protected void fromFacet(MisfitActivitySessionFacet facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
        this.points = facet.points;
        this.steps = facet.steps;
        this.calories = facet.calories;
        this.activityCalories = facet.calories;
        this.distance = facet.distance;
        this.activityType = facet.activityType;
    }
}
