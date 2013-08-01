package com.fluxtream.connectors;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import com.fluxtream.connectors.updaters.RateLimitReachedException;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.GuestService;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SignpostOAuthHelper extends ApiClientSupport {

    @Autowired
    GuestService guestService;

	public final String makeRestCall(ApiKey apiKey,
			int objectTypes, String urlString) throws RateLimitReachedException {

		if (hasReachedRateLimit(apiKey.getConnector(), apiKey.getGuestId()))
			throw new RateLimitReachedException();

		try {
			long then = System.currentTimeMillis();
			URL url = new URL(urlString);
			HttpURLConnection request = (HttpURLConnection) url.openConnection();
			
			OAuthConsumer consumer = new DefaultOAuthConsumer(
					getConsumerKey(apiKey), getConsumerSecret(apiKey));
	
			consumer.setTokenWithSecret(
					apiKey.getAttributeValue("accessToken", env),
					apiKey.getAttributeValue("tokenSecret", env));

			// sign the request (consumer is a Signpost DefaultOAuthConsumer)
			try {
				consumer.sign(request);
			} catch (Exception e) {
				throw new RuntimeException("OAuth exception: " + e.getMessage());
			}
			request.connect();
			if (request.getResponseCode() == 200) {
				String json = IOUtils.toString(request.getInputStream());
				connectorUpdateService.addApiUpdate(apiKey,
						objectTypes, then, System.currentTimeMillis() - then,
						urlString, true);
				// logger.info(apiKey.getGuestId(), "REST call success: " +
				// urlString);
				return json;
			} else {
				connectorUpdateService.addApiUpdate(apiKey,
						objectTypes, then, System.currentTimeMillis() - then,
						urlString, false);
				throw new RuntimeException(
						"Could not make REST call, got response code: "
								+ request.getResponseCode() + ", message: "
								+ request.getResponseMessage() + "\n+REST url: "
								+ urlString);
			}
		} catch (IOException e) {
			throw new RuntimeException("IOException trying to make rest call: " + e.getMessage());
		}
	}

	private String getConsumerSecret(ApiKey apiKey) {
		String consumerSecret = guestService.getApiKeyAttribute(apiKey, apiKey.getConnector().getName() + "ConsumerSecret");
		return consumerSecret == null ? "" : consumerSecret;
	}

	private String getConsumerKey(ApiKey apiKey) {
		String consumerKey = guestService.getApiKeyAttribute(apiKey, apiKey.getConnector().getName() + "ConsumerKey");
		return consumerKey == null ? "" : consumerKey;
	}

}
