package org.fluxtream.connectors.bodymedia;

import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.vos.AbstractInstantFacetVO;
import org.fluxtream.domain.GuestSettings;

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
