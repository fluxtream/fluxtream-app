package com.fluxtream.connectors.vos;

import java.awt.Dimension;
import java.util.List;

import com.fluxtream.domain.AbstractFacet;

public abstract class AbstractPhotoFacetVO<T extends AbstractFacet> extends
		AbstractInstantFacetVO<T> {

	public String photoUrl;
	
	public abstract String getThumbnail(int index);
	public abstract List<Dimension> getThumbnailSizes();
}
