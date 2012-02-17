package com.fluxtream.connectors.flickr;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;

@Component
public class FlickrFacetExtractor extends AbstractFacetExtractor {

	private static final DateTimeFormatter format = DateTimeFormat
			.forPattern("yyyy-MM-dd HH:mm:ss");

	public List<AbstractFacet> extractFacets(ApiData apiData,
			ObjectType objectType) {
		List<AbstractFacet> facets = new ArrayList<AbstractFacet>();

		JSONObject feed = JSONObject.fromObject(apiData.json);
		JSONObject photosWrapper = feed.getJSONObject("photos");

		if (photosWrapper != null) {

			JSONArray photos = photosWrapper.getJSONArray("photo");
			if (photos == null)
				return facets;

			@SuppressWarnings("rawtypes")
			Iterator eachPhoto = photos.iterator();
			while (eachPhoto.hasNext()) {
				JSONObject it = (JSONObject) eachPhoto.next();
				FlickrPhotoFacet facet = new FlickrPhotoFacet();
				super.extractCommonFacetData(facet, apiData);
				facet.flickrId = it.getString("id");
				facet.owner = it.getString("owner");
				facet.secret = it.getString("secret");
				facet.server = it.getString("server");
				facet.farm = it.getString("farm");
				facet.title = it.getString("title");
				facet.ispublic = Integer.valueOf(it.getString("ispublic")) == 1;
				facet.isfriend = Integer.valueOf(it.getString("isfriend")) == 1;
				facet.isfamily = Integer.valueOf(it.getString("isfamily")) == 1;
				Date datetakenDate = null;
				//TODO: set user's timezone for this date on format
				try {
					datetakenDate = new Date(format.parseMillis(it
							.getString("datetaken")));
				} catch (Exception e) {
				}
				if (datetakenDate != null) {
					facet.datetaken = datetakenDate.getTime();
					facet.start = datetakenDate.getTime();
					facet.end = datetakenDate.getTime();
				}
				facet.dateupload = Long.valueOf(it.getString("dateupload")) * 1000;
				facet.latitude = it.getString("latitude");
				facet.longitude = it.getString("longitude");
				facet.accuracy = it.getInt("accuracy");

				facets.add(facet);
			}

		}

		return facets;
	}

}
