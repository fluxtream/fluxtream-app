package org.fluxtream.core.connectors.vos;

import org.fluxtream.core.domain.AbstractLocalTimeFacet;
import org.fluxtream.core.images.ImageOrientation;
import org.fluxtream.core.mvc.models.DimensionModel;

import java.util.List;

/**
 * Created by candide on 23/07/15.
 */
public abstract class AbstractLocalTimePhotoFacetVO<T extends AbstractLocalTimeFacet> extends AbstractLocalTimeInstantFacetVO<T> {

    public String photoUrl;
    public String deviceName;
    public String channelName;
    public long UID;

    // timeType is either "gmt" for facets that know their absolute time, or
    // "local" for facets that only know local time.  This defaults to "gmt"
    // and should be changed by subclasses such as Flickr which use local time.
    public String timeType="gmt";

    public abstract String getPhotoUrl();
    public abstract String getThumbnail(int index);
    public abstract List<DimensionModel> getThumbnailSizes();


    /**
     * Return the {@link ImageOrientation image's orientation}.  Defaults to {@link ImageOrientation#ORIENTATION_1} if
     * not overridden.
     */
    public ImageOrientation getOrientation() {
        return ImageOrientation.ORIENTATION_1;
    }
}
