package org.fluxtream.connectors.fitbit;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractLocalTimeTimedFacetVO;
import org.fluxtream.core.connectors.vos.AllDayVO;
import org.fluxtream.core.domain.GuestSettings;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * User: candide
 * Date: 31/10/14
 * Time: 21:08
 */
public class FitbitFoodLogEntryFacetVO extends AbstractLocalTimeTimedFacetVO<FitbitFoodLogEntryFacet> implements AllDayVO {

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

    public String unit;
    public String meal;

    @Override
    protected void fromFacet(FitbitFoodLogEntryFacet facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
        this.isFavorite = facet.isFavorite;
        this.amount = facet.amount;
        this.brand = facet.brand;
        this.calories = facet.calories;
        this.mealTypeId = facet.mealTypeId;
        // the following is a hack: eventStart is computed off facet.start so we modify that in order
        // to set eventStart to the value we want
        // the arbitrary times of day are just a way to get the facets to be properly ordered in the web app
        final DateTime entryStartTime = ISODateTimeFormat.dateTime().parseDateTime(facet.startTimeStorage+"Z");
        switch (mealTypeId) {
            case 1:
                facet.start = entryStartTime.plusHours(8).getMillis();
                meal="Breakfast";
                break;
            case 2:
                facet.start = entryStartTime.plusHours(10).getMillis();
                meal="Morning Snack";
                break;
            case 3:
                facet.start = entryStartTime.plusHours(13).getMillis();
                meal="Lunch";
                break;
            case 4:
                facet.start = entryStartTime.plusHours(17).getMillis();
                meal="Afternoon Snack";
                break;
            case 5:
                facet.start = entryStartTime.plusHours(19).getMillis();
                meal="Dinner";
                break;
            case 6:
                facet.start = entryStartTime.plusHours(22).getMillis();
                meal="After Dinner";
                break;
            default:
                facet.start = entryStartTime.plusHours(6).getMillis();
                meal="Anytime";
                break;
        }
        this.locale = facet.locale;
        this.name = facet.name;
        this.unitName = facet.unitName;
        this.unitPlural = facet.unitPlural;
        this.unit = this.amount>1.?unitPlural:unitName;
        this.NV_Calories = facet.NV_Calories;
        this.NV_Carbs = facet.NV_Carbs;
        this.NV_Fat = facet.NV_Fat;
        this.NV_Fiber = facet.NV_Fiber;
        this.NV_Protein = facet.NV_Protein;
        this.NV_Sodium = facet.NV_Sodium;
    }

    @Override
    public boolean allDay() {
        return true;
    }
}
