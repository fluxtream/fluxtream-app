package com.fluxtream.connectors.vos;

import java.awt.Dimension;
import java.util.List;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.images.ImageOrientation;

public abstract class AbstractPhotoFacetVO<T extends AbstractFacet> extends
		AbstractInstantFacetVO<T> {

	public String photoUrl;
	
	public abstract String getPhotoUrl();
	public abstract String getThumbnail(int index);
	public abstract List<Dimension> getThumbnailSizes();

    /**
     * Return the {@link ImageOrientation image's orientation}.  Defaults to {@link ImageOrientation#ORIENTATION_1} if
     * not overridden.
     */
    public ImageOrientation getOrientation() {
        return ImageOrientation.ORIENTATION_1;
    }
}
