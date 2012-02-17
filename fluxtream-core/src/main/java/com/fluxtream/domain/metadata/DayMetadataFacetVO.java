package com.fluxtream.domain.metadata;

import java.util.Date;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.domain.GuestSettings;

public class DayMetadataFacetVO extends AbstractTimedFacetVO<DayMetadataFacet> {

	public String title;
	
	@Override
	protected void fromFacet(DayMetadataFacet facet, TimeInterval timeInterval,
			GuestSettings settings) {
		startMinute = toMinuteOfDay(new Date(facet.start), timeInterval.timeZone);
		endMinute = toMinuteOfDay(new Date(facet.end), timeInterval.timeZone);
		description = facet.cities;
		title = facet.title;
	}

}
