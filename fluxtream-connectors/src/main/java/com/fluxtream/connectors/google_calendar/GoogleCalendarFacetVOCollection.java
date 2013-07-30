package com.fluxtream.connectors.google_calendar;

import java.util.ArrayList;
import java.util.List;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractFacetVOCollection;
import com.fluxtream.domain.GuestSettings;

public class GoogleCalendarFacetVOCollection extends AbstractFacetVOCollection<GoogleCalendarEntryFacet> {

	List<GoogleCalendarEntryFacetVO> entries;
	
	@Override
	public void extractFacets(List<GoogleCalendarEntryFacet> facets, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
		if (facets.size()==0) return;
		entries = new ArrayList<GoogleCalendarEntryFacetVO>();
		for (GoogleCalendarEntryFacet entry : facets) {
			GoogleCalendarEntryFacetVO jsonFacet = new GoogleCalendarEntryFacetVO();
			jsonFacet.extractValues(entry, timeInterval, settings);
			entries.add(jsonFacet);
		}
	}

}
