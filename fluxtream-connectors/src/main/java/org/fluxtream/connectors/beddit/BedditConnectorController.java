package org.fluxtream.connectors.beddit;

import net.sf.json.JSONObject;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.utils.HttpUtils;
import org.fluxtream.core.utils.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/beddit")
public class BedditConnectorController {

    @Autowired
    protected Configuration env;
    @Autowired
    GuestService guestService;
    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @RequestMapping(value = "/enterAuthInfo")
    public ModelAndView enterProvisioningURL() {
        ModelAndView mav = new ModelAndView("connectors/beddit/enterAuthInfo");
        return mav;
    }

    @RequestMapping(value = "/setAuthInfo", produces = MediaType.APPLICATION_JSON, method = RequestMethod.POST)
    @ResponseBody
    public void setAuthInfo(@RequestParam("email") final String email,
                            @RequestParam("password") final String password,
                            HttpServletRequest req,
                            HttpServletResponse resp) {
        try {

            HttpPost login = new HttpPost("https://cloudapi.beddit.com/api/v1/auth/authorize");
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("grant_type", "password"));
            nameValuePairs.add(new BasicNameValuePair("username", email));
            nameValuePairs.add(new BasicNameValuePair("password", password));
            login.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpClient httpClient = env.getHttpClient();
            // we seem to have problems connecting to the beddit server using https at least locally
            // so let's "fix" this temporarily by accepting all ssl certs when the request is local
            if (env.get("development")!=null&&env.get("development").equals("true"))
                httpClient = HttpUtils.httpClientTrustingAllSSLCerts();
            HttpResponse response = httpClient.execute(login);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                ResponseHandler<String> responseHandler = new BasicResponseHandler();

                //extract the data from the response
                String content = responseHandler.handleResponse(response);
                JSONObject topLevelObject = JSONObject.fromObject(content);
                String access_token = topLevelObject.getString("access_token");
                long user = topLevelObject.getLong("user");

                //create the connector and set the api key attributes
                final Connector connector = Connector.getConnector("beddit");
                final ApiKey apiKey = guestService.createApiKey(AuthHelper.getGuestId(), connector);
                guestService.setApiKeyAttribute(apiKey, "access_token", access_token);
                guestService.setApiKeyAttribute(apiKey, "userid", user + "");

                //schedule and update for the connector
                connectorUpdateService.updateConnector(apiKey, false);

                //return success
            }
            resp.setStatus(statusCode);

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
        }
    }

}
