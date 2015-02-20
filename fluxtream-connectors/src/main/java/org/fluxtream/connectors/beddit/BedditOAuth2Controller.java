package org.fluxtream.connectors.beddit;

import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.JPADaoService;
import org.fluxtream.core.utils.HttpUtils;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.joda.time.DateTimeConstants;
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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

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
    GuestService guestService;

    @Autowired
    JPADaoService jpaDaoService;

    @Autowired
    Configuration env;

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
    public String upgradeToken(HttpServletRequest request) throws IOException, UnexpectedHttpResponseCodeException, NoSuchAlgorithmException, KeyManagementException {
        final String code = request.getParameter("code");
        Map<String,String> parameters = new HashMap<String,String>();
        parameters.put("grant_type", "authorization_code");
        parameters.put("code", code);
        parameters.put("redirect_uri", env.get("homeBaseUrl") + "beddit/upgradeToken");
        parameters.put("client_id", env.get("bedditConsumerKey"));
        parameters.put("client_secret", env.get("bedditConsumerSecret"));
        final String json = fetch("https://cloudapi.beddit.com/api/v1/auth/authorize", parameters);
        JSONObject token = JSONObject.fromObject(json);

        // Create the entry for this new apiKey in the apiKey table and populate
        // ApiKeyAttributes with all of the keys fro oauth.properties needed for
        // subsequent update of this connector instance.
        ApiKey apiKey;

        Guest guest = AuthHelper.getGuest();
        final Connector connector = Connector.getConnector("beddit");

        final String stateParameter = request.getParameter("state");
        if (stateParameter !=null&&!StringUtils.isEmpty(stateParameter)) {
            long apiKeyId = Long.valueOf(stateParameter);
            apiKey = guestService.getApiKey(apiKeyId);
        } else {
            apiKey = guestService.createApiKey(guest.getId(), connector);
        }

        guestService.populateApiKey(apiKey.getId());
        guestService.setApiKeyAttribute(apiKey,
                "accessToken", token.getString("access_token"));
        guestService.setApiKeyAttribute(apiKey,
                "userid", token.getString("user"));

        request.getSession().removeAttribute(BEDDIT_REQUEST_TOKEN);
        request.getSession().removeAttribute(BEDDIT_SERVICE);
        if (request.getSession().getAttribute(BEDDIT_RENEWTOKEN_APIKEYID) != null) {
            request.getSession().removeAttribute(BEDDIT_RENEWTOKEN_APIKEYID);
            return "redirect:/app/tokenRenewed/beddit";
        }
        return "redirect:/app/from/beddit";
    }

    public String fetch(String url, Map<String, String> params) throws UnexpectedHttpResponseCodeException, IOException, KeyManagementException, NoSuchAlgorithmException {
        HttpClient client = env.getHttpClient();
        if (env.get("development") != null && env.get("development").equals("true")) {
//            HttpHost proxy = new HttpHost("127.0.0.1", 8888, "http");
//            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,proxy);
            client = HttpUtils.httpClientTrustingAllSSLCerts();
        }
        String content = "";
        try {
            HttpPost post = new HttpPost(url);

            Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(params.size());
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs, "utf-8"));

            HttpResponse response = client.execute(post);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                content = responseHandler.handleResponse(response);
            }
            else {
                throw new UnexpectedHttpResponseCodeException(response.getStatusLine().getStatusCode(),
                        response.getStatusLine().getReasonPhrase());
            }
        }
        finally {
            client.getConnectionManager().shutdown();
        }
        return content;
    }


}
