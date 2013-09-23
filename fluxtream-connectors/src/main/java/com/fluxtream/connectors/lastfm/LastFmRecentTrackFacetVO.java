package com.fluxtream.connectors.lastfm;

import java.util.Date;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;

public class LastFmRecentTrackFacetVO extends AbstractInstantFacetVO<LastFmRecentTrackFacet>{

	public String artist;
	public String album_mbid;
	public String artist_mbid;
	public String url;
    public String title;
	public String[] imgUrls;
	
	@Override
	public void fromFacet(LastFmRecentTrackFacet facet, TimeInterval timeInterval,
			GuestSettings settings) throws OutsideTimeBoundariesException {
		startMinute = toMinuteOfDay(new Date(facet.time), timeInterval.getTimeZone(facet.start));
		artist = facet.artist;
		this.album_mbid = facet.album_mbid;
		this.artist_mbid = facet.artist_mbid;
		this.url = facet.url;
		if (facet.imgUrls!=null) {
			this.imgUrls = facet.imgUrls.split(",");
		}
        this.title = facet.name;
		description = facet.artist + ": " + facet.name;
	}

}
