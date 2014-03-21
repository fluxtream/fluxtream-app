package org.fluxtream.connectors;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.fluxtream.connectors.updaters.RateLimitReachedException;
import org.fluxtream.connectors.updaters.UnexpectedResponseCodeException;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.services.GuestService;
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
			int objectTypes, String urlString) throws RateLimitReachedException, UnexpectedResponseCodeException {

		if (hasReachedRateLimit(apiKey.getConnector(), apiKey.getGuestId()))
			throw new RateLimitReachedException();

		try {
			long then = System.currentTimeMillis();
			URL url = new URL(urlString);
			HttpURLConnection request = (HttpURLConnection) url.openConnection();
			
			OAuthConsumer consumer = new DefaultOAuthConsumer(
					getConsumerKey(apiKey), getConsumerSecret(apiKey));
	
			consumer.setTokenWithSecret(
                    guestService.getApiKeyAttribute(apiKey,"accessToken"),
                    guestService.getApiKeyAttribute(apiKey,"tokenSecret"));

			// sign the request (consumer is a Signpost DefaultOAuthConsumer)
			try {
				consumer.sign(request);
			} catch (Exception e) {
				throw new RuntimeException("OAuth exception: " + e.getMessage());
			}
			request.connect();
            final int httpResponseCode = request.getResponseCode();
            final String httpResponseMessage = request.getResponseMessage();
            if (httpResponseCode == 200) {
				String json = IOUtils.toString(request.getInputStream());
				connectorUpdateService.addApiUpdate(apiKey,
						objectTypes, then, System.currentTimeMillis() - then,
						urlString, true, httpResponseCode, httpResponseMessage);
				// logger.info(apiKey.getGuestId(), "REST call success: " +
				// urlString);
				return json;
			} else {
				connectorUpdateService.addApiUpdate(apiKey,
						objectTypes, then, System.currentTimeMillis() - then,
						urlString, false, httpResponseCode, httpResponseMessage);
				throw new UnexpectedResponseCodeException(httpResponseCode,
                                                          httpResponseMessage,
                                                          urlString);
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
