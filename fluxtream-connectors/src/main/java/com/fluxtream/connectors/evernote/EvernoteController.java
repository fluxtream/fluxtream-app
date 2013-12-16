package com.fluxtream.connectors.evernote;

import java.io.ByteArrayInputStream;
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
import net.coobird.thumbnailator.Thumbnailator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.EvernoteApi;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Controller
@RequestMapping(value = "/evernote")
public class EvernoteController {

    public static final String EVERNOTE_SANDBOX_KEY = "evernote.sandbox";
    private static final String EVERNOTE_SERVICE = "evernoteService";
    private static final String EVERNOTE_REQUEST_TOKEN = "evernoteRequestToken";
    private static final String EVERNOTE_RENEWTOKEN_APIKEYID = "evernote.renewtoken.apiKeyId";
    private static final short MAX_WIDTH = 600;

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
        final Boolean sandbox = Boolean.valueOf(env.get(EVERNOTE_SANDBOX_KEY));
        OAuthService service = new ServiceBuilder()
                .provider(sandbox?EvernoteApi.Sandbox.class:EvernoteApi.class)
                .apiKey(getConsumerKey())
                .apiSecret(getConsumerSecret())
                .callback(env.get("homeBaseUrl") + "evernote/upgradeToken")
                .build();
        request.getSession().setAttribute(EVERNOTE_SERVICE, service);

        // Obtain the Authorization URL
        Token requestToken = service.getRequestToken();
        request.getSession().setAttribute(EVERNOTE_REQUEST_TOKEN, requestToken);
        String authorizationUrl = service.getAuthorizationUrl(requestToken);
        final String apiKeyIdParameter = request.getParameter("apiKeyId");
        if (apiKeyIdParameter!=null)
            request.getSession().setAttribute(EVERNOTE_RENEWTOKEN_APIKEYID, apiKeyIdParameter);

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

        ApiKey apiKey;
        if (request.getSession().getAttribute(EVERNOTE_RENEWTOKEN_APIKEYID)!=null) {
            final String apiKeyIdString = (String) request.getSession().getAttribute(EVERNOTE_RENEWTOKEN_APIKEYID);
            long apiKeyId = Long.valueOf(apiKeyIdString);
            apiKey = guestService.getApiKey(apiKeyId);
        } else
            apiKey = guestService.createApiKey(guest.getId(), connector);

        guestService.populateApiKey(apiKey.getId());
        guestService.setApiKeyAttribute(apiKey, "accessToken", token);

        request.getSession().removeAttribute(EVERNOTE_REQUEST_TOKEN);
        request.getSession().removeAttribute(EVERNOTE_SERVICE);
        if (request.getSession().getAttribute(EVERNOTE_RENEWTOKEN_APIKEYID)!=null) {
            request.getSession().removeAttribute(EVERNOTE_RENEWTOKEN_APIKEYID);
            return "redirect:/app/tokenRenewed/evernote";
        }
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
        final Query nativeQuery = em.createNativeQuery(String.format("SELECT mime, dataBody, width FROM Facet_EvernoteResource WHERE guid='%s'", guid));
        final Object[] singleResult = (Object[])nativeQuery.getSingleResult();
        final String mimeType = (String)singleResult[0];
        response.setContentType(mimeType);
        byte[] resourceData = (byte[])singleResult[1];
        if (mimeType.indexOf("image")!=-1) {
            short width = (Short) singleResult[2];
            if (width>MAX_WIDTH) {
                Thumbnailator.createThumbnail(new ByteArrayInputStream(resourceData),
                                              response.getOutputStream(), MAX_WIDTH,
                                              Integer.MAX_VALUE);
                return;
            }
        }
        response.getOutputStream().write(resourceData, 0, resourceData.length);
    }

    @RequestMapping(value="/content/{guid}")
    public ModelAndView getContent(@PathVariable("guid") String guid,
                            HttpServletResponse response) throws IOException{
        ModelAndView mav = new ModelAndView("connectors/evernote/content");
        final Query nativeQuery = em.createNativeQuery(String.format("SELECT htmlContent FROM Facet_EvernoteNote WHERE guid='%s'", guid));
        String content = (String)nativeQuery.getSingleResult();
        content = removeImageSizeAttributes(content);
        response.setContentType("text/html; charset=utf-8");
        mav.addObject("content", content);
        mav.addObject("guid", guid);
        return mav;
    }

    private String removeImageSizeAttributes(String html) {
        Document doc = Jsoup.parse(html);
        Elements e = doc.getElementsByTag("img");
        for (Element element : e) {
            final String width = element.attr("width");
            if (width!=null&&Integer.valueOf(width)>MAX_WIDTH) {
                element.attr("width", String.valueOf(MAX_WIDTH));
                final String height = element.attr("height");
                if (height !=null) {
                    float w = Float.valueOf(width);
                    float h = Float.valueOf(height);
                    float r = Float.valueOf(MAX_WIDTH)/w;
                    element.attr("height", String.valueOf((int)(h*r)));
                }
            }
        }
        return doc.html();
    }

}
