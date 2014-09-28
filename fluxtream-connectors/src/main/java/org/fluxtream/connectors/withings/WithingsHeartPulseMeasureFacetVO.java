package org.fluxtream.connectors.withings;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractInstantFacetVO;
import org.fluxtream.core.domain.GuestSettings;

public class WithingsHeartPulseMeasureFacetVO extends AbstractInstantFacetVO<WithingsHeartPulseMeasureFacet> {

	float pulse;
	
	@Override
	public void fromFacet(WithingsHeartPulseMeasureFacet facet, TimeInterval timeInterval,
			GuestSettings settings) throws OutsideTimeBoundariesException {
		pulse = facet.heartPulse;
		description = facet.heartPulse + " bpm";
	}

}
