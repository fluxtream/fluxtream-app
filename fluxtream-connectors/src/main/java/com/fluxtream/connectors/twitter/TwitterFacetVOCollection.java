package com.fluxtream.connectors.twitter;

import java.util.ArrayList;
import java.util.List;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractFacetVOCollection;
import com.fluxtream.domain.GuestSettings;

@SuppressWarnings("rawtypes")
public class TwitterFacetVOCollection extends AbstractFacetVOCollection {

	List<TwitterDirectMessageFacetVO> directMessages;
	List<TweetFacetVO> tweets;
	
	@Override
	public void extractFacets(List facets, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
		for (Object facet : facets) {
			if (facet instanceof TweetFacet)
				addTweet((TweetFacet)facet, timeInterval, settings);
			else if (facet instanceof TwitterDirectMessageFacet)
				addDM((TwitterDirectMessageFacet)facet, timeInterval, settings);
		}
	}

	private void addTweet(TweetFacet facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
		if (tweets==null) tweets = new ArrayList<TweetFacetVO>();
		TweetFacetVO jsonFacet = new TweetFacetVO();
		jsonFacet.extractValues(facet, timeInterval, settings);
		tweets.add(jsonFacet);
	}

	private void addDM(TwitterDirectMessageFacet facet, TimeInterval timeInterval,
			GuestSettings settings) throws OutsideTimeBoundariesException {
		if (directMessages==null) directMessages = new ArrayList<TwitterDirectMessageFacetVO>();
		TwitterDirectMessageFacetVO jsonFacet = new TwitterDirectMessageFacetVO();
		jsonFacet.extractValues(facet, timeInterval, settings);
		directMessages.add(jsonFacet);
	}

}
