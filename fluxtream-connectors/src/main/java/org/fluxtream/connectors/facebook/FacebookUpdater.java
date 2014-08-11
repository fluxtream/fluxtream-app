package org.fluxtream.connectors.facebook;

import org.springframework.stereotype.Component;

import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
/**
 * @author candide
 *
 */

@Component
@Updater(prettyName = "Facebook", value = 30, hasFacets=false, objectTypes ={})
public class FacebookUpdater extends AbstractUpdater {

	public FacebookUpdater() {
		super();
	}

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
    }

	@Override
	public void updateConnectorData(UpdateInfo updateInfo) {
	}

}
