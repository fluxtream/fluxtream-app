package com.fluxtream.connectors.withings;

import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fluxtream.Configuration;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.SignpostOAuthHelper;
import com.fluxtream.domain.Guest;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.GuestService;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import org.apache.log4j.Logger;
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

    Logger logger = Logger.getLogger(WithingsOAuthConnectorController.class);

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
    public String getToken(HttpServletRequest request,
                           HttpServletResponse response) throws IOException, ServletException, OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {

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

        String approvalPageUrl = provider.retrieveRequestToken(consumer,
                                                               oauthCallback);

        return "redirect:" + approvalPageUrl;
    }

    @RequestMapping(value = "/upgradeToken")
    public String upgradeToken(HttpServletRequest request) throws Exception {
        String userid = request.getParameter("userid");

        OAuthConsumer consumer = (OAuthConsumer) request.getSession()
                .getAttribute(WITHINGS_OAUTH_CONSUMER);
        OAuthProvider provider = (OAuthProvider) request.getSession()
                .getAttribute(WITHINGS_OAUTH_PROVIDER);
        String verifier = request.getParameter("oauth_verifier");
        provider.retrieveAccessToken(consumer, verifier);
        Guest guest = AuthHelper.getGuest();

        Connector connector = Connector.getConnector("withings");

        guestService.setApiKeyAttribute(guest.getId(), connector,
                                        "accessToken", consumer.getToken());
        guestService.setApiKeyAttribute(guest.getId(), connector,
                                        "tokenSecret", consumer.getTokenSecret());
        guestService.setApiKeyAttribute(guest.getId(), connector,
                                        "userid", userid);

        return "redirect:/app/from/" + connector.getName();
    }


}
