package com.fluxtream.connectors.lastfm;

import java.util.ArrayList;
import java.util.List;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractFacetVOCollection;
import com.fluxtream.domain.GuestSettings;

@SuppressWarnings("rawtypes")
public class LastFmFacetVOCollection extends AbstractFacetVOCollection {

	List<LastFmRecentTrackFacetVO> recentTracks;

	@Override
	public void extractFacets(List facets, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
		for (Object facet : facets) {
            if (facet instanceof LastFmRecentTrackFacet)
				addRecentTrack((LastFmRecentTrackFacet) facet, timeInterval, settings);
		}
	}

	private void addRecentTrack(LastFmRecentTrackFacet facet, TimeInterval timeInterval,
			GuestSettings settings) throws OutsideTimeBoundariesException {
		if (recentTracks==null) recentTracks = new ArrayList<LastFmRecentTrackFacetVO>();
		LastFmRecentTrackFacetVO jsonFacet = new LastFmRecentTrackFacetVO();
		jsonFacet.extractValues(facet, timeInterval, settings);
		recentTracks.add(jsonFacet);
	}

}
