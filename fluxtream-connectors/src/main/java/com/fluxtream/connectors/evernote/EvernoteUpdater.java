package com.fluxtream.connectors.evernote;

import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Updater(prettyName = "Evernote", value = 17, objectTypes ={})
public class EvernoteUpdater extends AbstractUpdater {

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
    }

    @Override
    protected void updateConnectorData(final UpdateInfo updateInfo) throws Exception {
    }

}
