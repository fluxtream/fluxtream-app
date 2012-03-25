package com.fluxtream.connectors.bodytrack;

import org.springframework.stereotype.Component;

import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
/**
 * @author candide
 *
 */

@Component
@Updater(prettyName = "BodyTrack", value = 77, objectTypes ={}, hasFacets = false)
public class BodytrackUpdater extends AbstractUpdater {

	public BodytrackUpdater() {
		super();
	}

	@Override
	public void updateConnectorData(UpdateInfo updateInfo) {
	}

}
