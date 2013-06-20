package com.fluxtream.connectors.withings;

import java.util.Date;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;

public class WithingsBPMMeasureFacetVO extends AbstractInstantFacetVO<WithingsBPMMeasureFacet> {

	float systolic, diastolic, pulse;
	
	@Override
	public void fromFacet(WithingsBPMMeasureFacet facet, TimeInterval timeInterval,
			GuestSettings settings) throws OutsideTimeBoundariesException {
		this.startMinute = toMinuteOfDay(new Date(facet.measureTime), timeInterval.getTimeZone(facet.measureTime));
        this.start = facet.start;
		systolic = facet.systolic;
		diastolic = facet.diastolic;
		pulse = facet.heartPulse;
		description = facet.systolic + "/" + facet.diastolic + " mmHg";
	}

}
