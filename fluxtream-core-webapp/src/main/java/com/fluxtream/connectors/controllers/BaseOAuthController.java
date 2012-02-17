package com.fluxtream.connectors.controllers;

import static com.fluxtream.connectors.controllers.ControllerSupport.error;
import static com.fluxtream.utils.Utils.stackTrace;

import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.services.GuestService;
import com.google.gdata.client.authn.oauth.GoogleOAuthHelper;
import com.google.gdata.client.authn.oauth.GoogleOAuthParameters;
import com.google.gdata.client.authn.oauth.OAuthException;

public abstract class BaseOAuthController {

	protected abstract GuestService guestService();

	protected abstract GoogleOAuthHelper getOAuthHelper();

	protected String accessToken(HttpServletRequest request, String tokenSecretKey, String scope, String oauthCallback) {
		GoogleOAuthParameters oauthParameters = new GoogleOAuthParameters();
		oauthParameters.setOAuthConsumerKey(getConsumerKey());
		oauthParameters.setOAuthConsumerSecret(getConsumerSecret());
		if (scope!=null) oauthParameters.setScope(scope);

		if (request.getParameter("guestId")!=null)
			oauthCallback += "?guestId=" + request.getParameter("guestId");

		oauthParameters
				.setOAuthCallback(oauthCallback);

		GoogleOAuthHelper oauthHelper = getOAuthHelper();
		try {
			oauthHelper.getUnauthorizedRequestToken(oauthParameters);
		} catch (OAuthException e) {
			e.printStackTrace();
			return error(request, "Oops, something went wrong", stackTrace(e));
		}

		String oAuthTokenSecret = oauthParameters.getOAuthTokenSecret();

		if (oAuthTokenSecret==null) {
			return error(request, "There was an error getting your secret token from Google",
					"Something went wrong during the oauth dance");
		}

		request.getSession().setAttribute(tokenSecretKey, oAuthTokenSecret);

		String approvalPageUrl = oauthHelper
				.createUserAuthorizationUrl(oauthParameters);

		return "redirect:" + approvalPageUrl;
	}

	protected String authorizeToken(HttpServletRequest request, String tokenSecretKey,
			Connector api, Map<String,String> extraParameters) throws OAuthException {
		GoogleOAuthParameters oauthParameters = new GoogleOAuthParameters();
		oauthParameters.setOAuthConsumerKey(getConsumerKey());
		oauthParameters.setOAuthConsumerSecret(getConsumerSecret());
		if (extraParameters!=null) {
			for (Entry<String,String> extraParameter : extraParameters.entrySet())
				oauthParameters.addExtraParameter(extraParameter.getKey(),
						extraParameter.getValue());
		}
		String tokenSecret = (String) request.getSession().getAttribute(
				tokenSecretKey);
		oauthParameters.setOAuthTokenSecret(tokenSecret);

		GoogleOAuthHelper oauthHelper = getOAuthHelper();
		oauthHelper.getOAuthParametersFromCallback(request.getQueryString(),
				oauthParameters);

		String accessToken = null;
		accessToken = oauthHelper.getAccessToken(oauthParameters);

		String accessTokenSecret = oauthParameters.getOAuthTokenSecret();

		Guest guest = ControllerHelper.getGuest();

		guestService().setApiKeyAttribute(guest.getId(), api, "accessToken", accessToken);
		guestService().setApiKeyAttribute(guest.getId(), api, "tokenSecret", accessTokenSecret);

		return "redirect:/home/from/"+api.getName();
	}

	protected abstract String getConsumerKey();

	protected abstract String getConsumerSecret();

}
