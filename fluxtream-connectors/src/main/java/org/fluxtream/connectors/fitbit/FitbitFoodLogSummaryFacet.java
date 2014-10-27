package org.fluxtream.connectors.fitbit;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;

import javax.persistence.Entity;

/**
 * User: candide
 * Date: 27/10/14
 * Time: 01:08
 */
@Entity(name="Facet_FitbitFoodLogSummary")
@ObjectTypeSpec(name = "food_log_summary", value = 32, prettyname = "Food Log Summary", isDateBased = true)
public class FitbitFoodLogSummaryFacet extends AbstractLocalTimeFacet {

    public float calories;
    public float carbs;
    public float fat;
    public float fiber;
    public float protein;
    public float sodium;
    public float water;

    public FitbitFoodLogSummaryFacet() {super();}

    @Override
    protected void makeFullTextIndexable() {

    }
}
