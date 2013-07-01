package com.fluxtream.connectors.updaters;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fluxtream.domain.ApiKey;
import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.auth.oauth.OAuthParameters;
import com.google.api.client.googleapis.GoogleTransport;
import com.google.api.client.http.HttpTransport;
import com.google.gdata.client.authn.oauth.GoogleOAuthParameters;

public abstract class AbstractGoogleOAuthUpdater extends AbstractUpdater {

	public AbstractGoogleOAuthUpdater() {
		super();
	}

	protected final static DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");
	
	protected String getAccessToken(ApiKey apiKey) {
		return apiKey.getAttributeValue("accessToken", env);
	}
	
	protected String getTokenSecret(ApiKey apiKey) {
		return apiKey.getAttributeValue("tokenSecret", env);
	}
	
	protected GoogleOAuthParameters getOAuthParameters(ApiKey apiKey) {
		GoogleOAuthParameters oauthParameters = new GoogleOAuthParameters();
		
		String consumerKey = guestService.getApiKeyAttribute(apiKey, "googleConsumerKey");
		String consumerSecret = guestService.getApiKeyAttribute(apiKey, "googleConsumerSecret");
		
		oauthParameters.setOAuthConsumerKey(consumerKey);
		oauthParameters.setOAuthConsumerSecret(consumerSecret);
		oauthParameters.setOAuthToken(getAccessToken(apiKey));
		oauthParameters.setOAuthTokenSecret(getTokenSecret(apiKey));
		return oauthParameters;
	}

	protected HttpTransport getTransport(ApiKey apiKey) {
		HttpTransport transport = GoogleTransport.create();
        OAuthParameters authorizer = new OAuthParameters();
        authorizer.consumerKey = guestService.getApiKeyAttribute(apiKey, "googleConsumerKey");
        OAuthHmacSigner signer = new OAuthHmacSigner();
        signer.clientSharedSecret = guestService.getApiKeyAttribute(apiKey, "googleConsumerSecret");
        signer.tokenSharedSecret = getTokenSecret(apiKey);
        authorizer.signer = signer;
        authorizer.token = getAccessToken(apiKey);
        authorizer.signRequestsUsingAuthorizationHeader(transport);
		return transport;
	}
}
