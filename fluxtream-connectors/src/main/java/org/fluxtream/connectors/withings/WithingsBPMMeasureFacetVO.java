package org.fluxtream.connectors.withings;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractInstantFacetVO;
import org.fluxtream.core.domain.GuestSettings;

public class WithingsBPMMeasureFacetVO extends AbstractInstantFacetVO<WithingsBPMMeasureFacet> {

	float systolic, diastolic, pulse;
	
	@Override
	public void fromFacet(WithingsBPMMeasureFacet facet, TimeInterval timeInterval,
			GuestSettings settings) throws OutsideTimeBoundariesException {
        this.start = facet.start;
		systolic = facet.systolic;
		diastolic = facet.diastolic;
		pulse = facet.heartPulse;
		description = facet.systolic + "/" + facet.diastolic + " mmHg";
	}

}
