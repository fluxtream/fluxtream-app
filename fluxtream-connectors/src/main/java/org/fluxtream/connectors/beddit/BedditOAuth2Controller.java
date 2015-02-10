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
import scala.xml.dtd.EMPTY;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

/**
 * Created by candide on 09/02/15.
 */
@Controller
@RequestMapping(value = "/beddit")
public class BedditOAuth2Controller {

    private static final String BEDDIT_SERVICE = "bedditService";
    private static final String BEDDIT_REQUEST_TOKEN = "bedditRequestToken";
    private static final String BEDDIT_RENEWTOKEN_APIKEYID = "beddit.renewtoken.apiKeyId";

    private static final Token EMPTY_TOKEN = null;

    @Autowired
    Configuration env;

    @Autowired
    GuestService guestService;

    @Autowired
    JPADaoService jpaDaoService;

    // this seems to be needed in order to be able to access the beddit servers from — at least – my local dev machine
    static void trustAllSSLCertificates() {
// Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

// Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (GeneralSecurityException e) {
        }
// Now you can access an https URL without having the certificate in the truststore
        try {
            URL url = new URL("https://hostname/index.html");
        } catch (MalformedURLException e) {
        }
    }

    @RequestMapping(value = "/token")
    public String getBedditToken(HttpServletRequest request) throws IOException, ServletException {
        if (env.get("development") != null && env.get("development").equals("true"))
            trustAllSSLCertificates();
        OAuthService service = new ServiceBuilder()
                .provider(BedditApi.class)
                .apiKey(env.get("bedditConsumerKey"))
                .apiSecret(env.get("bedditConsumerSecret"))
                .callback(env.get("homeBaseUrl") + "beddit/upgradeToken")
                .build();
        request.getSession().setAttribute(BEDDIT_SERVICE, service);

        // Obtain the Authorization URL
        String authorizationUrl = service.getAuthorizationUrl(EMPTY_TOKEN);
        final String apiKeyIdParameter = request.getParameter("apiKeyId");
        if (apiKeyIdParameter != null)
            request.getSession().setAttribute(BEDDIT_RENEWTOKEN_APIKEYID, apiKeyIdParameter);

        return "redirect:" + authorizationUrl;
    }

    @RequestMapping(value = "/upgradeToken")
    public String upgradeToken(HttpServletRequest request) throws IOException {
        final String code = request.getParameter("code");
        Verifier verifier = new Verifier(code);
        OAuthService service = (OAuthService) request.getSession().getAttribute(BEDDIT_SERVICE);

        Token accessToken = service.getAccessToken(EMPTY_TOKEN, verifier);

        final String token = accessToken.getToken();

        Guest guest = AuthHelper.getGuest();

        final Connector connector = Connector.getConnector("beddit");

        ApiKey apiKey;
        if (request.getSession().getAttribute(BEDDIT_RENEWTOKEN_APIKEYID) != null) {
            final String apiKeyIdString = (String) request.getSession().getAttribute(BEDDIT_RENEWTOKEN_APIKEYID);
            long apiKeyId = Long.valueOf(apiKeyIdString);
            apiKey = guestService.getApiKey(apiKeyId);
        } else
            apiKey = guestService.createApiKey(guest.getId(), connector);

        guestService.populateApiKey(apiKey.getId());
        guestService.setApiKeyAttribute(apiKey, "accessToken", token);

        request.getSession().removeAttribute(BEDDIT_REQUEST_TOKEN);
        request.getSession().removeAttribute(BEDDIT_SERVICE);
        if (request.getSession().getAttribute(BEDDIT_RENEWTOKEN_APIKEYID) != null) {
            request.getSession().removeAttribute(BEDDIT_RENEWTOKEN_APIKEYID);
            return "redirect:/app/tokenRenewed/beddit";
        }
        return "redirect:/app/from/beddit";
    }

}
