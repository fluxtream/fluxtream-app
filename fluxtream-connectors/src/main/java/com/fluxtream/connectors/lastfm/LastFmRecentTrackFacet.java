package com.fluxtream.connectors.lastfm;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@Entity(name="Facet_LastFmRecentTrack")
@ObjectTypeSpec(name = "recent_track", value = 2, extractor=LastFmFacetExtractor.class, parallel=true, prettyname = "Recent Tracks",
            detailsTemplate="<h3 class=\"flx-dataType\">Track</h3>" +
                            "<span class=\"flx-deviceIcon\" style=\"background:url('{{imgUrl}}'); background-size:100% 100%;\"></span>" +
                            "<div class=\"flx-deviceData\">" +
                            "    <span class=\"flx-tTime\">{{time}}</span>" +
                            "    <span class=\"flx-data\">{{description}}</span>" +
                            "</div>")
@NamedQueries({
		@NamedQuery(name = "lastfm.recent_track.byStartEnd", query = "SELECT facet FROM Facet_LastFmRecentTrack facet WHERE facet.guestId=? AND facet.start=? AND facet.end=?"),
		@NamedQuery(name = "lastfm.recent_track.newest", query = "SELECT facet FROM Facet_LastFmRecentTrack facet WHERE facet.guestId=? ORDER BY time DESC LIMIT 1"),
		@NamedQuery(name = "lastfm.recent_track.deleteAll", query = "DELETE FROM Facet_LastFmRecentTrack facet WHERE facet.guestId=?"),
		@NamedQuery(name = "lastfm.recent_track.between", query = "SELECT facet FROM Facet_LastFmRecentTrack facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class LastFmRecentTrackFacet extends AbstractFacet {

    String artist;
    String artist_mbid;
    String name;
    String album_mbid;
    String url;
    @Lob
    String imgUrls;
    long time;
    
	@Override
	protected void makeFullTextIndexable() {
		this.fullTextDescription = artist + " " + name;
	}

}