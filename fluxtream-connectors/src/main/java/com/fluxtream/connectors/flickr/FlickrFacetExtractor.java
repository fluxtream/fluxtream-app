package com.fluxtream.connectors.flickr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.Tag;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class FlickrFacetExtractor extends AbstractFacetExtractor {

	private static final DateTimeFormatter format = DateTimeFormat
			.forPattern("yyyy-MM-dd HH:mm:ss").withZone(DateTimeZone.UTC);

	public List<AbstractFacet> extractFacets(final UpdateInfo updateInfo, final ApiData apiData,
			ObjectType objectType) {
		List<AbstractFacet> facets = new ArrayList<AbstractFacet>();

		JSONObject photosWrapper = apiData.jsonObject.getJSONObject("photos");

		if (photosWrapper != null) {

			JSONArray photos = photosWrapper.getJSONArray("photo");
			if (photos == null)
				return facets;

			@SuppressWarnings("rawtypes")
			Iterator eachPhoto = photos.iterator();
			while (eachPhoto.hasNext()) {
				JSONObject it = (JSONObject) eachPhoto.next();
				FlickrPhotoFacet facet = new FlickrPhotoFacet(apiData.updateInfo.apiKey.getId());
				super.extractCommonFacetData(facet, apiData);
				facet.flickrId = it.getString("id");
				facet.owner = it.getString("owner");
				facet.secret = it.getString("secret");
				facet.server = it.getString("server");
				facet.farm = it.getString("farm");
				facet.title = it.getString("title");
                final JSONObject descriptionObject = it.getJSONObject("description");
                if (descriptionObject != null) {
                    facet.comment = descriptionObject.getString("_content");
                }
				facet.ispublic = Integer.valueOf(it.getString("ispublic")) == 1;
				facet.isfriend = Integer.valueOf(it.getString("isfriend")) == 1;
				facet.isfamily = Integer.valueOf(it.getString("isfamily")) == 1;
                final String datetaken = it.getString("datetaken");
                final DateTime dateTime = format.parseDateTime(datetaken);
                facet.startTimeStorage = facet.endTimeStorage = toTimeStorage(dateTime.getYear(), dateTime.getMonthOfYear(),
                                                                              dateTime.getDayOfMonth(), dateTime.getHourOfDay(),
                                                                              dateTime.getMinuteOfHour(), 0);
                facet.date = (new StringBuilder(String.valueOf(dateTime.getYear())).append("-")
                              .append(pad(dateTime.getMonthOfYear())).append("-")
                              .append(pad(dateTime.getDayOfMonth()))).toString();
                facet.datetaken = dateTime.getMillis();
                facet.start = dateTime.getMillis();
                facet.end = dateTime.getMillis();
				facet.dateupload = it.getLong("dateupload")*1000;
				facet.latitude = it.getString("latitude");
				facet.longitude = it.getString("longitude");
				facet.accuracy = it.getInt("accuracy");
                facet.addTags(it.getString("tags"), Tag.SPACE_DELIMITER);
                facets.add(facet);
			}

		}

		return facets;
	}

    public static void main(final String[] args) {
        String datetaken = "2012-10-26 17:29:19";
        final DateTime dateTime = format.parseDateTime(datetaken);
        String date = (new StringBuilder(String.valueOf(dateTime.getYear())).append("-")
                              .append(pad(dateTime.getMonthOfYear())).append("-")
                              .append(pad(dateTime.getDayOfMonth()))).toString();
        System.out.println(date);
    }

}
