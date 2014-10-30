package org.fluxtream.connectors.fitbit;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * User: candide
 * Date: 27/10/14
 * Time: 01:08
 */
@Entity(name="Facet_FitbitFoodLogSummary")
@ObjectTypeSpec(name = "food_log_summary", value = 32, extractor= FitbitFoodLogFacetExtractor.class, prettyname = "Food Log Summary", isDateBased = true)
@NamedQueries({
        @NamedQuery(name = "fitbit.foodLog.summary.byDate",
                query = "SELECT facet FROM Facet_FitbitFoodLogSummary facet WHERE facet.apiKeyId=? AND facet.date=?"),
        @NamedQuery(name = "fitbit.foodLog.summary.latest",
                query = "SELECT facet FROM Facet_FitbitFoodLogSummary facet WHERE facet.apiKeyId=? ORDER BY facet.start DESC")
})
public class FitbitFoodLogSummaryFacet extends AbstractLocalTimeFacet {

    public float calories;
    public float carbs;
    public float fat;
    public float fiber;
    public float protein;
    public float sodium;
    public float water;
    public int caloriesGoal;
    public int caloriesOutGoal;

    public FitbitFoodLogSummaryFacet() {super();}

    public FitbitFoodLogSummaryFacet(final long apiKeyId) {super(apiKeyId);}

    @Override
    protected void makeFullTextIndexable() {

    }
}
