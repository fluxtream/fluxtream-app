package com.fluxtream.connectors.withings;

import java.io.IOException;
import java.util.AbstractMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import com.fluxtream.Configuration;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
import com.fluxtream.services.GuestService;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.WithingsApi;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Controller()
@RequestMapping("/withings")
public class WithingsOAuthController {

    private static final String WITHINGS_SERVICE = "withingsService";
    private static final String WITHINGS_REQUEST_TOKEN_KEY = "withingsRequestToken";

    @Autowired
    Configuration env;

    @Autowired
    GuestService guestService;

    @RequestMapping(value = "/token")
    public String getWithingsToken(HttpServletRequest request) throws IOException, ServletException {

        OAuthService service = getOAuthService();
        request.getSession().setAttribute(WITHINGS_SERVICE, service);
        Token requestToken = service.getRequestToken();

        request.getSession().setAttribute(WITHINGS_REQUEST_TOKEN_KEY, requestToken);
        // Obtain the Authorization URL
        String authorizationUrl = service.getAuthorizationUrl(requestToken);

        return "redirect:" + authorizationUrl;
    }

    public OAuthService getOAuthService() {
        return new ServiceBuilder()
                .provider(WithingsApi.class)
                .apiKey(getConsumerKey())
                .apiSecret(getConsumerSecret())
                .callback(env.get("homeBaseUrl") + "withings/upgradeToken")
                .build();
    }

    @RequestMapping(value = "/upgradeToken")
    public String upgradeToken(HttpServletRequest request) throws IOException {
        final String code = request.getParameter("oauth_verifier");
        Verifier verifier = new Verifier(code);
        final String userid = request.getParameter("userid");
        OAuthService service = (OAuthService)request.getSession().getAttribute(WITHINGS_SERVICE);
        Token requestToken = (Token)request.getSession().getAttribute(WITHINGS_REQUEST_TOKEN_KEY);

        AbstractMap.SimpleEntry<String,String> useridParameter = new AbstractMap.SimpleEntry<String, String>("userid", userid);
        Token accessToken = service.getAccessToken(requestToken, verifier, useridParameter);

        final String token = accessToken.getToken();
        final String secret = accessToken.getSecret();

        Guest guest = AuthHelper.getGuest();
        final ApiKey apiKey = guestService.createApiKey(guest.getId(), Connector.getConnector("withings"));

        guestService.setApiKeyAttribute(apiKey, "accessToken", token);
        guestService.setApiKeyAttribute(apiKey, "tokenSecret", secret);
        guestService.setApiKeyAttribute(apiKey, "userid", userid);

        request.getSession().removeAttribute(WITHINGS_SERVICE);
        request.getSession().removeAttribute(WITHINGS_REQUEST_TOKEN_KEY);
        return "redirect:/app/from/withings";
    }

    String getConsumerKey() {
        return env.get("withingsConsumerKey");
    }

    String getConsumerSecret() {
        return env.get("withingsConsumerSecret");
    }
}
