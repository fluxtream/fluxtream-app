package com.fluxtream.connectors.lastfm;

import java.util.ArrayList;
import java.util.List;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractFacetVOCollection;
import com.fluxtream.domain.GuestSettings;

@SuppressWarnings("rawtypes")
public class LastFmFacetVOCollection extends AbstractFacetVOCollection {

	List<LastFmRecentTrackFacetVO> recentTracks;
	List<LastFmLovedTrackFacetVO> lovedTracks;
	
	@Override
	public void extractFacets(List facets, TimeInterval timeInterval, GuestSettings settings) {
		for (Object facet : facets) {
			if (facet instanceof LastFmLovedTrackFacet)
				addLovedTrack((LastFmLovedTrackFacet) facet, timeInterval, settings);
			else if (facet instanceof LastFmRecentTrackFacet)
				addRecentTrack((LastFmRecentTrackFacet) facet, timeInterval, settings);
		}
	}

	private void addLovedTrack(LastFmLovedTrackFacet facet, TimeInterval timeInterval,
			GuestSettings settings) {
		if (lovedTracks==null) lovedTracks = new ArrayList<LastFmLovedTrackFacetVO>();
		LastFmLovedTrackFacetVO jsonFacet = new LastFmLovedTrackFacetVO();
		jsonFacet.extractValues(facet, timeInterval, settings);
		lovedTracks.add(jsonFacet);
	}

	private void addRecentTrack(LastFmRecentTrackFacet facet, TimeInterval timeInterval,
			GuestSettings settings) {
		if (recentTracks==null) recentTracks = new ArrayList<LastFmRecentTrackFacetVO>();
		LastFmRecentTrackFacetVO jsonFacet = new LastFmRecentTrackFacetVO();
		jsonFacet.extractValues(facet, timeInterval, settings);
		recentTracks.add(jsonFacet);
	}

}
