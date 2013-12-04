package com.fluxtream.connectors.runkeeper;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import com.fluxtream.Configuration;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.controllers.ControllerSupport;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
import com.fluxtream.services.GuestService;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.RunKeeperApi;
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
@Controller
@RequestMapping(value = "/runkeeper")
public class RunKeeperController {

    private static final String RUNKEEPER_SERVICE = "runkeeperService";

    @Autowired
    Configuration env;

    @Autowired
    GuestService guestService;

    private static final Token EMPTY_TOKEN = null;

    @RequestMapping(value = "/token")
    public String getRunkeeperToken(HttpServletRequest request) throws IOException, ServletException {

        OAuthService service = getOAuthService(request);
        request.getSession().setAttribute(RUNKEEPER_SERVICE, service);

        // Obtain the Authorization URL
        String authorizationUrl = service.getAuthorizationUrl(EMPTY_TOKEN);
        if (request.getParameter("apiKeyId") != null)
            authorizationUrl += authorizationUrl.indexOf("?")!=-1
                             ? "&state=" + request.getParameter("apiKeyId")
                             : "?state=" + request.getParameter("apiKeyId");

        return "redirect:" + authorizationUrl;
    }

    public OAuthService getOAuthService(HttpServletRequest request) {
        return new ServiceBuilder()
                .provider(RunKeeperApi.class)
                .apiKey(getConsumerKey())
                .apiSecret(getConsumerSecret())
                .callback(ControllerSupport.getLocationBase(request, env) + "runkeeper/upgradeToken")
                .build();
    }

    public OAuthService getOAuthService() {
        return new ServiceBuilder()
                    .provider(RunKeeperApi.class)
                    .apiKey(getConsumerKey())
                    .apiSecret(getConsumerSecret())
                    .callback(env.get("homeBaseUrl") + "runkeeper/upgradeToken")
                    .build();
    }

    @RequestMapping(value = "/upgradeToken")
    public String upgradeToken(HttpServletRequest request) throws IOException {
        final String code = request.getParameter("code");
        Verifier verifier = new Verifier(code);
        OAuthService service = (OAuthService)request.getSession().getAttribute(RUNKEEPER_SERVICE);

        Token accessToken = service.getAccessToken(EMPTY_TOKEN, verifier);
        final String token = accessToken.getToken();

        Guest guest = AuthHelper.getGuest();
        final Connector connector = Connector.getConnector("runkeeper");
        ApiKey apiKey;
        if (request.getParameter("state")!=null) {
            long apiKeyId = Long.valueOf(request.getParameter("state"));
            apiKey = guestService.getApiKey(apiKeyId);
        } else
            apiKey = guestService.createApiKey(guest.getId(), connector);

        guestService.populateApiKey(apiKey.getId());
        guestService.setApiKeyAttribute(apiKey, "accessToken", token);
        request.getSession().removeAttribute(RUNKEEPER_SERVICE);
        if (request.getParameter("state")!=null)
            return "redirect:/app/tokenRenewed/runkeeper";
        return "redirect:/app/from/runkeeper";
    }

    String getConsumerKey() {
        return env.get("runkeeperConsumerKey");
    }

    String getConsumerSecret() {
        return env.get("runkeeperConsumerSecret");
    }


}
