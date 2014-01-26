package com.fluxtream.connectors.up;

import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 26/01/14
 * Time: 09:56
 */
@Component
@Updater(prettyName = "Jawbone Up", value = 1999, objectTypes = {})
public class JawboneUpUpdater extends AbstractUpdater {

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {

    }

    @Override
    protected void updateConnectorData(final UpdateInfo updateInfo) throws Exception {

    }
}
