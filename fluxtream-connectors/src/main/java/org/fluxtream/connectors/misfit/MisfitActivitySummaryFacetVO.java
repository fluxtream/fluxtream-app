package org.fluxtream.connectors.misfit;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractTimedFacetVO;
import org.fluxtream.core.connectors.vos.AllDayVO;
import org.fluxtream.core.domain.GuestSettings;

/**
 * Created by candide on 24/02/15.
 */
public class MisfitActivitySummaryFacetVO  extends AbstractTimedFacetVO<MisfitActivitySummaryFacet> implements AllDayVO {

    public float points;
    public int steps;
    public float calories;
    public float distance;
    public float activityCalories;

    @Override
    public boolean allDay() {
        return true;
    }

    @Override
    protected void fromFacet(MisfitActivitySummaryFacet facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
        this.points = facet.points;
        this.steps = facet.steps;
        this.calories = facet.calories;
        this.activityCalories = facet.activityCalories;
        this.distance = facet.distance;
        this.date = facet.date;
    }

}
