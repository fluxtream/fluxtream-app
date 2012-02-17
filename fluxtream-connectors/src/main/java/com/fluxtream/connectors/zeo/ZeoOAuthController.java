package com.fluxtream.connectors.zeo;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.controllers.BaseOAuthController;
import com.fluxtream.services.GuestService;
import com.google.gdata.client.authn.oauth.GoogleOAuthHelper;
import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;

@Controller
@RequestMapping(value="/zeo")
public class ZeoOAuthController extends BaseOAuthController {

	@Autowired
	GuestService guestService;
	
	@Autowired
	Configuration env;
	private static final String ZEO_TOKEN_SECRET = "zeoTokenSecret";
	
	@RequestMapping(value = "/token")
	public String getLatitudeToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		return super.accessToken(request,
				ZEO_TOKEN_SECRET, null, env.get("homeBaseUrl")
						+ "zeo/upgradeToken");
	}

	@RequestMapping(value = "/upgradeToken")
	public String upgradeToken(HttpServletRequest request,
			HttpServletResponse response) throws OAuthException {
		return super.authorizeToken(request,
				ZEO_TOKEN_SECRET, Connector.getConnector("ZEO"), null);
	}
	
	@Override
	protected GuestService guestService() {
		return guestService;
	}

	@Override
	protected String getConsumerKey() {
		return env.get("zeoConsumerKey");
	}

	@Override
	protected String getConsumerSecret() {
		return env.get("zeoConsumerSecret");
	}

	@Override
	protected GoogleOAuthHelper getOAuthHelper() {
		GoogleOAuthHelper helper = new GoogleOAuthHelper(new OAuthHmacSha1Signer());
		helper.setAccessTokenUrl("http://api.myzeo.com:8080/zeows/oauth/access_token");
		helper.setRequestTokenUrl("http://api.myzeo.com:8080/zeows/oauth/request_token");
		helper.setUserAuthorizationUrl("http://api.myzeo.com:8080/zeows/oauth/confirm_access");
		return helper;
	}


}
