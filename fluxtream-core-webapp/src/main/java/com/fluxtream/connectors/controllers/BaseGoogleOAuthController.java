package com.fluxtream.connectors.controllers;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.Guest;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.services.GuestService;
import com.google.api.client.auth.oauth.OAuthAuthorizeTemporaryTokenUrl;
import com.google.api.client.auth.oauth.OAuthCredentialsResponse;
import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.auth.oauth.OAuthParameters;
import com.google.api.client.googleapis.GoogleTransport;
import com.google.api.client.googleapis.auth.oauth.GoogleOAuthGetAccessToken;
import com.google.api.client.googleapis.auth.oauth.GoogleOAuthGetTemporaryToken;
import com.google.api.client.http.HttpTransport;
import com.google.gdata.client.authn.oauth.GoogleOAuthHelper;

public abstract class BaseGoogleOAuthController {

	protected abstract GuestService guestService();
	
	protected abstract GoogleOAuthHelper getOAuthHelper();
	
	private OAuthHmacSigner signer;
	
	protected String accessToken(HttpServletRequest request, String tokenSecretKey, String scope, String oauthCallback)
		throws IOException, ServletException
	{
		GoogleOAuthGetTemporaryToken temporaryToken = new GoogleOAuthGetTemporaryToken();
		signer = new OAuthHmacSigner();
		signer.clientSharedSecret = getConsumerSecret();
		temporaryToken.signer = signer;
		temporaryToken.consumerKey = getConsumerKey();
		temporaryToken.scope = scope;
		temporaryToken.callback = oauthCallback;
		OAuthCredentialsResponse tempCredentials = temporaryToken.execute();
		signer.tokenSharedSecret = tempCredentials.tokenSecret;
		OAuthAuthorizeTemporaryTokenUrl authorizeUrl = new OAuthAuthorizeTemporaryTokenUrl(
				"https://www.google.com/accounts/OAuthAuthorizeToken");
		
		request.getSession().setAttribute(tokenSecretKey,
				tempCredentials.token);
		
		authorizeUrl.temporaryToken = tempCredentials.token;
		String authorizationUrl = authorizeUrl.build();
		
		return "redirect:" + authorizationUrl;
	}
	
	protected String authorizeToken(HttpServletRequest request, String tokenSecretKey,
			Connector api, Map<String,String> extraParameters) throws IOException {
		
		HttpTransport transport = GoogleTransport.create();
		
		GoogleOAuthGetAccessToken accessToken = new GoogleOAuthGetAccessToken();
		String tempToken = (String) request.getSession().getAttribute(
				tokenSecretKey);

		accessToken.temporaryToken = tempToken;
		accessToken.signer = signer;
		accessToken.consumerKey = getConsumerKey();
		accessToken.verifier = request.getParameter("oauth_verifier");
		OAuthCredentialsResponse credentials = accessToken.execute();
		signer.tokenSharedSecret = credentials.tokenSecret;
		
		createOAuthParameters(credentials).signRequestsUsingAuthorizationHeader(transport);
		
		Guest guest = AuthHelper.getGuest();

		guestService().setApiKeyAttribute(guest.getId(), api, "accessToken", credentials.token);
		guestService().setApiKeyAttribute(guest.getId(), api, "tokenSecret", credentials.tokenSecret);
				
		return "redirect:/app/from/"+api.getName();
	}

	private OAuthParameters createOAuthParameters(OAuthCredentialsResponse credentials) {
		OAuthParameters authorizer = new OAuthParameters();
		authorizer.consumerKey = getConsumerKey();
		authorizer.signer = signer;
		authorizer.token = credentials.token;
		return authorizer;
	}
	
	protected abstract String getConsumerKey();

	protected abstract String getConsumerSecret();
	
}
