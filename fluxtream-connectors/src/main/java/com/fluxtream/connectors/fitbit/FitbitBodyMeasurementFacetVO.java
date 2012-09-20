package com.fluxtream.connectors.fitbit;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;

public class FitbitBodyMeasurementFacetVO extends AbstractInstantFacetVO<FitbitBodyMeasurementFacet> {

    public double bicep;
    public double bmi;
    public double calf;
    public double chest;
    public double fat;
    public double fatPercentage;
    public double forearm;
    public double hips;
    public double neck;
    public double thigh;
    public double waist;
    public double weight;

    public String weightUnitLabel;

    @Override
    protected void fromFacet(final FitbitBodyMeasurementFacet facet, final TimeInterval timeInterval, final GuestSettings settings) {
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

        bicep = facet.bicep;
        bmi = facet.bmi;
        calf = facet.calf;
        chest = facet.chest;
        fat = round(facet.fat);
        fatPercentage = round((facet.fat/facet.weight) * 100d);
        forearm = facet.forearm;
        hips = facet.hips;
        neck = facet.neck;
        thigh = facet.thigh;
        waist = facet.waist;
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
