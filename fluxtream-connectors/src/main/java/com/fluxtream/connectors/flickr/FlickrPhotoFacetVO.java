package com.fluxtream.connectors.flickr;

import java.awt.Dimension;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractPhotoFacetVO;
import com.fluxtream.domain.GuestSettings;

public class FlickrPhotoFacetVO extends
		AbstractPhotoFacetVO<FlickrPhotoFacet> {

	public String photoUrl;
    public String thumbnailUrl;

    public FlickrPhotoFacetVO()
    {
        timeType="local";
    }

	@Override
	public void fromFacet(FlickrPhotoFacet facet, TimeInterval timeInterval,
			GuestSettings settings) {
        start = facet.datetaken;
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(start);
		startMinute = c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE);
		this.thumbnailUrl = "http://farm" + facet.farm + ".static.flickr.com/"
				+ facet.server + "/" + facet.flickrId + "_" + facet.secret
				+ "_s.jpg";
        this.photoUrl = "http://farm" + facet.farm + ".static.flickr.com/"
                        + facet.server + "/" + facet.flickrId + "_" + facet.secret
                        + "_z.jpg";
		description = facet.title;
	}

    @Override
    public String getPhotoUrl() {
        return photoUrl;
    }

    @Override
	public String getThumbnail(int index) {
		return thumbnailUrl;
	}

	@Override
	public List<Dimension> getThumbnailSizes() {
		// TODO...
		return null;
	}
}
