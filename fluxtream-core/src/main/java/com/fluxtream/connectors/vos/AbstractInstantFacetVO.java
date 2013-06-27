package com.fluxtream.connectors.vos;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.AbstractLocalTimeFacet;
import com.fluxtream.domain.GuestSettings;

public abstract class AbstractInstantFacetVO<T extends AbstractFacet> extends
		AbstractFacetVO<T> implements Comparable<AbstractInstantFacetVO<T>> {

	public long start;
	public int startMinute;
    public TimeOfDayVO startTime;

	@Override
	public void extractValues(T facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
        if (facet instanceof AbstractLocalTimeFacet) {
            AbstractLocalTimeFacet ltf = (AbstractLocalTimeFacet) facet;
            this.start = ltf.start - timeInterval.getTimeZone(ltf.date).getOffset(System.currentTimeMillis());
        } else
            this.start = facet.start;
		super.extractValues(facet, timeInterval, settings);
        this.startTime = new TimeOfDayVO(startMinute, true);
    }
	
	@Override
	public int compareTo(AbstractInstantFacetVO<T> other) {
		return this.start > other.start ? 1 : -1;
	}

}
