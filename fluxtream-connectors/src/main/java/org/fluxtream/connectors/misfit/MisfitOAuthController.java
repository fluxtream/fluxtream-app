package org.fluxtream.connectors.misfit;

import org.fluxtream.connectors.controllers.ControllerSupport;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.GuestService;
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
@RequestMapping(value = "/misfit")
public class MisfitOAuthController {

    private static final String MISFIT_SERVICE = "misfitService";
    private static final Token EMPTY_TOKEN = null;

    @Autowired
    Configuration env;

    @Autowired
    GuestService guestService;

    @RequestMapping(value = "/token")
    public String getMisfitToken(HttpServletRequest request) throws IOException, ServletException {

        OAuthService service = getOAuthService(request);
        request.getSession().setAttribute(MISFIT_SERVICE, service);

        // Obtain the Authorization URL
        String authorizationUrl = service.getAuthorizationUrl(EMPTY_TOKEN);
        authorizationUrl += authorizationUrl.indexOf("?")!=-1
                ? "&scope=public,birthday,email"
                : "?scope=public,birthday,email";
        if (request.getParameter("apiKeyId") != null)
            authorizationUrl += authorizationUrl.indexOf("?")!=-1
                    ? "&state=" + request.getParameter("apiKeyId")
                    : "?state=" + request.getParameter("apiKeyId");

        return "redirect:" + authorizationUrl;
    }

    public OAuthService getOAuthService(HttpServletRequest request) {
        return new ServiceBuilder()
                .provider(MisfitApi.class)
                .apiKey(getConsumerKey())
                .apiSecret(getConsumerSecret())
                .callback(ControllerSupport.getLocationBase(request, env) + "misfit/upgradeToken")
                .build();
    }

    @RequestMapping(value = "/upgradeToken")
    public String upgradeToken(HttpServletRequest request) throws IOException {
        final String code = request.getParameter("code");
        Verifier verifier = new Verifier(code);
        OAuthService service = (OAuthService)request.getSession().getAttribute(MISFIT_SERVICE);

        Token accessToken = service.getAccessToken(EMPTY_TOKEN, verifier);
        final String token = accessToken.getToken();

        Guest guest = AuthHelper.getGuest();
        final Connector connector = Connector.getConnector("misfit");
        ApiKey apiKey;
        if (request.getParameter("state")!=null) {
            long apiKeyId = Long.valueOf(request.getParameter("state"));
            apiKey = guestService.getApiKey(apiKeyId);
        } else
            apiKey = guestService.createApiKey(guest.getId(), connector);

        guestService.populateApiKey(apiKey.getId());
        guestService.setApiKeyAttribute(apiKey, "accessToken", token);
        request.getSession().removeAttribute(MISFIT_SERVICE);
        if (request.getParameter("state")!=null)
            return "redirect:/app/tokenRenewed/misfit";
        return "redirect:/app/from/misfit";
    }

    String getConsumerKey() {
        return env.get("misfitConsumerKey");
    }

    String getConsumerSecret() {
        return env.get("misfitConsumerSecret");
    }


}
