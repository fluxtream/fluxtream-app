package glacier.picasa;

import java.util.ArrayList;
import java.util.List;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractFacetVOCollection;
import com.fluxtream.connectors.vos.ImageVOCollection;
import com.fluxtream.domain.GuestSettings;

public class PicasaFacetVOCollection extends
		AbstractFacetVOCollection<PicasaPhotoFacet> implements
		ImageVOCollection {

	List<PicasaPhotoFacetVO> photos;

	@Override
	public void extractFacets(List<PicasaPhotoFacet> facets,
			TimeInterval timeInterval, GuestSettings settings) {
		if (facets.size() == 0)
			return;
		photos = new ArrayList<PicasaPhotoFacetVO>();
		for (PicasaPhotoFacet facet : facets) {
			PicasaPhotoFacetVO jsonFacet = new PicasaPhotoFacetVO();
			jsonFacet.extractValues(facet, timeInterval, settings);
			photos.add(jsonFacet);
		}
	}

}
