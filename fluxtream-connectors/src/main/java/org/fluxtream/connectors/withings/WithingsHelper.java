package org.fluxtream.connectors.withings;

import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.services.GuestService;

public class WithingsHelper {

	public boolean checkAuthorization(GuestService guestService, long guestId) {
		ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector("WITHINGS"));
		return apiKey!=null;
	}
	
}
