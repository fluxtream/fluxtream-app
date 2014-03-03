package org.fluxtream.connectors.lastfm;

import java.util.ArrayList;
import java.util.List;
import org.fluxtream.ApiData;
import org.fluxtream.connectors.ObjectType;
import org.fluxtream.connectors.updaters.UpdateInfo;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.facets.extractors.AbstractFacetExtractor;
import org.fluxtream.services.JPADaoService;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LastFmFacetExtractor extends AbstractFacetExtractor {

	@Autowired
	JPADaoService jpaDaoService;

	public List<AbstractFacet> extractFacets(UpdateInfo updateInfo, ApiData apiData,
			ObjectType objectType) {
		List<AbstractFacet> facets = new ArrayList<AbstractFacet>();

		try {
			if (objectType == ObjectType.getObjectType(connector(updateInfo),
					"recent_track")) {
				extractLastfmRecentTracks(apiData, facets);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return facets;
	}

	private void extractLastfmRecentTracks(ApiData apiData,
			List<AbstractFacet> facets) throws Throwable {
		JSONObject tracksToExtract = apiData.jsonObject
				.getJSONObject("recenttracks");
		JSONArray tracks = new JSONArray();
		if (tracksToExtract.containsKey("track")&&tracksToExtract.get("track") instanceof JSONArray)
			tracks = tracksToExtract.getJSONArray("track");
		else if (tracksToExtract.containsKey("track")&&tracksToExtract.get("track") instanceof JSONObject) {
			JSONObject aTrack = tracksToExtract.getJSONObject("track");
			tracks.add(aTrack);
		} else
			return;
		
		int ntracks = tracks.size();

		for (int i = 0; i < ntracks; i++) {
			LastFmRecentTrackFacet facet = new LastFmRecentTrackFacet(apiData.updateInfo.apiKey.getId());

			super.extractCommonFacetData(facet, apiData);

			JSONObject it = tracks.getJSONObject(i);

			if (!it.containsKey("artist"))
				continue;
			if (!it.getJSONObject("artist").containsKey("#text")) {
				JSONObject artist = it.getJSONObject("artist");
				facet.artist = artist.getString("name");
				facet.artist_mbid = artist.getString("mbid");
			} else {
				facet.artist = it.getJSONObject("artist").getString("#text");
			}
			if (!it.containsKey("name"))
				continue;
			String name = it.getString("name");

			facet.name = name;

			JSONObject dateObject = it.getJSONObject("date");
			if (!dateObject.containsKey("uts"))
				continue;

			long uts = dateObject.getLong("uts");
			long date = (Long.valueOf(uts)) * 1000;
			facet.time = date;
			facet.start = date;
			facet.end = date;

			if (it.containsKey("image")) {
				JSONArray images = it.getJSONArray("image");
				if (images != null) {
					StringBuffer bf = new StringBuffer();
					for (int j = 0; j < images.size(); j++) {
						if (bf.length() != 0)
							bf.append(",");
						JSONObject imageObject = images.getJSONObject(j);
						String size = imageObject.getString("size");
						if (size.trim().equalsIgnoreCase("small"))
							bf.append(imageObject.getString("#text"));
					}
					facet.imgUrls = bf.toString();
				}
			}

            if (it.containsKey("url"))
                facet.url = it.getString("url");
            if (it.containsKey("mbid"))
                facet.mbid = it.getString("mbid");

			LastFmRecentTrackFacet duplicate = jpaDaoService.findOne("lastfm.recent_track.byStartEnd",
					LastFmRecentTrackFacet.class,
					apiData.updateInfo.apiKey.getId(), date, date);
			if (duplicate==null)
				facets.add(facet);
		}
	}

}
