package com.fluxtream.connectors.bodymedia;

import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;

@Updater(prettyName = "BodyMedia", value = 88, objectTypes = {
		BodymediaBurnFacet.class, BodymediaSleepFacet.class,
		BodymediaStepsFacet.class }, hasFacets = true)
public class BodymediaUpdater extends AbstractUpdater {
	
	public BodymediaUpdater() {
		super();
	}

	public void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws Exception {
		ObjectType burnOT = ObjectType.getObjectType(connector(), "burn");
	}
	
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
	}
}
