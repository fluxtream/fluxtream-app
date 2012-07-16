package com.fluxtream.connectors.bodymedia;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.domain.GuestSettings;

@SuppressWarnings("unused")
public class BodymediaStepsFacetVO extends AbstractFacetVO<BodymediaStepsFacet>{

    public int totalSteps;

    public String stepsJson;

    @Override
    protected void fromFacet(final BodymediaStepsFacet facet, final TimeInterval timeInterval, final GuestSettings settings) {
        this.totalSteps = facet.totalSteps;
        this.stepsJson = facet.json;
    }
}
