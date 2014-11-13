package org.fluxtream.connectors.twitter;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;
import org.hibernate.annotations.Index;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity(name="Facet_TwitterDirectMessage")
@ObjectTypeSpec(name = "dm", value = 2, extractor=TwitterFacetExtractor.class, parallel=true, prettyname = "Direct Messages")
@NamedQueries({
		@NamedQuery(name = "twitter.received.dm.smallestTwitterId", query = "SELECT facet FROM Facet_TwitterDirectMessage facet WHERE facet.sent=0 AND facet.guestId=? ORDER BY facet.twitterId ASC LIMIT 1"),
		@NamedQuery(name = "twitter.received.dm.biggestTwitterId", query = "SELECT facet FROM Facet_TwitterDirectMessage facet WHERE facet.sent=0 AND facet.guestId=? ORDER BY facet.twitterId DESC LIMIT 1"),
		@NamedQuery(name = "twitter.sent.dm.smallestTwitterId", query = "SELECT facet FROM Facet_TwitterDirectMessage facet WHERE facet.sent=1 AND facet.guestId=? ORDER BY facet.twitterId ASC LIMIT 1"),
		@NamedQuery(name = "twitter.sent.dm.biggestTwitterId", query = "SELECT facet FROM Facet_TwitterDirectMessage facet WHERE facet.sent=1 AND facet.guestId=? ORDER BY facet.twitterId DESC LIMIT 1"),
		@NamedQuery(name = "twitter.dm.smallestTwitterId", query = "SELECT facet FROM Facet_TwitterDirectMessage facet WHERE facet.guestId=? ORDER BY facet.twitterId ASC LIMIT 1"),
		@NamedQuery(name = "twitter.dm.biggestTwitterId", query = "SELECT facet FROM Facet_TwitterDirectMessage facet WHERE facet.guestId=? ORDER BY facet.twitterId DESC LIMIT 1")
})
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