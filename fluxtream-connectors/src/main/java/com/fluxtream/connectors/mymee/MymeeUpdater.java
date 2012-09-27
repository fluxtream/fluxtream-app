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
        if (!connectorUpdateService.isHistoryUpdateCompleted(updateInfo.getGuestId(), connector().getName(), updateInfo.objectTypes)) {
            apiDataService.eraseApiData(updateInfo.getGuestId(), connector());
        }
    }

    @Override
    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        ApiUpdate lastUpdate = connectorUpdateService.getLastSuccessfulUpdate(updateInfo.apiKey.getGuestId(), connector());
    }

}