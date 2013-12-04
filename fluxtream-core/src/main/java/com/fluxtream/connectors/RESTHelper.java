package com.fluxtream.connectors;

import com.fluxtream.domain.ApiKey;
import com.fluxtream.utils.UnexpectedHttpResponseCodeException;
import org.springframework.stereotype.Component;

import com.fluxtream.connectors.updaters.RateLimitReachedException;
import com.fluxtream.utils.HttpUtils;

@Component
public class RESTHelper extends ApiClientSupport {
	
	public final String makeRestCall(final ApiKey apiKey,
			int objectTypes, String urlString) throws Exception {
		
		if (hasReachedRateLimit(apiKey.getConnector(), apiKey.getGuestId()))
			throw new RateLimitReachedException();
		
		long then = System.currentTimeMillis();
		try {
			String restResult = HttpUtils.fetch(urlString);
			connectorUpdateService.addApiUpdate(apiKey,
					objectTypes, then, System.currentTimeMillis() - then,
					urlString, true, null, null);
			return restResult;
		} catch (UnexpectedHttpResponseCodeException e) {
			connectorUpdateService.addApiUpdate(apiKey,
					objectTypes, then, System.currentTimeMillis() - then,
					urlString, false, e.getHttpResponseCode(), e.getHttpResponseMessage());
			throw e;
		}
	}

}
