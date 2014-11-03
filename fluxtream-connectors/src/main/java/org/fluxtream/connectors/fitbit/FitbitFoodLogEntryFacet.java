package org.fluxtream.connectors.fitbit;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * User: candide
 * Date: 27/10/14
 * Time: 01:06
 */
@Entity(name="Facet_FitbitFoodLogEntry")
@ObjectTypeSpec(name = "food_log_entry", value = 16, extractor= FitbitFoodLogFacetExtractor.class, prettyname = "Food Log Entry", isDateBased = true,
    orderBy = "mealTypeId")
@NamedQueries({
        @NamedQuery(name = "fitbit.foodLog.entry.byDate",
                query = "SELECT facet FROM Facet_FitbitFoodLogEntry facet WHERE facet.apiKeyId=? AND facet.date=?"),
        @NamedQuery(name = "fitbit.foodLog.entry.latest",
                query = "SELECT facet FROM Facet_FitbitFoodLogEntry facet WHERE facet.apiKeyId=? ORDER BY facet.start DESC")
})
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

    public FitbitFoodLogEntryFacet(final long apiKeyId) {super(apiKeyId);}

    @Override
    protected void makeFullTextIndexable() {

    }
}
