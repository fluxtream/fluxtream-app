package glacier.instagram;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.fluxtream.core.connectors.updaters.UpdateInfo;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.stereotype.Component;

import org.fluxtream.core.ApiData;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.facets.extractors.AbstractFacetExtractor;

@Component
class InstagramFacetExtractor extends AbstractFacetExtractor {
	
	public List<AbstractFacet> extractFacets(final UpdateInfo updateInfo, final ApiData apiData,
                                             final ObjectType objectType) {
		List<AbstractFacet> facets = new ArrayList<AbstractFacet>();

		JSONObject feed = JSONObject.fromObject(apiData.json);

		if (feed.has("data")) {
			
			JSONArray data = feed.getJSONArray("data");
			
			@SuppressWarnings("rawtypes")
			Iterator iterator = data.iterator();
			while(iterator.hasNext()) {
				JSONObject it = (JSONObject) iterator.next();
				if (it.getString("type").equals("image")) {
					InstagramPhotoFacet facet = new InstagramPhotoFacet(apiData.updateInfo.apiKey.getId());
					super.extractCommonFacetData(facet, apiData);
					
					JSONObject images = it.getJSONObject("images");
					
					if (images.has("low_resolution")) {
						JSONObject low_resolution = images.getJSONObject("low_resolution");
						facet.lowResolutionUrl = low_resolution.getString("url");
						facet.lowResolutionWidth = low_resolution.getInt("width");
						facet.lowResolutionHeight = low_resolution.getInt("height");
					}
					if (images.has("thumbnail")) {
						JSONObject thumbnail = images.getJSONObject("thumbnail");
						facet.thumbnailUrl = thumbnail.getString("url");
						facet.thumbnailWidth = thumbnail.getInt("width");;
						facet.thumbnailHeight = thumbnail.getInt("height");
					}
					if (images.has("standard_resolution")) {
						JSONObject standard_resolution = images.getJSONObject("standard_resolution");
						facet.standardResolutionUrl = standard_resolution.getString("url");
						facet.standardResolutionWidth = standard_resolution.getInt("width");;
						facet.standardResolutionHeight = standard_resolution.getInt("height");
					}

					facet.instagramId = it.getString("id");
					
					if (it.has("location")) {
						JSONObject location = images.getJSONObject("location");
						facet.locationName = location.getString("name");
						facet.latitude = location.getDouble("latitude");
						facet.longitude = location.getDouble("longitude");
					}
					
					facet.link = it.getString("link");
					facet.start = it.getLong("created_time")*1000;
					facet.end = it.getLong("created_time")*1000;
					
					facet.filter = it.getString("filter");
					facet.caption = it.getString("caption");
					
					facets.add(facet);
	
				}
			}
						
		}
		
		return facets;
	}
	
}
