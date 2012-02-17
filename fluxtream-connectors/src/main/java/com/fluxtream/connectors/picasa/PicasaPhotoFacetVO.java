package com.fluxtream.connectors.picasa;

import java.util.Date;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;

public class PicasaPhotoFacetVO extends
		AbstractInstantFacetVO<PicasaPhotoFacet> {

	public String thumbnailUrl;
	public String photoUrl;

	@Override
	public void fromFacet(PicasaPhotoFacet facet, TimeInterval timeInterval,
			GuestSettings settings) {
		startMinute = toMinuteOfDay(new Date(facet.start),
				timeInterval.timeZone);
		thumbnailUrl = facet.thumbnailUrl;
		photoUrl = facet.photoUrl;
		description = facet.title;
	}

}
