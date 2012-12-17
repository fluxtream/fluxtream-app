package com.fluxtream.connectors.fluxtream_capture;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractPhotoFacetVO;
import com.fluxtream.domain.GuestSettings;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class FluxtreamCapturePhotoFacetVO extends AbstractPhotoFacetVO<FluxtreamCapturePhotoFacet> {
    private static final int NUM_THUMBNAILS = 2;

    public String photoUrl;
    public Map<Integer, String> thumbnailUrls = new HashMap<Integer, String>(NUM_THUMBNAILS);
    public SortedMap<Integer, Dimension> thumbnailSizes = new TreeMap<Integer, Dimension>();

    @Override
    protected void fromFacet(final FluxtreamCapturePhotoFacet facet, final TimeInterval timeInterval, final GuestSettings settings) {
        start = facet.start;
        startMinute = toMinuteOfDay(new Date(facet.start), timeInterval.timeZone);

        // TODO: build the Map of thumbnail URLs
        thumbnailUrls.put(0, "");
        thumbnailUrls.put(1, "");

        // build the SortedMap of thumbnail sizes
        thumbnailSizes.put(0, facet.getThumbnailSmallSize());
        thumbnailSizes.put(1, facet.getThumbnailLargeSize());
    }

    @Override
    public String getPhotoUrl() {
        // TODO: implement me once we have an API method for fetching Fluxtream Capture photos
        return null;
    }

    @Override
    public String getThumbnail(final int index) {
        // TODO: implement me once we have an API method for fetching Fluxtream Capture thumbnails
        return thumbnailUrls.get(index);
    }

    @Override
    public List<Dimension> getThumbnailSizes() {
        List<Dimension> sizes = new ArrayList<Dimension>();
        for (final Dimension dimension : thumbnailSizes.values()) {
            sizes.add(new Dimension(dimension)); // create a copy so the caller can't modify this instance
        }
        return sizes;
    }
}
