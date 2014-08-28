package org.fluxtream.connectors.bodymedia;

import java.io.IOException;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.connectors.controllers.ControllerSupport;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.Notification;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.NotificationsService;
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

    @Autowired
    ConnectorUpdateService connectorUpdateService;

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
            notificationsService.addNamedNotification(AuthHelper.getGuestId(),
                                                      Notification.Type.ERROR, connector().statusNotificationName(),
                                                      "Oops. There was an error with the BodyMedia API. " +
                                                      "Hang tight, we are working on it.");
            // TODO: Should we record permanent failure since an existing connector won't work again until
            // it is reauthenticated?  We would need to get hold of the apiKey and do:
            //  guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, null);

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

        // Store the OAuth server keys used for the token upgrade, which are the ones currently in oauth.properties
        // into ApiKeyAttributes.  The values in oauth.properties may change over time, so we need to preserve
        // the values used for the token creation with the ApiKey itself.
        guestService.setApiKeyAttribute(apiKey, "bodymediaConsumerKey", env.get("bodymediaConsumerKey"));
        guestService.setApiKeyAttribute(apiKey, "bodymediaConsumerSecret", env.get("bodymediaConsumerSecret"));

        // If bodymediaRateDelayMs is specified in oauth.properties store it with the attributes
        // This should be 1000/Calls per second for the associated key.  The default is 2 calls per second,
        // which is bodymediaRateDelayMs=500
        String bodymediaRateDelayMs = env.get("bodymediaRateDelayMs");

        if(bodymediaRateDelayMs==null) {
            bodymediaRateDelayMs="500";
        }
        guestService.setApiKeyAttribute(apiKey, "bodymediaRateDelayMs", bodymediaRateDelayMs);

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
                                                           OAuthCommunicationException, OAuthNotAuthorizedException,
                                                           UpdateFailedException {
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

             // Notify the user that the tokens need to be manually renewed
            notificationsService.addNamedNotification(updateInfo.getGuestId(), Notification.Type.WARNING, connector().statusNotificationName(),
                                                      "Heads Up. This server cannot automatically refresh your authentication tokens.<br>" +
                                                      "Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
                                                      "scroll to the BodyMedia connector, and renew your tokens (look for the <i class=\"icon-resize-small icon-large\"></i> icon)");

            // Record permanent failure since this connector won't work again until
            // it is reauthenticated
            guestService.setApiKeyStatus(updateInfo.apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, null, ApiKey.PermanentFailReason.NEEDS_REAUTH);
            throw new UpdateFailedException("requires token reauthorization",true, ApiKey.PermanentFailReason.NEEDS_REAUTH);
        }

        // We're not on a mirrored test server.  Try to swap the expired
        // access token for a fresh one.

        // First, retrieve the OAuth server keys used when this key was created.  These are automatically stored
        // in the ApiKeyAttribute table at the time of creation based on the values present in
        // oauth.properties.
        String bodymediaConsumerKey = guestService.getApiKeyAttribute(updateInfo.apiKey, "bodymediaConsumerKey");
        String bodymediaConsumerSecret = guestService.getApiKeyAttribute(updateInfo.apiKey, "bodymediaConsumerSecret");

        OAuthConsumer consumer = new DefaultOAuthConsumer(
                bodymediaConsumerKey,
                bodymediaConsumerSecret);

        String accessToken = guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken");
        consumer.setTokenWithSecret(accessToken,
                guestService.getApiKeyAttribute(updateInfo.apiKey, "tokenSecret"));
        HttpParameters additionalParameter = new HttpParameters();
        additionalParameter.put("api_key", bodymediaConsumerKey);
        additionalParameter.put("oauth_token",
                                accessToken);
        consumer.setAdditionalParameters(additionalParameter);

        HttpClient httpClient = env.getHttpClient();

        OAuthProvider provider = new CommonsHttpOAuthProvider(
                "https://api.bodymedia.com/oauth/request_token?api_key="+bodymediaConsumerKey,
                "https://api.bodymedia.com/oauth/access_token?api_key="+bodymediaConsumerKey,
                "https://api.bodymedia.com/oauth/authorize?api_key="+bodymediaConsumerKey, httpClient);

        try {
            provider.retrieveAccessToken(consumer, null);

            guestService.setApiKeyAttribute(updateInfo.apiKey,
                                            "accessToken", consumer.getToken());
            guestService.setApiKeyAttribute(updateInfo.apiKey,
                                            "tokenSecret", consumer.getTokenSecret());
            guestService.setApiKeyAttribute(updateInfo.apiKey,
                                            "tokenExpiration", provider.getResponseParameters().get("xoauth_token_expiration_time").first());

            // Record this connector as having status up
            guestService.setApiKeyStatus(updateInfo.apiKey.getId(), ApiKey.Status.STATUS_UP, null, null);
            // Schedule an update for this connector
            connectorUpdateService.updateConnector(updateInfo.apiKey, false);

        } catch (Throwable t) {
            // Notify the user that the tokens need to be manually renewed
            notificationsService.addNamedNotification(updateInfo.getGuestId(), Notification.Type.WARNING, connector().statusNotificationName(),
                                                      "Heads Up. We failed in our attempt to automatically refresh your authentication tokens.<br>" +
                                                      "Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
                                                      "scroll to the BodyMedia connector, and renew your tokens (look for the <i class=\"icon-resize-small icon-large\"></i> icon)");
            // Record permanent failure since this connector won't work again until
            // it is reauthenticated
            guestService.setApiKeyStatus(updateInfo.apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, null, ApiKey.PermanentFailReason.NEEDS_REAUTH);
            throw new UpdateFailedException("refresh token attempt failed", t, true, ApiKey.PermanentFailReason.NEEDS_REAUTH);
        }
    }
}
