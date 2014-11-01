package org.fluxtream.connectors.fitbit;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractLocalTimeTimedFacetVO;
import org.fluxtream.core.connectors.vos.AllDayVO;
import org.fluxtream.core.domain.GuestSettings;

/**
 * User: candide
 * Date: 31/10/14
 * Time: 21:09
 */
public class FitbitFoodLogSummaryFacetVO extends AbstractLocalTimeTimedFacetVO<FitbitFoodLogSummaryFacet> implements AllDayVO {

    public float calories;
    public float carbs;
    public float fat;
    public float fiber;
    public float protein;
    public float sodium;
    public float water;
    public int caloriesGoal;
    public int caloriesOutGoal;

    @Override
    protected void fromFacet(FitbitFoodLogSummaryFacet facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
        if (facet.calories==0) this.isEmpty = true;
        this.calories = facet.calories;
        this.carbs = facet.carbs;
        this.fiber = facet.fiber;
        this.protein = facet.protein;
        this.sodium = facet.sodium;
        this.water = facet.water;
        this.caloriesGoal = facet.caloriesGoal;
        this.caloriesOutGoal = facet.caloriesOutGoal;
    }

    @Override
    public boolean allDay() {
        return true;
    }

}
