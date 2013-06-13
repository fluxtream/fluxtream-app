package com.fluxtream.connectors.twitter;

import java.util.Date;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.utils.SecurityUtils;

public class TwitterDirectMessageFacetVO extends AbstractInstantFacetVO<TwitterDirectMessageFacet> {

	public String profileImageUrl;
	public String userName;
    public boolean sent;
	
	@Override
	public void fromFacet(TwitterDirectMessageFacet facet, TimeInterval timeInterval,
			GuestSettings settings) throws OutsideTimeBoundariesException {
		Date date = new Date(facet.start);
		
		this.startMinute = toMinuteOfDay(date, timeInterval.getTimeZone(facet.start));
		if (SecurityUtils.isDemoUser())
			this.description = "***demo - text content hidden***";
		else
			this.description = facet.text;
		if (facet.sent==1) {
			this.profileImageUrl = facet.recipientProfileImageUrl;
			this.userName = facet.recipientName;
		} else {
			this.profileImageUrl = facet.senderProfileImageUrl;
			this.userName = facet.senderName;
		}
        this.sent = facet.sent == 1;
	}
	
	protected String getSubtype(TwitterDirectMessageFacet facet) {
		return null;
	}

}
