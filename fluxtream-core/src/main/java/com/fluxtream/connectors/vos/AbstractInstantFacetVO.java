package com.fluxtream.connectors.vos;

import com.fluxtream.TimeInterval;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.GuestSettings;

public abstract class AbstractInstantFacetVO<T extends AbstractFacet> extends
		AbstractFacetVO<T> implements Comparable<AbstractInstantFacetVO<T>> {

	public long start;
	public int startMinute;
    public TimeOfDayVO startTime;

	@Override
	public void extractValues(T facet, TimeInterval timeInterval, GuestSettings settings) {
		super.extractValues(facet, timeInterval, settings);
		this.start = facet.start;
        this.startTime = new TimeOfDayVO(startMinute);
    }
	
	@Override
	public int compareTo(AbstractInstantFacetVO<T> other) {
		return this.start > other.start ? 1 : -1;
	}

}
