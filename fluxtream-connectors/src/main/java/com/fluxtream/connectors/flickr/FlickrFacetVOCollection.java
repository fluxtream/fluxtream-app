package com.fluxtream.connectors.flickr;

import java.util.ArrayList;
import java.util.List;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractFacetVOCollection;
import com.fluxtream.connectors.vos.ImageVOCollection;
import com.fluxtream.domain.GuestSettings;

public class FlickrFacetVOCollection extends
		AbstractFacetVOCollection<FlickrPhotoFacet> implements
		ImageVOCollection {

	List<FlickrPhotoFacetVO> photos;

	@Override
	public void extractFacets(List<FlickrPhotoFacet> facets,
			TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
		if (facets.size() == 0)
			return;
		photos = new ArrayList<FlickrPhotoFacetVO>();
		for (FlickrPhotoFacet facet : facets) {
			FlickrPhotoFacetVO jsonFacet = new FlickrPhotoFacetVO();
			jsonFacet.extractValues(facet, timeInterval, settings);
			photos.add(jsonFacet);
		}
	}

}
