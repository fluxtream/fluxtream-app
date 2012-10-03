package com.fluxtream.connectors.bodymedia;

import java.io.IOException;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.Guest;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.services.GuestService;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.http.HttpParameters;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/bodymedia")
public class BodymediaController {

	@Autowired
	GuestService guestService;

	@Autowired
	Configuration env;

	private static final String BODYMEDIA_OAUTH_CONSUMER = "bodymediaOAuthConsumer";
	private static final String BODYMEDIA_OAUTH_PROVIDER = "bodymediaOAuthProvider";

	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request) throws IOException, ServletException,
			OAuthMessageSignerException, OAuthNotAuthorizedException,
			OAuthExpectationFailedException, OAuthCommunicationException {

		String oauthCallback = env.get("homeBaseUrl") + "bodymedia/upgradeToken";
		if (request.getParameter("guestId") != null)
			oauthCallback += "?guestId=" + request.getParameter("guestId");

		String apiKey = env.get("bodymediaConsumerKey");
		OAuthConsumer consumer = new DefaultOAuthConsumer(
				apiKey,
				env.get("bodymediaConsumerSecret"));
		HttpParameters additionalParameter = new HttpParameters();
		additionalParameter.put("api_key", apiKey);
		consumer.setAdditionalParameters(additionalParameter);

		HttpClient httpClient = env.getHttpClient();

		OAuthProvider provider = new CommonsHttpOAuthProvider(
				"https://api.bodymedia.com/oauth/request_token?api_key="+apiKey,
				"https://api.bodymedia.com/oauth/access_token?api_key="+apiKey,
				"https://api.bodymedia.com/oauth/authorize?api_key="+apiKey, httpClient);

		request.getSession().setAttribute(BODYMEDIA_OAUTH_CONSUMER, consumer);
		request.getSession().setAttribute(BODYMEDIA_OAUTH_PROVIDER, provider);

		String approvalPageUrl = provider.retrieveRequestToken(consumer,
				oauthCallback);
		
		System.out.println("the token secret is: " + consumer.getTokenSecret());
		approvalPageUrl+="&oauth_api=" + apiKey;
		approvalPageUrl = URLDecoder.decode(approvalPageUrl, "UTF-8");

		return "redirect:" + approvalPageUrl;
	}

	@RequestMapping(value = "/upgradeToken")
	public String upgradeToken(HttpServletRequest request) throws NoSuchAlgorithmException, IOException, OAuthMessageSignerException,
			OAuthNotAuthorizedException, OAuthExpectationFailedException,
			OAuthCommunicationException {

		OAuthConsumer consumer = (OAuthConsumer) request.getSession()
				.getAttribute(BODYMEDIA_OAUTH_CONSUMER);
		OAuthProvider provider = (OAuthProvider) request.getSession()
				.getAttribute(BODYMEDIA_OAUTH_PROVIDER);
		String verifier = request.getParameter("oauth_verifier");
		provider.retrieveAccessToken(consumer, verifier);
		Guest guest = AuthHelper.getGuest();
		
		guestService.setApiKeyAttribute(guest.getId(), connector(),
				"api_key", env.get("bodymediaConsumerKey"));
		guestService.setApiKeyAttribute(guest.getId(), connector(),
				"accessToken", consumer.getToken());
		guestService.setApiKeyAttribute(guest.getId(), connector(),
				"tokenSecret", consumer.getTokenSecret());
        guestService.setApiKeyAttribute(guest.getId(), connector(),
                "tokenExpiration", provider.getResponseParameters().get("xoauth_token_expiration_time").first());

		return "redirect:/app/from/" + connector().getName();
	}

	private Connector connector() {
		return Connector.getConnector("bodymedia");
	}

    public void replaceToken(UpdateInfo updateInfo) throws OAuthExpectationFailedException, OAuthMessageSignerException,
                                                           OAuthCommunicationException, OAuthNotAuthorizedException {
        String apiKey = guestService.getApiKeyAttribute(updateInfo.getGuestId(), connector(), "api_key");
        OAuthConsumer consumer = new DefaultOAuthConsumer(
                apiKey,
                env.get("bodymediaConsumerSecret"));
        String accessToken = guestService.getApiKeyAttribute(updateInfo.getGuestId(), connector(), "accessToken");
        consumer.setTokenWithSecret(accessToken,
                guestService.getApiKeyAttribute(updateInfo.getGuestId(), connector(), "tokenSecret"));
        HttpParameters additionalParameter = new HttpParameters();
        additionalParameter.put("api_key", apiKey);
        additionalParameter.put("oauth_token",
                                accessToken);
        consumer.setAdditionalParameters(additionalParameter);

        HttpClient httpClient = env.getHttpClient();

        OAuthProvider provider = new CommonsHttpOAuthProvider(
                "https://api.bodymedia.com/oauth/request_token?api_key="+apiKey,
                "https://api.bodymedia.com/oauth/access_token?api_key="+apiKey,
                "https://api.bodymedia.com/oauth/authorize?api_key="+apiKey, httpClient);

        provider.retrieveAccessToken(consumer, null);

        guestService.setApiKeyAttribute(updateInfo.getGuestId(), connector(),
                                        "api_key", env.get("bodymediaConsumerKey"));
        guestService.setApiKeyAttribute(updateInfo.getGuestId(), connector(),
                                        "accessToken", consumer.getToken());
        guestService.setApiKeyAttribute(updateInfo.getGuestId(), connector(),
                                        "tokenSecret", consumer.getTokenSecret());
        guestService.setApiKeyAttribute(updateInfo.getGuestId(), connector(),
                                        "tokenExpiration", provider.getResponseParameters().get("xoauth_token_expiration_time").first());
    }
}
