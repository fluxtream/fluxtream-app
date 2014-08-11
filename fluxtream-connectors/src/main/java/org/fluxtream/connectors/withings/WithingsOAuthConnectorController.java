package org.fluxtream.connectors.withings;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.SignpostOAuthHelper;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.GuestService;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.http.HttpParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Controller()
@RequestMapping(value="/withings")
public class WithingsOAuthConnectorController {

    private static final String WITHINGS_RENEWTOKEN_APIKEYID = "withings.renewtoken.apiKeyId";
    static final String HAS_UPGRADED_TO_OAUTH = "HAS_UPGRADED_TO_OAUTH";

    @Autowired
    GuestService guestService;

    @Autowired
    ApiDataService apiDataService;

    @Autowired
    SignpostOAuthHelper signpostHelper;

    @Autowired
    Configuration env;
    private static final String WITHINGS_OAUTH_CONSUMER = "withingsOAuthConsumer";
    private static final String WITHINGS_OAUTH_PROVIDER = "withingsOAuthProvider";

    @RequestMapping(value = "/token")
    public String getToken(HttpServletRequest request) throws IOException, ServletException, OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {

        String oauthCallback = env.get("homeBaseUrl") + "withings/upgradeToken";
        if (request.getParameter("guestId") != null)
            oauthCallback += "?guestId=" + request.getParameter("guestId");

        String consumerKey = env.get("withingsConsumerKey");
        String consumerSecret = env.get("withingsConsumerSecret");

        OAuthConsumer consumer = new DefaultOAuthConsumer(consumerKey,
                                                          consumerSecret);

        OAuthProvider provider = new DefaultOAuthProvider(
                "https://oauth.withings.com/account/request_token",
                "https://oauth.withings.com/account/access_token",
                "https://oauth.withings.com/account/authorize");

        request.getSession().setAttribute(WITHINGS_OAUTH_CONSUMER, consumer);
        request.getSession().setAttribute(WITHINGS_OAUTH_PROVIDER, provider);

        if (request.getParameter("apiKeyId")!=null)
            request.getSession().setAttribute(WITHINGS_RENEWTOKEN_APIKEYID,
                                              request.getParameter("apiKeyId"));

        String approvalPageUrl = provider.retrieveRequestToken(consumer,
                                                               oauthCallback);

        return "redirect:" + approvalPageUrl;
    }

    @RequestMapping(value = "/upgradeToken")
    public String upgradeToken(HttpServletRequest request) throws Exception {
        OAuthConsumer consumer = (OAuthConsumer) request.getSession()
                .getAttribute(WITHINGS_OAUTH_CONSUMER);

        HttpParameters additionalParameter = new HttpParameters();
        String userid = request.getParameter("userid");
        additionalParameter.put("userid", userid);
        consumer.setAdditionalParameters(additionalParameter);

        OAuthProvider provider = (OAuthProvider) request.getSession()
                .getAttribute(WITHINGS_OAUTH_PROVIDER);
        String verifier = request.getParameter("oauth_verifier");
        provider.retrieveAccessToken(consumer, verifier);

        Guest guest = AuthHelper.getGuest();

        Connector connector = Connector.getConnector("withings");

        ApiKey apiKey;
        if (request.getSession().getAttribute(WITHINGS_RENEWTOKEN_APIKEYID)!=null) {
            final String apiKeyIdString = (String) request.getSession().getAttribute(WITHINGS_RENEWTOKEN_APIKEYID);
            long apiKeyId = Long.valueOf(apiKeyIdString);
            apiKey = guestService.getApiKey(apiKeyId);
        } else {
            apiKey = guestService.createApiKey(guest.getId(), connector);
        }

        // We need to store the consumer ID and secret with the
        // apiKeyAttributes in either the case of original creation of the key
        // or token renewal.  createApiKey actually handles the former case, but
        // not the latter.  Do it in all cases here.
        guestService.setApiKeyAttribute(apiKey, "withingsConsumerKey",
                                        env.get("withingsConsumerKey"));
        guestService.setApiKeyAttribute(apiKey, "withingsConsumerSecret",
                                        env.get("withingsConsumerSecret"));

        guestService.setApiKeyAttribute(apiKey,
                                        "accessToken", consumer.getToken());
        guestService.setApiKeyAttribute(apiKey,
                                        "tokenSecret", consumer.getTokenSecret());
        guestService.setApiKeyAttribute(apiKey,
                                        "userid", userid);
        guestService.setApiKeyAttribute(apiKey,
                                        HAS_UPGRADED_TO_OAUTH, "y");

        if (request.getSession().getAttribute(WITHINGS_RENEWTOKEN_APIKEYID)!=null) {
            request.getSession().removeAttribute(WITHINGS_RENEWTOKEN_APIKEYID);
            return "redirect:/app/tokenRenewed/withings";
        }
        return "redirect:/app/from/withings";
    }


}