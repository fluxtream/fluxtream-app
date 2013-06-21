package com.fluxtream.connectors.vos;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.models.DurationModel;

public abstract class AbstractTimedFacetVO<T extends AbstractFacet> extends AbstractInstantFacetVO<T> {

	public long end;
	public int endMinute;
    public TimeOfDayVO endTime;
    public DurationModel duration;

	@Override
	public void extractValues(T facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
		super.extractValues(facet, timeInterval, settings);
		this.end = facet.end;
        this.endTime = new TimeOfDayVO(endMinute, true);
	}
	
}
