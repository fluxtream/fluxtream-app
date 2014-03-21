package org.fluxtream.connectors.flickr;

import java.awt.Dimension;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.vos.AbstractPhotoFacetVO;
import org.fluxtream.domain.GuestSettings;
import org.fluxtream.mvc.models.DimensionModel;

public class FlickrPhotoFacetVO extends
		AbstractPhotoFacetVO<FlickrPhotoFacet> {

	public String photoUrl;
    public String thumbnailUrl;
    public Map<Integer, String> thumbnailUrls = new HashMap<Integer, String>();
    public SortedMap<Integer, Dimension> thumbnailSizes = new TreeMap<Integer, Dimension>();

    public float[] position;

    public FlickrPhotoFacetVO()
    {
        timeType="local";
    }

	@Override
	public void fromFacet(FlickrPhotoFacet facet, TimeInterval timeInterval,
			GuestSettings settings) {
        deviceName = "Flickr";
        channelName = "photo";
        UID = facet.getId();
        this.date = facet.date;
        start = facet.datetaken;
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(start);
		startMinute = c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE);

        int i = 0;
        thumbnailUrls.put(i, String.format("https://farm%s.static.flickr.com/%s/%s_%s_s.jpg", facet.farm, facet.server, facet.flickrId, facet.secret));
        thumbnailSizes.put(i, new Dimension(75, 75));
        i++;
        final String thumbnailUrl = String.format("https://farm%s.static.flickr.com/%s/%s_%s_q.jpg", facet.farm, facet.server, facet.flickrId, facet.secret);
        this.thumbnailUrl = thumbnailUrl;
        thumbnailUrls.put(i, thumbnailUrl);
        thumbnailSizes.put(i, new Dimension(150, 150));
        i++;

        // hereafter, flickr documentation specifies a number of pixels *on longest side* - since we don't have the
        // original image's dimension, we just specify a square of that number
        thumbnailUrls.put(i, String.format("https://farm%s.static.flickr.com/%s/%s_%s_t.jpg", facet.farm, facet.server, facet.flickrId, facet.secret));
        thumbnailSizes.put(i, new Dimension(100, 100));
        i++;
        thumbnailUrls.put(i, String.format("https://farm%s.static.flickr.com/%s/%s_%s_m.jpg", facet.farm, facet.server, facet.flickrId, facet.secret));
        thumbnailSizes.put(i, new Dimension(240, 240));
        i++;
        thumbnailUrls.put(i, String.format("https://farm%s.static.flickr.com/%s/%s_%s_n.jpg", facet.farm, facet.server, facet.flickrId, facet.secret));
        thumbnailSizes.put(i, new Dimension(320, 320));
        i++;
        thumbnailUrls.put(i, String.format("https://farm%s.static.flickr.com/%s/%s_%s_-.jpg", facet.farm, facet.server, facet.flickrId, facet.secret));
        thumbnailSizes.put(i, new Dimension(500, 500));
        i++;
        thumbnailUrls.put(i, String.format("https://farm%s.static.flickr.com/%s/%s_%s_z.jpg", facet.farm, facet.server, facet.flickrId, facet.secret));
        thumbnailSizes.put(i, new Dimension(640, 640));
        i++;
        thumbnailUrls.put(i, String.format("https://farm%s.static.flickr.com/%s/%s_%s_c.jpg", facet.farm, facet.server, facet.flickrId, facet.secret));
        thumbnailSizes.put(i, new Dimension(800, 800));
        i++;
        thumbnailUrls.put(i, String.format("https://farm%s.static.flickr.com/%s/%s_%s_b.jpg", facet.farm, facet.server, facet.flickrId, facet.secret));
        thumbnailSizes.put(i, new Dimension(1024, 1024));

        this.photoUrl = "https://farm" + facet.farm + ".static.flickr.com/"
                        + facet.server + "/" + facet.flickrId + "_" + facet.secret
                        + "_z.jpg";
		description = facet.title;

        if (facet.longitude!=null && facet.latitude!=null){
            position = new float[2];
            position[0] = facet.latitude;
            position[1] = facet.longitude;
        }
	}

    @Override
    public String getPhotoUrl() {
        return photoUrl;
    }

    @Override
	public String getThumbnail(int index) {
		return thumbnailUrls.get(index);
	}

	@Override
	public List<DimensionModel> getThumbnailSizes() {
		// TODO...
		return null;
	}
}
