package org.fluxtream.connectors.fluxtream_capture;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractInstantFacetVO;
import org.fluxtream.core.domain.GuestSettings;

/**
 * Created by candide on 18/02/15.
 */
public class FluxtreamObservationFacetVO extends AbstractInstantFacetVO<FluxtreamObservationFacet> {

    Integer value;
    String topicName;

    @Override
    protected void fromFacet(FluxtreamObservationFacet facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
        this.start = facet.start;
        this.value = facet.value;
        this.topicName = settings.topics.get("topic_" + facet.topicId);
    }

}
