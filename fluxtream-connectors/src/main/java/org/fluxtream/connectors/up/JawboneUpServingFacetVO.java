package org.fluxtream.connectors.up;

import java.awt.Dimension;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import org.fluxtream.OutsideTimeBoundariesException;
import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.vos.AbstractPhotoFacetVO;
import org.fluxtream.domain.GuestSettings;
import org.fluxtream.mvc.models.DimensionModel;

/**
 * User: candide
 * Date: 11/02/14
 * Time: 15:54
 */
public class JawboneUpServingFacetVO extends AbstractPhotoFacetVO<JawboneUpServingFacet> {

    public Map<Integer, String> thumbnailUrls = new HashMap<Integer, String>();
    public SortedMap<Integer, Dimension> thumbnailSizes = new TreeMap<Integer, Dimension>();
    public String thumbnailUrl;
    public String photoUrl;
    public float[] position;

    @Override
    protected void fromFacet(final JawboneUpServingFacet facet, final TimeInterval timeInterval, final GuestSettings settings) throws OutsideTimeBoundariesException {
        deviceName = "Jawbone_UP";
        channelName = "serving";
        UID = facet.getId();
        start = facet.start;
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(start);
        startMinute = c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE);

        int i = 0;

        this.thumbnailUrl = JawboneUpVOHelper.getImageURL(facet.image, facet, settings.config, 150);
        this.photoUrl = JawboneUpVOHelper.getImageURL(facet.image, facet, settings.config);
        thumbnailUrls.put(i, thumbnailUrl);
        thumbnailSizes.put(i, new Dimension(150, 150));
        i++;

        for (Integer width : new Integer[]{75, 100, 240, 320, 500, 640, 800, 1024}) {
            thumbnailUrls.put(i, JawboneUpVOHelper.getImageURL(facet.image, facet, settings.config, width));
            thumbnailSizes.put(i, new Dimension(width, width));
            i++;
        }

        if (facet.meal.place_lon!=null && facet.meal.place_lat!=null){
            position = new float[2];
            position[0] = facet.meal.place_lat.floatValue();
            position[1] = facet.meal.place_lon.floatValue();
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
        //List<DimensionModel> dimensions = new ArrayList<DimensionModel>();
        //for (Dimension dimension : thumbnailSizes.values()) {
        //    dimensions.add(new DimensionModel(dimension.width, dimension.height));
        //}
        //return dimensions;

        return null;
    }
}
