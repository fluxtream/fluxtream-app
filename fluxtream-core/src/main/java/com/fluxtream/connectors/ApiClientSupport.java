package com.fluxtream.connectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.fluxtream.Configuration;
import com.fluxtream.services.ConnectorUpdateService;

public class ApiClientSupport {

	@Autowired
	protected Configuration env;

	@Autowired
	protected ConnectorUpdateService connectorUpdateService;

	protected final boolean hasReachedRateLimit(Connector connector, long guestId) {
		String rateLimitString = env.connectors.get(connector.getName()
				+ ".rateLimit");
		if (rateLimitString == null)
			rateLimitString = env.connectors.get("rateLimit");
		int count = Integer.valueOf(rateLimitString.split("/")[0]);
		int millis = Integer.valueOf(rateLimitString.split("/")[1]);
		long then = System.currentTimeMillis() - millis;
		if (rateLimitString.endsWith("/user")) {
			long numberOfUpdates = connectorUpdateService
					.getNumberOfUpdatesSince(guestId, connector, then);
			if (numberOfUpdates >= count) {
				return true;
			}
			return false;
		} else {
			long numberOfUpdates = connectorUpdateService
					.getTotalNumberOfUpdatesSince(connector, then);
			if (numberOfUpdates >= count)
				return true;
			return false;
		}
	}
}
