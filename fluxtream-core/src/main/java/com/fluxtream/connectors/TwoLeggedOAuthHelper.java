package com.fluxtream.connectors;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.http.HttpParameters;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import com.fluxtream.connectors.updaters.RateLimitReachedException;

@Component
public class TwoLeggedOAuthHelper extends ApiClientSupport {

    public final String makeRestCall(Connector connector, long guestId,
                                     String accessToken, String tokenSecret, Map<String, String> additionalParameters,
                                     int objectTypes, String urlString) throws RateLimitReachedException {

        if (hasReachedRateLimit(connector, guestId))
            throw new RateLimitReachedException();

        try {
            long then = System.currentTimeMillis();
            URL url = new URL(urlString);
            HttpURLConnection request = (HttpURLConnection) url.openConnection();

            OAuthConsumer consumer = new DefaultOAuthConsumer(
                    accessToken, tokenSecret);
            if (additionalParameters != null && additionalParameters.size() > 0)
                addAdditionalParameters(consumer, additionalParameters);

            consumer.setTokenWithSecret(
                    "",
                    "");

            // sign the request (consumer is a Signpost DefaultOAuthConsumer)
            try {
                consumer.sign(request);
            } catch (Exception e) {
                throw new RuntimeException("OAuth exception: " + e.getMessage());
            }
            request.connect();
            if (request.getResponseCode() == 200) {
                String response = IOUtils.toString(request.getInputStream());
                connectorUpdateService.addApiUpdate(guestId, connector,
                        objectTypes, then, System.currentTimeMillis() - then,
                        urlString, true);
                // logger.info(apiKey.getGuestId(), "REST call success: " +
                // urlString);
                return response;
            } else {
                connectorUpdateService.addApiUpdate(guestId, connector,
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

    private void addAdditionalParameters(OAuthConsumer consumer,
                                         Map<String, String> additionalParameters) {
        for (String additionalParameterName : additionalParameters.keySet()) {
            HttpParameters additionalParameter = new HttpParameters();
            additionalParameter.put(additionalParameterName, additionalParameters.get(additionalParameterName));
            consumer.setAdditionalParameters(additionalParameter);
        }
    }

}
