package com.fluxtream.connectors.quantifiedmind;

import java.net.URL;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.services.GuestService;
import com.fluxtream.utils.HttpUtils;
import com.fluxtream.utils.Utils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author candide
 *
 */

@Component
@Updater(prettyName = "QuantifiedMind", value = 100, updateStrategyType = Connector.UpdateStrategyType.INCREMENTAL,
         objectTypes = {QuantifiedMindTestFacet.class}, extractor = QuantifiedMindTestFacetExtractor.class)
public class QuantifiedMindUpdater extends AbstractUpdater {

    @Autowired
    GuestService guestService;

    public QuantifiedMindUpdater() {
        super();
    }

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        if (!connectorUpdateService.isHistoryUpdateCompleted(updateInfo.getGuestId(), connector().getName(), updateInfo.objectTypes)) {
            apiDataService.eraseApiData(updateInfo.getGuestId(), connector());
        }
        loadHistory(updateInfo, 0, System.currentTimeMillis());
    }

    @Override
    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        ApiUpdate lastUpdate = connectorUpdateService.getLastSuccessfulUpdate(updateInfo.apiKey.getGuestId(), connector());
        loadHistory(updateInfo, lastUpdate.ts, System.currentTimeMillis());
    }

    private void loadHistory(UpdateInfo updateInfo, long from, long to) throws Exception {
        String queryUrl = "request url not set yet";
        long then = System.currentTimeMillis();
        String username = guestService.getApiKeyAttribute(updateInfo.getGuestId(),
                                                          Connector.getConnector("quantifiedmind"),
                                                          "username");
        String token = guestService.getApiKeyAttribute(updateInfo.getGuestId(),
                                                       Connector.getConnector("quantifiedmind"),
                                                       "token");
        try {
            boolean partialResult = false;
            String start_time = null;
            do {
                queryUrl = "http://www.quantified-mind.com/api/get_session_data?username=" + username + "&token=" + token;
                if (partialResult)
                    queryUrl += "&start_time=" + start_time;
                final String json = HttpUtils.fetch(queryUrl, env);
                JSONObject jsonObject = JSONObject.fromObject(json);
                String status = jsonObject.getString("status");
                partialResult = status.equals("partial");
                if (partialResult)
                    start_time = jsonObject.getString("next_date");
                JSONArray sessionData = jsonObject.getJSONArray("session_data");
                final String sessionDataJSON = sessionData.toString();
                apiDataService.cacheApiDataJSON(updateInfo, sessionDataJSON, -1, -1);
            } while (partialResult);
        }
        catch (Exception e) {
            countFailedApiCall(updateInfo.apiKey.getGuestId(), updateInfo.objectTypes, then,
                               queryUrl, Utils.stackTrace(e));
            throw new Exception("Could not get QuantifiedMind tests: "
                                + e.getMessage() + "\n" + Utils.stackTrace(e));
        }

        countSuccessfulApiCall(updateInfo.apiKey.getGuestId(), updateInfo.objectTypes, then, queryUrl);
    }

}