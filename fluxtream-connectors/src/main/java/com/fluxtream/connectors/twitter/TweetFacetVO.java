package com.fluxtream.connectors.twitter;

import java.util.Date;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;

public class TweetFacetVO extends AbstractInstantFacetVO<TweetFacet> {

	String text;
    public String profileImageUrl;
	
	@Override
	public void fromFacet(TweetFacet facet, TimeInterval timeInterval, GuestSettings settings) {
		startMinute = toMinuteOfDay(new Date(facet.time), timeInterval.timeZone);
		text = facet.text;
		description = facet.text;
        this.profileImageUrl = facet.profileImageUrl;
	}

}
