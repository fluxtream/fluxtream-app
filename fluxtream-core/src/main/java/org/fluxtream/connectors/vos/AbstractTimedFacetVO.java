package org.fluxtream.connectors.vos;

import org.fluxtream.OutsideTimeBoundariesException;
import org.fluxtream.TimeInterval;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.domain.GuestSettings;
import org.fluxtream.mvc.models.DurationModel;

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
