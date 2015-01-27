package org.fluxtream.connectors.evernote;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.auth.TrustRelationshipRevokedException;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.JPADaoService;
import com.google.api.client.util.IOUtils;
import net.coobird.thumbnailator.Thumbnailator;
import org.apache.commons.lang.StringUtils;
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

    FlxLogger logger = FlxLogger.getLogger(EvernoteController.class);

    public static final String EVERNOTE_SANDBOX_KEY = "evernote.sandbox";
    private static final String EVERNOTE_SERVICE = "evernoteService";
    private static final String EVERNOTE_REQUEST_TOKEN = "evernoteRequestToken";
    private static final String EVERNOTE_RENEWTOKEN_APIKEYID = "evernote.renewtoken.apiKeyId";
    private static final short DEFAULT_MAX_WIDTH = 600;

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


    @RequestMapping(value="/res/{apiKeyId}/{guid}")
    public void getResource(@PathVariable("apiKeyId") long apiKeyId,
                            @PathVariable("guid") String rawGuid,
                            HttpServletResponse response) throws IOException, TrustRelationshipRevokedException {
        String guid = rawGuid;
        Integer maxWidth = null;
        if (rawGuid.indexOf("@")!=-1) {
            guid = rawGuid.substring(0, rawGuid.indexOf("@"));
            String formatSpecs = rawGuid.substring(guid.length());
            StringTokenizer st = new StringTokenizer(formatSpecs, "=");
            st.nextToken();
            String w = st.nextToken();
            maxWidth = Integer.valueOf(w);
        }
        // we want to reduce the size of images that are too big to be transported over http in a timely manner,
        // so here we ask the db for the mime type of the requested resource and its width so that,
        // if we are dealing with an image, we know if we need to make it smaller
        final Query nativeQuery = em.createNativeQuery(String.format("SELECT mime, width FROM Facet_EvernoteResource WHERE apiKeyId=%s AND guid='%s'",
                                                                     apiKeyId, guid));
        final Object[] singleResult = (Object[])nativeQuery.getSingleResult();
        if (singleResult==null){
            logger.warn("no Facet_EvernoteResource row found for guid=" + guid);
            response.sendError(404);
            return;
        }

        // use the resource's mimetype to set the Content-Type of the response
        final String mimeType = (String)singleResult[0];
        response.setContentType(mimeType);

        // retrieve the main data file associated with this resource
        final String devKvsLocation = env.get("btdatastore.db.location");
        if (devKvsLocation==null)
            throw new RuntimeException("No btdatastore.db.location property was specified (local.properties)");
        ApiKey apiKey = guestService.getApiKey(apiKeyId);
        File resourceFile = EvernoteUpdater.getResourceFile(apiKey.getGuestId(), apiKeyId,
                                                            guid, EvernoteUpdater.MAIN_APPENDIX, mimeType, devKvsLocation);
        if (!resourceFile.exists()) {
            logger.warn("resource file was not found for guid=" + guid);
            response.sendError(404);
            return;
        }

        // if it's an image, maybe reduce its size
        if (mimeType.indexOf("image")!=-1) {
            short width = (Short) singleResult[1];
            // if the width of the image is larger than our max, then use Thumbnailator
            // to make it smaller and directly stream the result
            // TODO: cache the resulting image
            int specifiedWidth = maxWidth!=null?maxWidth:DEFAULT_MAX_WIDTH;
            if (width>specifiedWidth) {
                Thumbnailator.createThumbnail(new FileInputStream(resourceFile),
                                              response.getOutputStream(), specifiedWidth,
                                              Integer.MAX_VALUE);
                return;
            }
        }

        // stream the file's contents directly in the response
        IOUtils.copy(new FileInputStream(resourceFile), response.getOutputStream());
    }

    @RequestMapping(value="/content/{apiKeyId}/{guid}")
    public ModelAndView getContent(@PathVariable("apiKeyId") String apiKeyId,
                                   @PathVariable("guid") String guid,
                            HttpServletResponse response) throws IOException {
        ModelAndView mav = new ModelAndView("connectors/evernote/content");
        final Query nativeQuery = em.createNativeQuery(String.format("SELECT htmlContent FROM Facet_EvernoteNote WHERE apiKeyId=%s AND guid='%s'",
                                                                     apiKeyId,
                                                                     guid));
        String content = (String)nativeQuery.getSingleResult();
        content = adjustImageSizeAttributes(content);
        response.setContentType("text/html; charset=utf-8");
        mav.addObject("content", content);
        mav.addObject("guid", guid);
        return mav;
    }

    @RequestMapping(value="/popup/{apiKeyId}/{guid}")
    public ModelAndView getPopup(@PathVariable("apiKeyId") String apiKeyId,
                                   @PathVariable("guid") String guid,
                                   HttpServletResponse response) throws IOException {
        ModelAndView mav = new ModelAndView("connectors/evernote/popup");
        final Query nativeQuery = em.createNativeQuery(String.format("SELECT title FROM Facet_EvernoteNote WHERE apiKeyId=%s AND guid='%s'",
                                                                     apiKeyId,
                                                                     guid));
        String title = (String)nativeQuery.getSingleResult();
        response.setContentType("text/html; charset=utf-8");
        mav.addObject("apiKeyId", apiKeyId);
        mav.addObject("title", title);
        mav.addObject("guid", guid);
        return mav;
    }

    /**
     * Parse the html string, detect img tags' width/height attribute and adapt them
     * to our max allowed width
     * @param html a note's html content
     * @return
     */
    private String adjustImageSizeAttributes(String html) {
        Document doc = Jsoup.parse(html);
        Elements e = doc.getElementsByTag("img");
        for (Element element : e) {
            final String width = element.attr("width");
            if (StringUtils.isNotEmpty(width)&&Integer.valueOf(width)> DEFAULT_MAX_WIDTH) {
                element.attr("width", String.valueOf(DEFAULT_MAX_WIDTH));
                final String height = element.attr("height");
                if (StringUtils.isNotEmpty(height)) {
                    float w = Float.valueOf(width);
                    float h = Float.valueOf(height);
                    float r = Float.valueOf(DEFAULT_MAX_WIDTH)/w;
                    element.attr("height", String.valueOf((int)(h*r)));
                }
            }
        }
        return doc.html();
    }

}
