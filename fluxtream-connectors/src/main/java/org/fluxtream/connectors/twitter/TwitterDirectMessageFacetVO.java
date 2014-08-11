package org.fluxtream.connectors.twitter;

import java.util.Date;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractInstantFacetVO;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.utils.SecurityUtils;
import org.apache.commons.lang.StringEscapeUtils;

public class TwitterDirectMessageFacetVO extends AbstractInstantFacetVO<TwitterDirectMessageFacet> {

	public String profileImageUrl;
	public String userName;
    public boolean sent;
	
	@Override
	public void fromFacet(TwitterDirectMessageFacet facet, TimeInterval timeInterval,
			GuestSettings settings) throws OutsideTimeBoundariesException {
		Date date = new Date(facet.start);
		
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
