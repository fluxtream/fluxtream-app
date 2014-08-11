package org.fluxtream.connectors.bodymedia;

import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractInstantFacetVO;
import org.fluxtream.core.domain.GuestSettings;

public class BodymediaBurnFacetVO extends AbstractInstantFacetVO<BodymediaBurnFacet> {

    public int totalCalories = 0;
    public int estimatedCalories = 0;
    public int predictedCalories = 0;

    public boolean allDay = true;

//    public String burnJson;

    @Override
    protected void fromFacet(final BodymediaBurnFacet facet, final TimeInterval timeInterval, final GuestSettings settings) {
        this.totalCalories = facet.totalCalories;
        this.estimatedCalories = facet.estimatedCalories;
        this.predictedCalories = facet.predictedCalories;
        this.date = facet.date;
//        this.burnJson = facet.getJson();
    }
}
