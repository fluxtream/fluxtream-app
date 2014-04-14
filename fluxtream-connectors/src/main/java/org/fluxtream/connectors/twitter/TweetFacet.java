package org.fluxtream.connectors.twitter;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.search.annotations.Indexed;

import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;

@Entity(name="Facet_Tweet")
@ObjectTypeSpec(name = "tweet", value = 1, extractor = TwitterFacetExtractor.class, parallel = true, prettyname = "Tweets")
//@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@NamedQueries({
		@NamedQuery(name = "twitter.tweet.smallestTwitterId", query = "SELECT facet FROM Facet_Tweet facet WHERE facet.guestId=? ORDER BY facet.tweetId ASC LIMIT 1"),
		@NamedQuery(name = "twitter.tweet.biggestTwitterId", query = "SELECT facet FROM Facet_Tweet facet WHERE facet.guestId=? ORDER BY facet.tweetId DESC LIMIT 1")
})
@Indexed
public class TweetFacet extends AbstractFacet {

    public long tweetId;

	public String text;
    public String profileImageUrl;
    public String userName;
	long time;

    public TweetFacet() {
        super();
    }

    public TweetFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    @Override
	protected void makeFullTextIndexable() {
		this.fullTextDescription = text;
	}
	
}