package com.fluxtream.connectors.lastfm;

import java.util.Date;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;

public class LastFmLovedTrackFacetVO extends AbstractInstantFacetVO<LastFmLovedTrackFacet> {

	public String artist;
	public String album_mbid;
	public String artist_mbid;
	public String url;
	public String[] imgUrls;

	@Override
	public void fromFacet(LastFmLovedTrackFacet facet, TimeInterval timeInterval,
			GuestSettings settings) {
		startMinute = toMinuteOfDay(new Date(facet.time), timeInterval.timeZone);
		artist = facet.artist;
		this.album_mbid = facet.album_mbid;
		this.artist_mbid = facet.artist_mbid;
		this.url = facet.url;
		if (facet.imgUrls!=null) {
			this.imgUrls = facet.imgUrls.split(",");
		}
		description = facet.artist + ": " + facet.name;
	}

}
