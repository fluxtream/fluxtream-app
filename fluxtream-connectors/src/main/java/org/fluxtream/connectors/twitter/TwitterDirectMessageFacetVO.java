package org.fluxtream.connectors.twitter;

import java.util.Date;

import org.fluxtream.OutsideTimeBoundariesException;
import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.vos.AbstractInstantFacetVO;
import org.fluxtream.domain.GuestSettings;
import org.fluxtream.utils.SecurityUtils;
import org.apache.commons.lang.StringEscapeUtils;

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
			this.description = StringEscapeUtils.escapeHtml(facet.text);
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
