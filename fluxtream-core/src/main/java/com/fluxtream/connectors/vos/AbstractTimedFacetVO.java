package com.fluxtream.connectors.vos;

import com.fluxtream.TimeInterval;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.GuestSettings;

public abstract class AbstractTimedFacetVO<T extends AbstractFacet> extends AbstractInstantFacetVO<T> {

	public long end;
	public int endMinute;

	@Override
	public void extractValues(T facet, TimeInterval timeInterval, GuestSettings settings) {
		super.extractValues(facet, timeInterval, settings);
		this.end = facet.end;
	}
	
}
