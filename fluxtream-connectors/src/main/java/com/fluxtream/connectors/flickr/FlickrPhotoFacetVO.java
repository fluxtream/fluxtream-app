package com.fluxtream.connectors.flickr;

import java.util.Date;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;

public class FlickrPhotoFacetVO extends
		AbstractInstantFacetVO<FlickrPhotoFacet> {

	public String photoUrl;

	@Override
	public void fromFacet(FlickrPhotoFacet facet, TimeInterval timeInterval,
			GuestSettings settings) {
		startMinute = toMinuteOfDay(new Date(facet.datetaken),
				timeInterval.timeZone);
		this.photoUrl = "http://farm" + facet.farm + ".static.flickr.com/"
				+ facet.server + "/" + facet.flickrId + "_" + facet.secret
				+ "_s.jpg";
		description = facet.title;
	}

}
