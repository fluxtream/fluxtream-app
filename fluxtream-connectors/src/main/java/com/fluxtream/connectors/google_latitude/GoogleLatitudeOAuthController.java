package com.fluxtream.connectors.google_latitude;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.services.GuestService;
import com.google.api.client.auth.oauth.OAuthAuthorizeTemporaryTokenUrl;
import com.google.api.client.auth.oauth.OAuthCredentialsResponse;
import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.auth.oauth.OAuthParameters;
import com.google.api.client.googleapis.GoogleTransport;
import com.google.api.client.googleapis.auth.oauth.GoogleOAuthGetAccessToken;
import com.google.api.client.googleapis.auth.oauth.GoogleOAuthGetTemporaryToken;
import com.google.api.client.http.HttpTransport;

@Controller
@RequestMapping(value = "/google_latitude")
public class GoogleLatitudeOAuthController {

	private static final String GOOGLE_LATITUDE_SCOPE = "https://www.googleapis.com/auth/latitude";
	private static final String LATITUDE_TOKEN_SECRET = "latitudeTokenSecret";

	@Autowired
	Configuration env;

	@Autowired
	GuestService guestService;

	private OAuthHmacSigner signer;

	@RequestMapping(value = "/token")
	public String getLatitudeToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {

		String oauthCallback = env.get("homeBaseUrl")
				+ "google_latitude/upgradeToken";

		GoogleOAuthGetTemporaryToken temporaryToken = new GoogleOAuthGetTemporaryToken();
		signer = new OAuthHmacSigner();
		signer.clientSharedSecret = getConsumerSecret();
		temporaryToken.signer = signer;
		temporaryToken.consumerKey = getConsumerKey();
		temporaryToken.scope = GOOGLE_LATITUDE_SCOPE;
		temporaryToken.displayName = "fluxtream";
		temporaryToken.callback = oauthCallback;
		OAuthCredentialsResponse tempCredentials = temporaryToken.execute();
		signer.tokenSharedSecret = tempCredentials.tokenSecret;
		OAuthAuthorizeTemporaryTokenUrl authorizeUrl = new OAuthAuthorizeTemporaryTokenUrl(
				"https://www.google.com/latitude/apps/OAuthAuthorizeToken");
		authorizeUrl.put("domain", getConsumerKey());
		authorizeUrl.put("location", "all");
		authorizeUrl.put("granularity", "best");

		request.getSession().setAttribute(LATITUDE_TOKEN_SECRET,
				tempCredentials.token);

		authorizeUrl.temporaryToken = tempCredentials.token;
		String authorizationUrl = authorizeUrl.build();

		System.out.println("redirect url: " + authorizationUrl);

		return "redirect:" + authorizationUrl;
	}

	@RequestMapping(value = "/upgradeToken")
	public String upgradeToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		GoogleOAuthGetAccessToken accessToken = new GoogleOAuthGetAccessToken();
		String tempToken = (String) request.getSession().getAttribute(
				LATITUDE_TOKEN_SECRET);

		accessToken.temporaryToken = tempToken;
		accessToken.signer = signer;
		accessToken.consumerKey = getConsumerKey();
		accessToken.verifier = request.getParameter("oauth_verifier");
		OAuthCredentialsResponse credentials = accessToken.execute();
		signer.tokenSharedSecret = credentials.tokenSecret;
		HttpTransport transport = GoogleTransport.create();
		
		createOAuthParameters(credentials).signRequestsUsingAuthorizationHeader(transport);
		
		Guest guest = ControllerHelper.getGuest();

		guestService().setApiKeyAttribute(guest.getId(), Connector.getConnector("GOOGLE_LATITUDE"), "accessToken", credentials.token);
		guestService().setApiKeyAttribute(guest.getId(), Connector.getConnector("GOOGLE_LATITUDE"), "tokenSecret", credentials.tokenSecret);
		
		return "redirect:/app/from/"+Connector.getConnector("GOOGLE_LATITUDE").getName();
	}

	private OAuthParameters createOAuthParameters(OAuthCredentialsResponse credentials) {
		OAuthParameters authorizer = new OAuthParameters();
		authorizer.consumerKey = getConsumerKey();
		authorizer.signer = signer;
		authorizer.token = credentials.token;
		return authorizer;
	}

	GuestService guestService() {
		return guestService;
	}

	String getConsumerKey() {
		return env.get("googleConsumerKey");
	}

	String getConsumerSecret() {
		return env.get("googleConsumerSecret");
	}

}
