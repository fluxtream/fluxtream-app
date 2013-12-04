package com.fluxtream.connectors.vos;

import java.util.List;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.GuestSettings;

public abstract class AbstractFacetVOCollection<T extends AbstractFacet> {

	public transient boolean hasData;
	
	public void fromFacets(List<T> facets, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
		hasData = facets.size()>0;
		extractFacets(facets, timeInterval, settings);
	}
	
	protected abstract void extractFacets(List<T> facets, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException;
		
}
