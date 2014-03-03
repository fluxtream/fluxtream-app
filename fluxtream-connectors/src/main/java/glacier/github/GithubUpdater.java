package glacier.github;

import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.annotations.Updater;
import org.fluxtream.connectors.updaters.AbstractUpdater;
import org.fluxtream.connectors.updaters.UpdateInfo;
import org.fluxtream.domain.ApiUpdate;
import org.fluxtream.services.GuestService;
import org.fluxtream.utils.HttpUtils;
import org.fluxtream.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author candide
 *
 */

@Component
@Updater(prettyName = "Github", value = 200, updateStrategyType = Connector.UpdateStrategyType.INCREMENTAL,
         objectTypes = {GithubPushFacet.class}, extractor = GithubPushFacetExtractor.class)
public class GithubUpdater extends AbstractUpdater {

    @Autowired
    GuestService guestService;
    
    public GithubUpdater() {
        super();
    }

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        if (!connectorUpdateService.isHistoryUpdateCompleted(updateInfo.apiKey, updateInfo.objectTypes)) {
            apiDataService.eraseApiData(updateInfo.apiKey);
        }
        loadHistory(updateInfo, 0, System.currentTimeMillis());
    }

    @Override
    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        ApiUpdate lastUpdate = connectorUpdateService.getLastSuccessfulUpdate(updateInfo.apiKey);
        loadHistory(updateInfo, lastUpdate.ts, System.currentTimeMillis());
    }

    private void loadHistory(UpdateInfo updateInfo, long from, long to) throws Exception {
        String queryUrl = "request url not set yet";
        long then = System.currentTimeMillis();

        String accessToken = guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken");

        try {
            queryUrl = "https://api.singly.com/services/github/events?limit=10000&access_token=" + accessToken + "&since=" + from + "&until=" + to;
            final String json = HttpUtils.fetch(queryUrl);
            apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
        }
        catch (Exception e) {
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then,
                               queryUrl, Utils.stackTrace(e), null, null);
            throw new Exception("Could not get GitHub Commits (from Singly): "
                                + e.getMessage() + "\n" + Utils.stackTrace(e));
        }

        countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, queryUrl);
    }

}