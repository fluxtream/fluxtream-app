package org.fluxtream.connectors.misfit;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractTimedFacetVO;
import org.fluxtream.core.domain.GuestSettings;

import java.text.NumberFormat;

/**
 * Created by candide on 24/02/15.
 */
public class MisfitActivitySessionFacetVO extends AbstractTimedFacetVO<MisfitActivitySessionFacet> {

    public float points;
    public int steps;
    public float calories;
    public float activityCalories;
    public String distance;
    public String activityType;


    @Override
    protected void fromFacet(MisfitActivitySessionFacet facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
        this.points = facet.points;
        this.steps = facet.steps;
        this.calories = facet.calories;
        this.activityCalories = facet.calories;
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        if (settings.distanceMeasureUnit== GuestSettings.DistanceMeasureUnit.MILES_YARDS)
            this.distance = numberFormat.format(facet.distance*0.621371f) + " Miles";
        else
            this.distance = numberFormat.format(facet.distance) + " Km";
        this.activityType = facet.activityType;
    }
}
