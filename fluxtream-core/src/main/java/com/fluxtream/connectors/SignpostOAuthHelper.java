package com.fluxtream.connectors;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.http.HttpParameters;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import com.fluxtream.connectors.updaters.RateLimitReachedException;
import com.fluxtream.domain.ApiKey;

@Component
public class SignpostOAuthHelper extends ApiClientSupport {

	public final String makeRestCall(Connector connector, ApiKey apiKey,
			int objectTypes, String urlString) throws RateLimitReachedException {

		if (hasReachedRateLimit(connector, apiKey.getGuestId()))
			throw new RateLimitReachedException();

		try {
			long then = System.currentTimeMillis();
			URL url = new URL(urlString);
			HttpURLConnection request = (HttpURLConnection) url.openConnection();
			
			OAuthConsumer consumer = new DefaultOAuthConsumer(
					getConsumerKey(connector), getConsumerSecret(connector));
	
			consumer.setTokenWithSecret(
					apiKey.getAttributeValue("accessToken", env),
					apiKey.getAttributeValue("tokenSecret", env));
			if (connector.hasAdditionalParameters()) {
				addAdditionalParameters(consumer, apiKey, connector.getAdditionalParameters());
			}
			
			// sign the request (consumer is a Signpost DefaultOAuthConsumer)
			try {
				consumer.sign(request);
			} catch (Exception e) {
				throw new RuntimeException("OAuth exception: " + e.getMessage());
			}
			request.connect();
			if (request.getResponseCode() == 200) {
				String json = IOUtils.toString(request.getInputStream());
				connectorUpdateService.addApiUpdate(apiKey.getGuestId(), connector,
						objectTypes, then, System.currentTimeMillis() - then,
						urlString, true);
				// logger.info(apiKey.getGuestId(), "REST call success: " +
				// urlString);
				return json;
			} else {
				connectorUpdateService.addApiUpdate(apiKey.getGuestId(), connector,
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

	private void addAdditionalParameters(OAuthConsumer consumer, ApiKey apiKey,
			String[] additionalParameters) {
		for (String additionalParameterName : additionalParameters) {
			HttpParameters additionalParameter = new HttpParameters();
			additionalParameter.put("api_key", apiKey.getAttributeValue(additionalParameterName, env));
			consumer.setAdditionalParameters(additionalParameter);
		}
	}

	private String getConsumerSecret(Connector connector) {
		return env.get(connector.getName() + "ConsumerSecret");
	}

	private String getConsumerKey(Connector connector) {
		return env.get(connector.getName() + "ConsumerKey");
	}

}
