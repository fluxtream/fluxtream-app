package com.fluxtream.connectors.fitbit;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;

public class FitbitWeightFacetVO extends AbstractInstantFacetVO<FitbitWeightFacet> {

    public double bmi;
    public double fat;
    public double weight;
    public double weightInKilos;

    public String weightUnitLabel;

    @Override
    protected void fromFacet(final FitbitWeightFacet facet, final TimeInterval timeInterval, final GuestSettings settings) {
        if (facet.startTimeStorage!=null)
            this.startMinute = minuteOfDayFromTimeStorage(facet.startTimeStorage);
        else
            this.startMinute = 0;
        switch (settings.weightMeasureUnit) {
            case SI:
                this.weightUnitLabel = "kg";
                break;
            case STONES:
                this.weightUnitLabel = "stones";
                break;
            default:
                this.weightUnitLabel = "lb";
        }
        bmi = round(facet.bmi);
        fat = round(facet.fat);
        this.weightInKilos = facet.weight;
        format(facet.weight, settings.weightMeasureUnit);
    }

    private void format(double weight, GuestSettings.WeightMeasureUnit weightMeasureUnit) {
        switch (weightMeasureUnit) {
            case SI:
                this.weight = round(weight);
                break;
            case STONES:
                this.weight = round(weight * 0.157473044f);
                break;
            default:
                this.weight = round(weight * 2.20462262f);
        }
    }

    double round(double v) {
        return (double) Math.round(v * 100) / 100;
    }

}
