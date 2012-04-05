package com.fluxtream.connectors.picasa;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;

public class PicasaPhotoFacetVO extends
		AbstractInstantFacetVO<PicasaPhotoFacet> {

	public String thumbnailUrl;
	public String photoUrl;
	
	private String thumbnailsJson;
	private JSONArray thumbnails;

	@Override
	public void fromFacet(PicasaPhotoFacet facet, TimeInterval timeInterval,
			GuestSettings settings) {
		startMinute = toMinuteOfDay(new Date(facet.start),
				timeInterval.timeZone);
		thumbnailUrl = facet.thumbnailUrl;
		photoUrl = facet.photoUrl;
		description = facet.title;
		thumbnailsJson = facet.thumbnailsJson;
	}
	
	public int getNumberOfThumbnailSizes() {
		if (thumbnails==null)
			thumbnails = JSONArray.fromObject(thumbnailsJson);
		return thumbnails.size();
	}
	
	@SuppressWarnings("rawtypes")
	public String getThumbnail(int index) {
		if (thumbnails==null)
			thumbnails = JSONArray.fromObject(thumbnailsJson);
		if (index>thumbnails.size())
			return null;
		int[] widths = getThumbnailsWidths();
		Arrays.sort(widths);
		for (Iterator eachThumbnail = thumbnails.iterator(); eachThumbnail.hasNext();) {
			JSONObject thumbnail = (JSONObject) eachThumbnail.next();
			if (thumbnail.getInt("width")==widths[index])
				return thumbnail.getString("url");
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	private int[] getThumbnailsWidths() {
		Iterator eachThumbnail = thumbnails.iterator();
		int[] widths = new int[thumbnails.size()];
		for (int i=0; eachThumbnail.hasNext(); i++) {
			JSONObject thumbnail = (JSONObject) eachThumbnail.next();
			widths[i] = thumbnail.getInt("width");
		}
		return widths;
	}

}
