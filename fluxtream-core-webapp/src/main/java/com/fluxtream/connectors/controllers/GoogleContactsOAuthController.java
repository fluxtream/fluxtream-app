package com.fluxtream.connectors.controllers;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.services.GuestService;
import com.google.gdata.client.authn.oauth.GoogleOAuthHelper;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;

@Controller
@RequestMapping(value="/contacts")
public class GoogleContactsOAuthController extends BaseGoogleOAuthController {

	private static final String GOOGLE_CONTACTS_SCOPE = "https://www.google.com/m8/feeds/";
	private static final String CONTACTS_TOKEN_SECRET = "contactsTokenSecret";

	@Autowired
	Configuration env;
	
	@Autowired
	GuestService guestService;

	@RequestMapping(value = "/token")
	public String getLatitudeToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		return super.accessToken(request,
				CONTACTS_TOKEN_SECRET, GOOGLE_CONTACTS_SCOPE, env.get("homeBaseUrl")
						+ "contacts/upgradeToken");
	}

	@RequestMapping(value = "/upgradeToken")
	public String upgradeToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		return super.authorizeToken(request,
				CONTACTS_TOKEN_SECRET, Connector.getConnector("GOOGLE_CONTACTS"), null);
	}

	@Override
	protected String getConsumerKey() {
		return env.get("googleConsumerKey");
	}

	@Override
	protected String getConsumerSecret() {
		return env.get("googleConsumerSecret");
	}
	
	protected GuestService guestService() { return guestService; }

	@Override
	protected GoogleOAuthHelper getOAuthHelper() {
		return new GoogleOAuthHelper(new OAuthHmacSha1Signer());
	}

}
