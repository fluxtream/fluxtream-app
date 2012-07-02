package com.fluxtream.connectors.twitter;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@Entity(name="Facet_Tweet")
@ObjectTypeSpec(name = "tweet", value = 1, extractor = TwitterFacetExtractor.class, parallel = true, prettyname = "Tweets")
//@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@NamedQueries({
		@NamedQuery(name = "twitter.tweet.deleteAll", query = "DELETE FROM Facet_Tweet facet WHERE facet.guestId=?"),
		@NamedQuery(name = "twitter.tweet.between", query = "SELECT facet FROM Facet_Tweet facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?"),
		@NamedQuery(name = "twitter.tweet.oldest", query = "SELECT facet FROM Facet_Tweet facet WHERE facet.guestId=? ORDER BY facet.start ASC"),
		@NamedQuery(name = "twitter.tweet.newest", query = "SELECT facet FROM Facet_Tweet facet WHERE facet.guestId=? ORDER BY facet.start DESC LIMIT 1")
})
@Indexed
public class TweetFacet extends AbstractFacet {

    public long tweetId;

	public String text;
    public String profileImageUrl;
    public String userName;
	long time;
	
	@Override
	protected void makeFullTextIndexable() {
		this.fullTextDescription = text;
	}
	
}