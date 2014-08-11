package org.fluxtream.connectors.fitbit;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.SignpostOAuthHelper;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.services.GuestService;

@Component(value="fitbitHelper")
public class FitbitAdminHelper {

	@Autowired
	SignpostOAuthHelper signpostHelper;

	@Autowired
	GuestService guestService;
	
	public JSONArray getApiSubscriptions(long guestId) throws Exception {
		ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector("fitbit"));
		
		String json = signpostHelper
				.makeRestCall(apiKey, -10,
						"http://api.fitbit.com/1/user/-/apiSubscriptions.json");

		JSONObject wrapper = JSONObject.fromObject(json);
		JSONArray jsonSubscriptions = wrapper.getJSONArray("apiSubscriptions");
		return jsonSubscriptions;
	}
	
	public JSONArray deleteApiSubscription(long guestId) throws Exception {
		ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector("fitbit"));
		
		String json = signpostHelper
				.makeRestCall(apiKey, -10,
						"http://api.fitbit.com/1/user/-/apiSubscriptions.json");

		JSONObject wrapper = JSONObject.fromObject(json);
		JSONArray jsonSubscriptions = wrapper.getJSONArray("apiSubscriptions");
		return jsonSubscriptions;
	}
	
}
