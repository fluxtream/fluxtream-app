package org.fluxtream.connectors.fitbit;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractLocalTimeTimedFacetVO;
import org.fluxtream.core.domain.GuestSettings;

/**
 * User: candide
 * Date: 31/10/14
 * Time: 21:08
 */
public class FitbitFoodLogEntryFacetVO extends AbstractLocalTimeTimedFacetVO<FitbitFoodLogEntryFacet> {

    public boolean isFavorite;
    public float amount;
    public String brand;
    public int calories;
    public int mealTypeId;
    public String locale;
    public String name;
    public String unitName;
    public String unitPlural;
    public float NV_Calories;
    public float NV_Carbs;
    public float NV_Fat;
    public float NV_Fiber;
    public float NV_Protein;
    public float NV_Sodium;

    @Override
    protected void fromFacet(FitbitFoodLogEntryFacet facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
        this.isFavorite = facet.isFavorite;
        this.amount = facet.amount;
        this.brand = facet.brand;
        this.calories = facet.calories;
        this.mealTypeId = facet.mealTypeId;
        this.locale = facet.locale;
        this.name = facet.name;
        this.unitName = facet.unitName;
        this.unitPlural = facet.unitPlural;
        this.NV_Calories = facet.NV_Calories;
        this.NV_Carbs = facet.NV_Carbs;
        this.NV_Fat = facet.NV_Fat;
        this.NV_Fiber = facet.NV_Fiber;
        this.NV_Protein = facet.NV_Protein;
        this.NV_Sodium = facet.NV_Sodium;
    }

}
