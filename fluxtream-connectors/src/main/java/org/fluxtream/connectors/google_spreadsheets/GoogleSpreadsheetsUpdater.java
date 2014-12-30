package org.fluxtream.connectors.google_spreadsheets;

import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.ApiKey;
import org.springframework.stereotype.Component;

/**
 * Created by candide on 29/12/14.
 */
@Component
@Updater(prettyName = "Google Spreadsheets", value = 1, objectTypes = { GoogleSpreadsheetRowFacet.class }, updateStrategyType = Connector.UpdateStrategyType.INCREMENTAL)
public class GoogleSpreadsheetsUpdater extends AbstractUpdater {

    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {

    }

    @Override
    protected void updateConnectorData(UpdateInfo updateInfo) throws Exception {

    }

    @Override
    public void setDefaultChannelStyles(ApiKey apiKey) {

    }
}
