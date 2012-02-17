package com.fluxtream.connectors.zeo;

import java.util.ArrayList;
import java.util.List;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractFacetVOCollection;
import com.fluxtream.domain.GuestSettings;

public class ZeoFacetVOCollection extends AbstractFacetVOCollection<ZeoSleepStatsFacet>{

	List<ZeoSleepStatsFacetVO> sleepMeasures;
	
	@Override
	public void extractFacets(List<ZeoSleepStatsFacet> facets, TimeInterval timeInterval,
			GuestSettings settings) {
		if (facets.size()==0) return;
		sleepMeasures = new ArrayList<ZeoSleepStatsFacetVO>();
		for (ZeoSleepStatsFacet zeoSleepStatsFacet : facets) {
			ZeoSleepStatsFacetVO jsonFacet = new ZeoSleepStatsFacetVO();
			jsonFacet.extractValues(zeoSleepStatsFacet, timeInterval, settings);
			sleepMeasures.add(jsonFacet);
		}
	}

}
