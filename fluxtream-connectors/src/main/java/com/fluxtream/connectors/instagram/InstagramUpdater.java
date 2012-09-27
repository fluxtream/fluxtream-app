package com.fluxtream.connectors.instagram;

import static com.fluxtream.utils.HttpUtils.fetch;

import org.springframework.stereotype.Component;

import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;

@Component
@Updater(prettyName = "Instagram", value = 14, objectTypes={InstagramPhotoFacet.class},
extractor=InstagramFacetExtractor.class)
public class InstagramUpdater extends AbstractUpdater {
	
	public InstagramUpdater() {
		super();
	}

	@Override
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
	}
	
	@Override
	public void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
		String accessToken = updateInfo.apiKey.getAttributeValue("accessToken", env);
		
		String feedUrl =
				"https://api.instagram.com/v1/users/self/feed?access_token=" + accessToken;

		String json;
			json = fetch(feedUrl);
		
		if (json!=null) {
			apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
		}
	}


}
