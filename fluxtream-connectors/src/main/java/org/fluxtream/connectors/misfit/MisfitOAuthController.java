package org.fluxtream.connectors.misfit;

import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicNameValuePair;
import org.fluxtream.connectors.controllers.ControllerSupport;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.utils.HttpUtils;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by candide on 09/02/15.
 */
@Controller
@RequestMapping(value = "/misfit")
public class MisfitOAuthController {

    private static final String MISFIT_SERVICE = "misfitService";
    private static final Token EMPTY_TOKEN = null;
    private static final String MISFIT_RENEWTOKEN_APIKEYID = "misfit.renewtoken.apiKeyId";

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
        String apiKeyIdParameter = request.getParameter("apiKeyId");
        if (apiKeyIdParameter != null) {
            request.getSession().setAttribute(MISFIT_RENEWTOKEN_APIKEYID, apiKeyIdParameter);
            authorizationUrl += authorizationUrl.indexOf("?") != -1
                    ? "&state=" + apiKeyIdParameter
                    : "?state=" + apiKeyIdParameter;
        }

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
    public String upgradeToken(HttpServletRequest request) throws IOException, NoSuchAlgorithmException, UnexpectedHttpResponseCodeException, KeyManagementException {
        final String code = request.getParameter("code");
        Map<String,String> parameters = new HashMap<String,String>();
        parameters.put("grant_type", "authorization_code");
        parameters.put("code", code);
        parameters.put("redirect_uri", env.get("homeBaseUrl") + "misfit/upgradeToken");
        parameters.put("client_id", env.get("misfitConsumerKey"));
        parameters.put("client_secret", env.get("misfitConsumerSecret"));

        final String json = fetch("https://api.misfitwearables.com/auth/tokens/exchange?grant_type=authorization_code", parameters);
        JSONObject token = JSONObject.fromObject(json);

        // Create the entry for this new apiKey in the apiKey table and populate
        // ApiKeyAttributes with all of the keys fro oauth.properties needed for
        // subsequent update of this connector instance.
        ApiKey apiKey;

        Guest guest = AuthHelper.getGuest();
        final Connector connector = Connector.getConnector("misfit");

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

        request.getSession().removeAttribute(MISFIT_SERVICE);
        if (request.getSession().getAttribute(MISFIT_RENEWTOKEN_APIKEYID) != null) {
            request.getSession().removeAttribute(MISFIT_RENEWTOKEN_APIKEYID);
            return "redirect:/app/tokenRenewed/misfit";
        }
        return "redirect:/app/from/misfit";
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

    String getConsumerKey() {
        return env.get("misfitConsumerKey");
    }

    String getConsumerSecret() {
        return env.get("misfitConsumerSecret");
    }


}
