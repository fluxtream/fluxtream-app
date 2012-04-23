package com.fluxtream.connectors.openpath;

import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.TwoLeggedOAuthHelper;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.google_latitude.LocationFacet;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.RateLimitReachedException;
import com.fluxtream.connectors.updaters.UpdateInfo;

@Component
@Updater(prettyName = "OpenPath", value = 89, objectTypes = { LocationFacet.class }, hasFacets = true)
public class OpenPathUpdater extends AbstractUpdater {

	@Autowired
	TwoLeggedOAuthHelper twoLeggedOAuthHelper;

	Connector connector;

	public OpenPathUpdater() {
		super();
		connector = Connector.getConnector("openpath");
	}

	public boolean testConnection(long guestId, String accessKey,
			String secretKey) throws RateLimitReachedException {
		try {
			String restResponse = twoLeggedOAuthHelper.makeRestCall(connector,
					guestId, accessKey, secretKey, null, -1,
					"https://openpaths.cc/api/1");
			System.out.println("restResponse: " + restResponse);
			JSONObject.fromObject(restResponse);
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

	public void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws Exception {
	}

	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {

	}

}
