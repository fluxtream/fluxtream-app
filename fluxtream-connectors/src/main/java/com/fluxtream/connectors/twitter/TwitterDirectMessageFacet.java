package com.fluxtream.connectors.twitter;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Index;
import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@Entity(name="Facet_TwitterDirectMessage")
@ObjectTypeSpec(name = "dm", value = 2, extractor=TwitterFacetExtractor.class, parallel=false, prettyname = "Direct Messages")
@NamedQueries({
		@NamedQuery(name = "twitter.received.dm.oldest", query = "SELECT facet FROM Facet_TwitterDirectMessage facet WHERE facet.sent=0 AND facet.guestId=? ORDER BY facet.start ASC"),
		@NamedQuery(name = "twitter.received.dm.newest", query = "SELECT facet FROM Facet_TwitterDirectMessage facet WHERE facet.sent=0 AND facet.guestId=? ORDER BY facet.start DESC"),
		@NamedQuery(name = "twitter.sent.dm.oldest", query = "SELECT facet FROM Facet_TwitterDirectMessage facet WHERE facet.sent=1 AND facet.guestId=? ORDER BY facet.start ASC"),
		@NamedQuery(name = "twitter.sent.dm.newest", query = "SELECT facet FROM Facet_TwitterDirectMessage facet WHERE facet.sent=1 AND facet.guestId=? ORDER BY facet.start DESC"),
		@NamedQuery(name = "twitter.dm.oldest", query = "SELECT facet FROM Facet_TwitterDirectMessage facet WHERE facet.guestId=? ORDER BY facet.start ASC"),
		@NamedQuery(name = "twitter.dm.newest", query = "SELECT facet FROM Facet_TwitterDirectMessage facet WHERE facet.guestId=? ORDER BY facet.start DESC LIMIT 1")
})
@Indexed
public class TwitterDirectMessageFacet extends AbstractFacet {

	public long twitterId;
	
	public String text;
	long time;
	
	@Index(name="sent_index")
	byte sent;
	
	public String recipientName;
	public String senderName;
	
	public String recipientScreenName;
	public String senderScreenName;

	public String senderProfileImageUrl;
	public String recipientProfileImageUrl;

    public TwitterDirectMessageFacet() {
        super();
    }

    public TwitterDirectMessageFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    @Override
	protected void makeFullTextIndexable() {
		this.fullTextDescription = senderName + " " + senderScreenName + " " + text;
	}
	
}