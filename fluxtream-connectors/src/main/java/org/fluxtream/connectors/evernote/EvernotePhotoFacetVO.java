package org.fluxtream.connectors.evernote;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractPhotoFacetVO;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.DimensionModel;

/**
 * User: candide
 * Date: 03/01/14
 * Time: 17:06
 */
public class EvernotePhotoFacetVO extends AbstractPhotoFacetVO<EvernotePhotoFacet> {

    public Map<Integer, String> thumbnailUrls = new HashMap<Integer, String>();
    public SortedMap<Integer, Dimension> thumbnailSizes = new TreeMap<Integer, Dimension>();
    public String thumbnailUrl;
    public String photoUrl;
    public float[] position;

    @Override
    protected void fromFacet(final EvernotePhotoFacet facet, final TimeInterval timeInterval, final GuestSettings settings) throws OutsideTimeBoundariesException {
        deviceName = "Evernote";
        channelName = "photo";
        UID = facet.getId();
        start = facet.start;
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(start);

        int i = 0;

        String homeBaseUrl = settings.config.get("homeBaseUrl");
        final String thumbnailUrl = String.format("%severnote/res/%s/%s@w=150", homeBaseUrl, facet.apiKeyId, facet.guid);
        this.thumbnailUrl = thumbnailUrl;
        this.photoUrl = String.format("%severnote/res/%s/%s", homeBaseUrl, facet.apiKeyId, facet.guid);
        thumbnailUrls.put(i, thumbnailUrl);
        thumbnailSizes.put(i, new Dimension(150, 150));
        i++;

        // hereafter, flickr documentation specifies a number of pixels *on longest side* - since we don't have the
        // original image's dimension, we just specify a square of that number
        for (Integer width : new Integer[]{75, 100, 240, 320, 500, 640, 800, 1024}) {
            thumbnailUrls.put(i, String.format("%severnote/res/%s/%s@w=%s", homeBaseUrl, facet.apiKeyId, facet.guid, width));
            thumbnailSizes.put(i, new Dimension(width, width));
            i++;
        }

        if (facet.resourceFacet.longitude!=null && facet.resourceFacet.latitude!=null){
            position = new float[2];
            position[0] = facet.resourceFacet.latitude.floatValue();
            position[1] = facet.resourceFacet.longitude.floatValue();
        }
    }

    @Override
    public String getPhotoUrl() {
        return photoUrl;
    }

    @Override
    public String getThumbnail(final int index) {
        return thumbnailUrls.get(index);
    }

    @Override
    public List<DimensionModel> getThumbnailSizes() {
        List<DimensionModel> dimensions = new ArrayList<DimensionModel>();
        for (Dimension dimension : thumbnailSizes.values()) {
            dimensions.add(new DimensionModel(dimension.width, dimension.height));
        }
        return dimensions;
    }
}
