package com.fluxtream.connectors.withings;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fluxtream.Configuration;
import com.fluxtream.connectors.SignpostOAuthHelper;
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
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
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

        String oauthCallback = env.get("homeBaseUrl") + "fitbit/upgradeToken";
        if (request.getParameter("guestId") != null)
            oauthCallback += "?guestId=" + request.getParameter("guestId");

        String consumerKey = env.get("fitbitConsumerKey");
        String consumerSecret = env.get("fitbitConsumerSecret");

        OAuthConsumer consumer = new DefaultOAuthConsumer(consumerKey,
                                                          consumerSecret);

        OAuthProvider provider = new DefaultOAuthProvider(
                "http://api.fitbit.com/oauth/request_token",
                "http://api.fitbit.com/oauth/access_token",
                "http://api.fitbit.com/oauth/authorize");

        request.getSession().setAttribute(WITHINGS_OAUTH_CONSUMER, consumer);
        request.getSession().setAttribute(WITHINGS_OAUTH_PROVIDER, provider);
        System.out.println("the token secret is: " + consumer.getTokenSecret());

        String approvalPageUrl = provider.retrieveRequestToken(consumer,
                                                               oauthCallback);

        return "redirect:" + approvalPageUrl;
    }

}
