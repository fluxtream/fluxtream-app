package com.fluxtream.connectors.runkeeper;

import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component
@Updater(prettyName = "RunKeeper", value = 12, updateStrategyType = Connector.UpdateStrategyType.INCREMENTAL,
         objectTypes = {RunKeeperFitnessActivityFacet.class})
public class RunKeeperUpdater  extends AbstractUpdater {

    Logger logger = Logger.getLogger(RunKeeperUpdater.class);

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {

    }

    @Override
    protected void updateConnectorData(final UpdateInfo updateInfo) throws Exception {
    }
}
