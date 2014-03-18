package org.fluxtream.connectors.withings;

import org.fluxtream.OutsideTimeBoundariesException;
import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.vos.AbstractInstantFacetVO;
import org.fluxtream.domain.GuestSettings;

public class WithingsHeartPulseMeasureFacetVO extends AbstractInstantFacetVO<WithingsHeartPulseMeasureFacet> {

	float pulse;
	
	@Override
	public void fromFacet(WithingsHeartPulseMeasureFacet facet, TimeInterval timeInterval,
			GuestSettings settings) throws OutsideTimeBoundariesException {
		pulse = facet.heartPulse;
		description = facet.heartPulse + " bpm";
	}

}
