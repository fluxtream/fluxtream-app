package org.fluxtream.connectors.misfit;

import org.fluxtream.core.connectors.Autonomous;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.ApiKey;
import org.springframework.stereotype.Component;

/**
 * Created by candide on 09/02/15.
 */
@Component
@Updater(prettyName = "Misfit", value = 8, objectTypes = {MisfitActivitySummaryFacet.class, MisfitActivitySessionFacet.class, MisfitSleepFacet.class},
        userProfile = MisfitUserProfile.class
//        bodytrackResponder = MisfitBodytrackResponder.class,
//        defaultChannels = {"Misfit.steps"}
)
public class MisfitUpdater extends AbstractUpdater implements Autonomous {


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
