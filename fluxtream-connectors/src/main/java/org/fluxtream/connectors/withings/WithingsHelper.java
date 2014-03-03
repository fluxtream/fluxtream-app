package org.fluxtream.connectors.withings;

import org.fluxtream.connectors.Connector;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.services.GuestService;

public class WithingsHelper {

	public boolean checkAuthorization(GuestService guestService, long guestId) {
		ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector("WITHINGS"));
		return apiKey!=null;
	}
	
}
