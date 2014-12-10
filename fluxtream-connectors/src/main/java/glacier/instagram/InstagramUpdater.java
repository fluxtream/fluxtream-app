package glacier.instagram;

import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.ApiKey;
import org.springframework.stereotype.Component;

import static org.fluxtream.core.utils.HttpUtils.fetch;

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
		String accessToken = guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken");
		
		String feedUrl =
				"https://api.instagram.com/v1/users/self/feed?access_token=" + accessToken;

		String json;
			json = fetch(feedUrl);
		
		if (json!=null) {
			apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
		}
	}

    @Override
    public void setDefaultChannelStyles(ApiKey apiKey) {}


}
