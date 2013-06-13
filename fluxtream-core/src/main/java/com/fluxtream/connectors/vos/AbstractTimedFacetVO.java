package com.fluxtream.connectors.vos;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.GuestSettings;

public abstract class AbstractTimedFacetVO<T extends AbstractFacet> extends AbstractInstantFacetVO<T> {

	public long end;
	public int endMinute;
    public TimeOfDayVO endTime;

	@Override
	public void extractValues(T facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
		super.extractValues(facet, timeInterval, settings);
		this.end = facet.end;
        this.endTime = new TimeOfDayVO(endMinute, settings.distanceMeasureUnit == GuestSettings.DistanceMeasureUnit.MILES_YARDS);
	}
	
}
