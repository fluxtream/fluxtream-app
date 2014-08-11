package org.fluxtream.core.connectors;

import org.springframework.beans.factory.annotation.Autowired;

import org.fluxtream.core.Configuration;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.springframework.beans.factory.annotation.Qualifier;

public class ApiClientSupport {

	@Autowired
	protected Configuration env;

    @Qualifier("connectorUpdateServiceImpl")
    @Autowired
	protected ConnectorUpdateService connectorUpdateService;

	protected final boolean hasReachedRateLimit(Connector connector, long guestId) {
		String rateLimitString = (String) env.connectors.getProperty(connector.getName()
				+ ".rateLimit");
		if (rateLimitString == null)
			rateLimitString = (String) env.connectors.getProperty("rateLimit");
		int count = Integer.valueOf(rateLimitString.split("/")[0]);
		int millis = Integer.valueOf(rateLimitString.split("/")[1]);
		long then = System.currentTimeMillis() - millis;
        long numberOfUpdates;
		if (rateLimitString.endsWith("/user")) {
			numberOfUpdates = connectorUpdateService
					.getNumberOfUpdatesSince(guestId, connector.value(), then);
        } else {
			numberOfUpdates = connectorUpdateService
					.getTotalNumberOfUpdatesSince(connector, then);
        }
        return numberOfUpdates >= count;
	}
	
}
