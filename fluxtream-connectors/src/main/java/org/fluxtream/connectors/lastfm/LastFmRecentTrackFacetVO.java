package org.fluxtream.connectors.lastfm;

import java.util.Date;

import org.fluxtream.OutsideTimeBoundariesException;
import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.vos.AbstractInstantFacetVO;
import org.fluxtream.domain.GuestSettings;

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
        for (int i=0; i<imgUrls.length; i++) {
            this.imgUrls[i] = String.format("%slastfm/img?url=%s", settings.config.get("homeBaseUrl"),imgUrls[i]);
        }
        this.title = facet.name;
		description = facet.artist + ": " + facet.name;
	}

}
