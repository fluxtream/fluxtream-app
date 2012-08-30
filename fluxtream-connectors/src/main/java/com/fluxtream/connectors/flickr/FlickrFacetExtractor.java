package com.fluxtream.connectors.flickr;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.fluxtream.domain.Tag;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.velocity.util.StringUtils;
import org.joda.time.DateTime;
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

        String feedString = apiData.jsonObject.toString();
		JSONObject photosWrapper = apiData.jsonObject.getJSONObject("photos");

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
                final String datetaken = it.getString("datetaken");
                final DateTime dateTime = format.parseDateTime(datetaken);
                facet.startTimeStorage = facet.endTimeStorage = toTimeStorage(dateTime.getYear(), dateTime.getMonthOfYear(),
                              dateTime.getDayOfMonth(), dateTime.getHourOfDay(),
                              dateTime.getMinuteOfHour(), 0);
                facet.datetaken = dateTime.getMillis();
                facet.start = dateTime.getMillis();
                facet.end = dateTime.getMillis();
				facet.dateupload = it.getLong("dateupload")*1000;
				facet.latitude = it.getString("latitude");
				facet.longitude = it.getString("longitude");
				facet.accuracy = it.getInt("accuracy");
                final String[] tagses = StringUtils.split(it.getString("tags"), " ");
                StringBuilder sb = new StringBuilder();
                for (int i=0; i<tagses.length; i++) {
                    if (tagses[i].indexOf(":")!=-1)
                        continue;
                    Tag tag = new Tag();
                    tag.name = tagses[i];
                    if (facet.tagsList==null)
                        facet.tagsList = new ArrayList<Tag>();
                    else if (!facet.tagsList.contains(tag)) {
                        if (facet.tagsList.size()>0)
                            sb.append(", ");
                    }
                    facet.tagsList.add(tag);
                    sb.append(tag.name);
                }
                facet.tags = sb.toString();
                facets.add(facet);
			}

		}

		return facets;
	}

}
