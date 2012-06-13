package com.fluxtream.connectors.twitter;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@Entity(name="Facet_Tweet")
@ObjectTypeSpec(name = "tweet", value = 1, extractor = TwitterFacetExtractor.class, parallel = true, prettyname = "Tweets",
                detailsTemplate="<h3 class=\"flx-dataType\">Tweet</h3>" +
                                "<span class=\"flx-deviceIcon\" style=\"background:url('{{profileImageUrl}}'); background-size:100% 100%;\"></span>" +
                                "<div class=\"flx-deviceData\">" +
                                "    <span class=\"flx-tTime\">{{time}}</span>" +
                                "    <span class=\"flx-data\">{{description}}</span>" +
                                "</div>")
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
	long time;
	
	@Override
	protected void makeFullTextIndexable() {
		this.fullTextDescription = text;
	}
	
}