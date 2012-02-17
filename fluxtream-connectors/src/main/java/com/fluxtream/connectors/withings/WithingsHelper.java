package com.fluxtream.connectors.withings;

import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.GuestService;

public class WithingsHelper {

	public boolean checkAuthorization(GuestService guestService, long guestId) {
		ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector("WITHINGS"));
		return apiKey!=null;
	}
	
}
