package com.fluxtream.connectors.evernote;

import java.io.IOException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fluxtream.Configuration;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.JPADaoService;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.EvernoteApi;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Controller
@RequestMapping(value = "/evernote")
public class EvernoteController {

    private static final String EVERNOTE_SERVICE = "evernoteService";
    private static final String EVERNOTE_REQUEST_TOKEN = "evernoteRequestToken";

    @Autowired
    Configuration env;

    @Autowired
    GuestService guestService;

    @Autowired
    JPADaoService jpaDaoService;

    @PersistenceContext
    EntityManager em;

    @RequestMapping(value = "/token")
    public String getEvernoteToken(HttpServletRequest request) throws IOException, ServletException {
        OAuthService service = new ServiceBuilder()
                .provider(EvernoteApi.Sandbox.class)
                .apiKey(getConsumerKey())
                .apiSecret(getConsumerSecret())
                .callback(env.get("homeBaseUrl") + "evernote/upgradeToken")
                .build();
        request.getSession().setAttribute(EVERNOTE_SERVICE, service);

        // Obtain the Authorization URL
        Token requestToken = service.getRequestToken();
        request.getSession().setAttribute(EVERNOTE_REQUEST_TOKEN, requestToken);
        String authorizationUrl = service.getAuthorizationUrl(requestToken);

        return "redirect:" + authorizationUrl;
    }

    @RequestMapping(value = "/upgradeToken")
    public String upgradeToken(HttpServletRequest request) throws IOException {
        final String code = request.getParameter("oauth_verifier");
        Verifier verifier = new Verifier(code);
        OAuthService service = (OAuthService)request.getSession().getAttribute(EVERNOTE_SERVICE);

        Token requestToken = (Token)request.getSession().getAttribute(EVERNOTE_REQUEST_TOKEN);
        Token accessToken = service.getAccessToken(requestToken, verifier);

        final String token = accessToken.getToken();
        final String secret = accessToken.getSecret();

        Guest guest = AuthHelper.getGuest();

        final Connector connector = Connector.getConnector("evernote");
        final ApiKey apiKey = guestService.createApiKey(guest.getId(), connector);

        guestService.setApiKeyAttribute(apiKey, "accessToken", token);
        guestService.setApiKeyAttribute(apiKey, "tokenSecret", secret);

        request.getSession().removeAttribute(EVERNOTE_REQUEST_TOKEN);
        request.getSession().removeAttribute(EVERNOTE_SERVICE);
        return "redirect:/app/from/evernote";
    }

    String getConsumerKey() {
        return env.get("evernoteConsumerKey");
    }

    String getConsumerSecret() {
        return env.get("evernoteConsumerSecret");
    }

    @RequestMapping(value="/res/{guid}")
    public void getResource(@PathVariable("guid") String guid,
                            HttpServletResponse response) throws IOException {
        final Query nativeQuery = em.createNativeQuery(String.format("SELECT mime, dataBody FROM Facet_EvernoteResource WHERE guid='%s'", guid));
        final Object[] singleResult = (Object[])nativeQuery.getSingleResult();
        response.setContentType((String)singleResult[0]);
        byte[] resourceData = (byte[])singleResult[1];
        response.getOutputStream().write(resourceData, 0, resourceData.length);
    }


}
