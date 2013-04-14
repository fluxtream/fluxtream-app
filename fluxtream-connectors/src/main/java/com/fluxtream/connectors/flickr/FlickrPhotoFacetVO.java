package com.fluxtream.connectors.flickr;

import java.awt.Dimension;
import java.util.List;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractPhotoFacetVO;
import com.fluxtream.domain.GuestSettings;
import org.joda.time.LocalDateTime;

public class FlickrPhotoFacetVO extends
		AbstractPhotoFacetVO<FlickrPhotoFacet> {

	public String photoUrl;
    public String thumbnailUrl;

	@Override
	public void fromFacet(FlickrPhotoFacet facet, TimeInterval timeInterval,
			GuestSettings settings) {
        start = facet.datetaken;
        LocalDateTime t = new LocalDateTime(facet.start);
		startMinute = t.getHourOfDay()*60+t.getMinuteOfHour();
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
