package com.fluxtream.connectors.mymee;

import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.quantifiedmind.QuantifiedMindTestFacet;
import com.fluxtream.connectors.quantifiedmind.QuantifiedMindTestFacetExtractor;
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
@Updater(prettyName = "Mymee", value = 110, updateStrategyType = Connector.UpdateStrategyType.INCREMENTAL,
         objectTypes = {MymeeObservationFacet.class}, extractor = MymeeObservationFacetExtractor.class)
public class MymeeUpdater extends AbstractUpdater {

    @Autowired
    GuestService guestService;

    public MymeeUpdater() {
        super();
    }

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        apiDataService.eraseApiData(updateInfo.getGuestId(), connector());
        loadEverything(updateInfo);
    }

    @Override
    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        apiDataService.eraseApiData(updateInfo.getGuestId(), connector());
        loadEverything(updateInfo);
    }

    private void loadEverything(final UpdateInfo updateInfo) throws Exception {
        long then = System.currentTimeMillis();
        String queryUrl = guestService.getApiKeyAttribute(updateInfo.getGuestId(), connector(), "fetchURL");
        try {
            final String json = HttpUtils.fetch(queryUrl);
            apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
        }
        catch (Exception e) {
            countFailedApiCall(updateInfo.apiKey.getGuestId(), updateInfo.objectTypes, then,
                               queryUrl, Utils.stackTrace(e));
            throw new Exception("Could not get Mymee observations: "
                                + e.getMessage() + "\n" + Utils.stackTrace(e));
        }

        countSuccessfulApiCall(updateInfo.apiKey.getGuestId(), updateInfo.objectTypes, then, queryUrl);
    }

}