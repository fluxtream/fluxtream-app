package com.fluxtream.connectors.withings;

import java.util.Date;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;

public class WithingsBPMMeasureFacetVO extends AbstractInstantFacetVO<WithingsBPMMeasureFacet> {

	float systolic, diastolic, pulse;
	
	@Override
	public void fromFacet(WithingsBPMMeasureFacet facet, TimeInterval timeInterval,
			GuestSettings settings) {
		this.startMinute = toMinuteOfDay(new Date(facet.measureTime), timeInterval.getMainTimeZone());
		systolic = facet.systolic;
		diastolic = facet.diastolic;
		pulse = facet.heartPulse;
		description = facet.systolic + "/" + facet.diastolic + " mmHg";
	}

}
