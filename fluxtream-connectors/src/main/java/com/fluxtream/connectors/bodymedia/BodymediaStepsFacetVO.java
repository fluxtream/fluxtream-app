package com.fluxtream.connectors.bodymedia;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;

public class BodymediaStepsFacetVO extends AbstractInstantFacetVO<BodymediaStepsFacet> {

    public int totalSteps;

    public boolean allDay = true;

//    public String stepsJson;

    @Override
    protected void fromFacet(final BodymediaStepsFacet facet, final TimeInterval timeInterval, final GuestSettings settings) {
        this.totalSteps = facet.totalSteps;
//        this.stepsJson = facet.json;
        this.date = facet.date;
    }
}
