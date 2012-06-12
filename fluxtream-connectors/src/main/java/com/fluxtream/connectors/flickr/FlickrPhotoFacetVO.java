package com.fluxtream.connectors.flickr;

import java.awt.Dimension;
import java.util.Date;
import java.util.List;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractPhotoFacetVO;
import com.fluxtream.domain.GuestSettings;

public class FlickrPhotoFacetVO extends
		AbstractPhotoFacetVO<FlickrPhotoFacet> {

	public String photoUrl;

	@Override
	public void fromFacet(FlickrPhotoFacet facet, TimeInterval timeInterval,
			GuestSettings settings) {
        start = facet.datetaken;
		startMinute = toMinuteOfDay(new Date(facet.datetaken),
				timeInterval.timeZone);
		this.photoUrl = "http://farm" + facet.farm + ".static.flickr.com/"
				+ facet.server + "/" + facet.flickrId + "_" + facet.secret
				+ "_s.jpg";
		description = facet.title;
	}

	@Override
	public String getThumbnail(int index) {
		return photoUrl;
	}

	@Override
	public List<Dimension> getThumbnailSizes() {
		// TODO...
		return null;
	}
}
