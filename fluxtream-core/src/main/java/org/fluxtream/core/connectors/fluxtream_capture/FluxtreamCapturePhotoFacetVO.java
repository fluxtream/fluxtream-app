package org.fluxtream.core.connectors.fluxtream_capture;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.fluxtream_capture.FluxtreamCapturePhotoFacet;
import org.fluxtream.core.connectors.vos.AbstractPhotoFacetVO;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.images.ImageOrientation;
import org.fluxtream.core.mvc.models.DimensionModel;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class FluxtreamCapturePhotoFacetVO extends AbstractPhotoFacetVO<FluxtreamCapturePhotoFacet> {

    public Map<Integer, String> thumbnailUrls = new HashMap<Integer, String>(FluxtreamCapturePhotoFacet.NUM_THUMBNAILS);
    public SortedMap<Integer, Dimension> thumbnailSizes = new TreeMap<Integer, Dimension>();

    public transient ImageOrientation imageOrientation;

    public int orientation;

    @Override
    protected void fromFacet(final FluxtreamCapturePhotoFacet facet, final TimeInterval timeInterval, final GuestSettings settings)
            throws OutsideTimeBoundariesException {
        deviceName = "FluxtreamCapture";
        channelName = "photo";
        UID = facet.getId();
        start = facet.start;

        final String photoStoreKey = facet.getPhotoStoreKey();
        photoUrl = "/api/v1/bodytrack/photo/" + photoStoreKey;

        // build the Map of thumbnail URLS and the SortedMap of thumbnail sizes
        for (int i = 0; i < FluxtreamCapturePhotoFacet.NUM_THUMBNAILS; i++) {
            thumbnailUrls.put(i, "/api/v1/bodytrack/photoThumbnail/" + facet.getGuestId() + "/" + facet.getId() + "/" + i);
            thumbnailSizes.put(i, facet.getThumbnailSize(i));
        }
        imageOrientation = facet.getOrientation();
        orientation = imageOrientation.getId();
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
        List<DimensionModel> sizes = new ArrayList<DimensionModel>();
        for (final Dimension dimension : thumbnailSizes.values()) {
            sizes.add(new DimensionModel(dimension.width, dimension.height)); // create a copy so the caller can't modify this instance
        }
        return sizes;
    }

    @Override
    public ImageOrientation getOrientation() {
        return imageOrientation;
    }
}
