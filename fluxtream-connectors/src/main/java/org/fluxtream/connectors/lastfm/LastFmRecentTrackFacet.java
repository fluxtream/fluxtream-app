package org.fluxtream.connectors.lastfm;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity(name="Facet_LastFmRecentTrack")
@ObjectTypeSpec(name = "recent_track", value = 2, extractor=LastFmFacetExtractor.class, parallel=true, prettyname = "Recent Tracks")
@NamedQueries({
		@NamedQuery(name = "lastfm.recent_track.byStartEnd", query = "SELECT facet FROM Facet_LastFmRecentTrack facet WHERE facet.apiKeyId=? AND facet.start=? AND facet.end=?"),
		@NamedQuery(name = "lastfm.recent_track.newest", query = "SELECT facet FROM Facet_LastFmRecentTrack facet WHERE facet.apiKeyId=? ORDER BY time DESC LIMIT 1")
})
public class LastFmRecentTrackFacet extends AbstractFacet {

    String artist;
    String artist_mbid;
    String name;
    String album_mbid;
    String url;
    @Lob
    String imgUrls;
    long time;
    String mbid;

    public LastFmRecentTrackFacet() {
        super();
    }

    public LastFmRecentTrackFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    @Override
	protected void makeFullTextIndexable() {
		this.fullTextDescription = artist + " " + name;
	}

}
