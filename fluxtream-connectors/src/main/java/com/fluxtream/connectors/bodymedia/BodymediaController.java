package com.fluxtream.connectors.bodymedia;

import java.io.IOException;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import com.fluxtream.Configuration;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.controllers.ControllerSupport;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.Notification;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.NotificationsService;
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
import org.apache.log4j.Logger;
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

    @Autowired
    NotificationsService notificationsService;

    static final Logger logger = Logger.getLogger(BodymediaController.class);

	private static final String BODYMEDIA_OAUTH_CONSUMER = "bodymediaOAuthConsumer";
	private static final String BODYMEDIA_OAUTH_PROVIDER = "bodymediaOAuthProvider";

	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request) throws IOException, ServletException,
			OAuthMessageSignerException, OAuthNotAuthorizedException,
			OAuthExpectationFailedException, OAuthCommunicationException {

		String oauthCallback = ControllerSupport.getLocationBase(request, env) + "bodymedia/upgradeToken";
		if (request.getParameter("guestId") != null)
			oauthCallback += "?guestId=" + request.getParameter("guestId");
        if (request.getParameter("apiKeyId") != null)
            oauthCallback += "?apiKeyId=" + request.getParameter("apiKeyId");

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

        String approvalPageUrl = null;
        try {
            approvalPageUrl = provider.retrieveRequestToken(consumer,
                    oauthCallback);
        } catch (Throwable t) {
            logger.error("Couldn't retrieve BodyMedia request token.");
            t.printStackTrace();
            notificationsService.addNotification(AuthHelper.getGuestId(),
                                                 Notification.Type.ERROR,
                                                 "Oops. There was an error with the BodyMedia API. " +
                                                 "Hang tight, we are working on it.");
            return "redirect:/app/";
        }
		
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

        ApiKey apiKey;
        if (request.getParameter("apiKeyId")!=null) {
            long apiKeyId = Long.valueOf(request.getParameter("apiKeyId"));
            apiKey = guestService.getApiKey(apiKeyId);
        } else
            apiKey = guestService.createApiKey(guest.getId(), connector());

        guestService.setApiKeyAttribute(apiKey, "api_key", env.get("bodymediaConsumerKey"));
		guestService.setApiKeyAttribute(apiKey,
				"accessToken", consumer.getToken());
		guestService.setApiKeyAttribute(apiKey,
				"tokenSecret", consumer.getTokenSecret());
        guestService.setApiKeyAttribute(apiKey,
                "tokenExpiration", provider.getResponseParameters().get("xoauth_token_expiration_time").first());

        request.getSession().removeAttribute(BODYMEDIA_OAUTH_CONSUMER);
        request.getSession().removeAttribute(BODYMEDIA_OAUTH_PROVIDER);

        if (request.getParameter("apiKeyId")!=null)
    		return "redirect:/app/tokenRenewed/" + connector().getName();
        else
            return "redirect:/app/from/" + connector().getName();
	}

	private Connector connector() {
		return Connector.getConnector("bodymedia");
	}

    public void replaceToken(UpdateInfo updateInfo) throws OAuthExpectationFailedException, OAuthMessageSignerException,
                                                           OAuthCommunicationException, OAuthNotAuthorizedException {
        // Check to see if we are running on a mirrored test instance
        // and should therefore refrain from swapping tokens lest we
        // invalidate an existing token instance
        String disableTokenSwap = env.get("disableTokenSwap");
        if(disableTokenSwap!=null && disableTokenSwap.equals("true")) {
            String msg = "**** Skipping refreshToken for bodymedia connector instance because disableTokenSwap is set on this server";
                                            ;
            StringBuilder sb2 = new StringBuilder("module=BodymediaController component=BodymediaController action=replaceToken apiKeyId=" + updateInfo.apiKey.getId())
            			    .append(" message=\"").append(msg).append("\"");
            logger.info(sb2.toString());
            System.out.println(msg);
            return;
        }

        // We're not on a mirrored test server.  Try to swap the expired
        // access token for a fresh one.
        String apiKey = guestService.getApiKeyAttribute(updateInfo.apiKey, "api_key");

        // The api_key attribute will be the same as the env.get("bodymediaConsumerKey"), so
        // in the case where apiKey is null default to the value from properties
        if(apiKey==null) {
            apiKey = env.get("bodymediaConsumerKey");
        }

        // TODO: If apiKey is still null, we should report to the user since it means this instance isn't
        // properly configured with OAuth host keys for BodyMedia
        OAuthConsumer consumer = new DefaultOAuthConsumer(
                apiKey,
                env.get("bodymediaConsumerSecret"));
        String accessToken = guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken");
        consumer.setTokenWithSecret(accessToken,
                guestService.getApiKeyAttribute(updateInfo.apiKey, "tokenSecret"));
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

        guestService.setApiKeyAttribute(updateInfo.apiKey,
                                        "api_key", env.get("bodymediaConsumerKey"));
        guestService.setApiKeyAttribute(updateInfo.apiKey,
                                        "accessToken", consumer.getToken());
        guestService.setApiKeyAttribute(updateInfo.apiKey,
                                        "tokenSecret", consumer.getTokenSecret());
        guestService.setApiKeyAttribute(updateInfo.apiKey,
                                        "tokenExpiration", provider.getResponseParameters().get("xoauth_token_expiration_time").first());
    }
}
