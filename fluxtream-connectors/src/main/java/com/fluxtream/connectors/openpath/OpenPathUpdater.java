package com.fluxtream.connectors.openpath;

import oauth.signpost.OAuthConsumer;

import org.springframework.beans.factory.annotation.Autowired;

import com.fluxtream.connectors.SignpostOAuthHelper;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.google_latitude.LocationFacet;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;

@Updater(prettyName = "OpenPath", value = 89, objectTypes = {
		LocationFacet.class}, hasFacets = true)
public class OpenPathUpdater extends AbstractUpdater {

	@Autowired
	SignpostOAuthHelper signpostHelper;
	
	public OpenPathUpdater() {
		super();
	}

	public void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws Exception {
	}

	OAuthConsumer consumer;
	
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
		
	}
	
}
