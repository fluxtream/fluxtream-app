package org.fluxtream.connectors.beddit;

import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.JPADaoService;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by candide on 09/02/15.
 */
@Controller
@RequestMapping(value = "/beddit")
public class BedditOAuth2Controller {

    private static final String BEDDIT_SERVICE = "bedditService";
    private static final String BEDDIT_REQUEST_TOKEN = "bedditRequestToken";
    private static final String BEDDIT_RENEWTOKEN_APIKEYID = "beddit.renewtoken.apiKeyId";

    @Autowired
    Configuration env;

    @Autowired
    GuestService guestService;

    @Autowired
    JPADaoService jpaDaoService;

    @RequestMapping(value = "/token")
    public String getBedditToken(HttpServletRequest request) throws IOException, ServletException {
        OAuthService service = new ServiceBuilder()
                .provider(BedditApi.class)
                .apiKey(env.get("bedditConsumerKey"))
                .apiSecret(env.get("bedditConsumerSecret"))
                .callback(env.get("homeBaseUrl") + "beddit/upgradeToken")
                .build();
        request.getSession().setAttribute(BEDDIT_SERVICE, service);

        // Obtain the Authorization URL
        Token requestToken = service.getRequestToken();
        request.getSession().setAttribute(BEDDIT_REQUEST_TOKEN, requestToken);
        String authorizationUrl = service.getAuthorizationUrl(requestToken);
        final String apiKeyIdParameter = request.getParameter("apiKeyId");
        if (apiKeyIdParameter!=null)
            request.getSession().setAttribute(BEDDIT_RENEWTOKEN_APIKEYID, apiKeyIdParameter);

        return "redirect:" + authorizationUrl;
    }

    @RequestMapping(value = "/upgradeToken")
    public String upgradeToken(HttpServletRequest request) throws IOException {
        final String code = request.getParameter("oauth_verifier");
        Verifier verifier = new Verifier(code);
        OAuthService service = (OAuthService)request.getSession().getAttribute(BEDDIT_SERVICE);

        Token requestToken = (Token)request.getSession().getAttribute(BEDDIT_REQUEST_TOKEN);
        Token accessToken = service.getAccessToken(requestToken, verifier);

        final String token = accessToken.getToken();

        Guest guest = AuthHelper.getGuest();

        final Connector connector = Connector.getConnector("beddit");

        ApiKey apiKey;
        if (request.getSession().getAttribute(BEDDIT_RENEWTOKEN_APIKEYID)!=null) {
            final String apiKeyIdString = (String) request.getSession().getAttribute(BEDDIT_RENEWTOKEN_APIKEYID);
            long apiKeyId = Long.valueOf(apiKeyIdString);
            apiKey = guestService.getApiKey(apiKeyId);
        } else
            apiKey = guestService.createApiKey(guest.getId(), connector);

        guestService.populateApiKey(apiKey.getId());
        guestService.setApiKeyAttribute(apiKey, "accessToken", token);

        request.getSession().removeAttribute(BEDDIT_REQUEST_TOKEN);
        request.getSession().removeAttribute(BEDDIT_SERVICE);
        if (request.getSession().getAttribute(BEDDIT_RENEWTOKEN_APIKEYID)!=null) {
            request.getSession().removeAttribute(BEDDIT_RENEWTOKEN_APIKEYID);
            return "redirect:/app/tokenRenewed/beddit";
        }
        return "redirect:/app/from/beddit";
    }

}
