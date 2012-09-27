package com.fluxtream.connectors;

import org.springframework.stereotype.Component;

import com.fluxtream.connectors.updaters.RateLimitReachedException;
import com.fluxtream.utils.HttpUtils;

@Component
public class RESTHelper extends ApiClientSupport {
	
	public final String makeRestCall(Connector connector, long guestId,
			int objectTypes, String urlString) throws Exception {
		
		if (hasReachedRateLimit(connector, guestId))
			throw new RateLimitReachedException();
		
		long then = System.currentTimeMillis();
		try {
			String restResult = HttpUtils.fetch(urlString);
			connectorUpdateService.addApiUpdate(guestId, connector,
					objectTypes, then, System.currentTimeMillis() - then,
					urlString, true);
			return restResult;
		} catch (Exception e) {
			connectorUpdateService.addApiUpdate(guestId, connector,
					objectTypes, then, System.currentTimeMillis() - then,
					urlString, false);
			throw e;
		}
	}

}
