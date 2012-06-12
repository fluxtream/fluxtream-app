package com.fluxtream.connectors.lastfm;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@Entity(name="Facet_LastFmLovedTrack")
@ObjectTypeSpec(name = "loved_track", value = 1, extractor=LastFmFacetExtractor.class, parallel=true, prettyname = "Loved Tracks",
                detailsTemplate="<h3 class=\"flx-dataType\">Loved Track</h3>" +
                                "<span class=\"flx-deviceIcon\" style=\"background:url('{{imgUrl}}'); background-size:100% 100%;\"></span>" +
                                "<div class=\"flx-deviceData\">" +
                                "    <span class=\"flx-tTime\">{{time}}</span>" +
                                "    <span class=\"flx-data\">{{description}}</span>" +
                                "</div>")
@NamedQueries({
		@NamedQuery(name = "lastfm.loved_track.byStartEnd", query = "SELECT facet FROM Facet_LastFmLovedTrack facet WHERE facet.guestId=? AND facet.start=? AND facet.end=?"),
		@NamedQuery(name = "lastfm.loved_track.newest", query = "SELECT facet FROM Facet_LastFmLovedTrack facet WHERE facet.guestId=? ORDER BY time DESC LIMIT 1"),
		@NamedQuery(name = "lastfm.loved_track.deleteAll", query = "DELETE FROM Facet_LastFmLovedTrack facet WHERE facet.guestId=?"),
		@NamedQuery(name = "lastfm.loved_track.between", query = "SELECT facet FROM Facet_LastFmLovedTrack facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class LastFmLovedTrackFacet extends AbstractFacet {

    String artist;
    String artist_mbid;
    String name;
    String album_mbid;
    String url;
    @Lob
    String imgUrls;
    long time;
    
	public LastFmLovedTrackFacet() {}
	
	@Override
	protected void makeFullTextIndexable() {
		this.fullTextDescription = artist + " " + name;
	}
}