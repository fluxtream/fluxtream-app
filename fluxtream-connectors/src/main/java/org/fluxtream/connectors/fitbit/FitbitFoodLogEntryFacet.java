package org.fluxtream.connectors.fitbit;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;

import javax.persistence.Entity;

/**
 * User: candide
 * Date: 27/10/14
 * Time: 01:06
 */
@Entity(name="Facet_FitbitFoodLogEntry")
@ObjectTypeSpec(name = "food_log_entry", value = 16, prettyname = "Food Log Entry", isDateBased = true)
public class FitbitFoodLogEntryFacet extends AbstractLocalTimeFacet {

    public String accessLevel;
    public boolean isFavorite;
    public long logId;
    public float amount;
    public String brand;
    public int calories;
    public long foodId;
    public int mealTypeId;
    public String locale;
    public String name;
    public int unitId;
    public String unitName;
    public String unitPlural;
    public float NV_Calories;
    public float NV_Carbs;
    public float NV_Fat;
    public float NV_Fiber;
    public float NV_Protein;
    public float NV_Sodium;

    public FitbitFoodLogEntryFacet() {super();}

    @Override
    protected void makeFullTextIndexable() {

    }
}
