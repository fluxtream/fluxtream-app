package org.fluxtream.connectors.mymee;

import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller()
@RequestMapping("/mymee")
public class MymeeConnectorController {

	@Autowired
	GuestService guestService;

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Autowired
    MymeeUpdater updater;

	@RequestMapping(value = "/enterAuthInfo")
	public ModelAndView enterProvisioningURL() {
		ModelAndView mav = new ModelAndView("connectors/mymee/enterAuthInfo");
		return mav;
	}

	@RequestMapping(value="/setFetchURL", method=RequestMethod.POST)
	public ModelAndView setProvisioningURL(@RequestParam("url") String url,
                                           HttpServletRequest request ) throws MessagingException
	{
        List<String> required = new ArrayList<String>();
        if (url==null|| StringUtils.isEmpty(url)) required.add("fetchURL");
        if (required.size()>0) {
            ModelAndView mav = new ModelAndView("forward:/mymee/enterAuthInfo");
            mav.addObject("required", required);
            return mav;
        }
		ModelAndView mav = new ModelAndView("connectors/mymee/success");
		long guestId = AuthHelper.getGuestId();
        boolean worked = false;
		try { updater.fetchRetrying(null, url, 20); worked = true;}
		catch (Exception e) {
            e.printStackTrace();
        }
		if (worked) {
            final Connector connector = Connector.getConnector("mymee");
            final ApiKey apiKey = guestService.createApiKey(guestId, connector);
			guestService.setApiKeyAttribute(apiKey,  "fetchURL", url);
            connectorUpdateService.updateConnector(apiKey, false);
			return mav;
		} else {
			request.setAttribute("errorMessage", "Sorry, the URL you provided did not work.\n" +
                                                 "Please check that you entered it correctly.");
			return new ModelAndView("connectors/mymee/enterAuthInfo");
		}
	}

    @RequestMapping(value="/setConnectionParams", method=RequestMethod.POST)
    public ModelAndView setConnectionParams(@FormParam("cloudDatabaseDomain") String cloudDatabaseDomain,
                                            @FormParam("cloudDatabaseName") String cloudDatabaseName,
                                            @FormParam("cloudDatabaseUsername") String cloudDatabaseUsername,
                                            @FormParam("cloudDatabasePassword") String cloudDatabasePassword)
    {
        long guestId = AuthHelper.getGuestId();
        final Connector connector = Connector.getConnector("mymee");
        final ApiKey apiKey = guestService.createApiKey(guestId, connector);
        guestService.setApiKeyAttribute(apiKey, "cloudDatabaseDomain", cloudDatabaseDomain);
        guestService.setApiKeyAttribute(apiKey, "cloudDatabaseName", cloudDatabaseName);
        guestService.setApiKeyAttribute(apiKey, "cloudDatabaseUsername", cloudDatabaseUsername);
        guestService.setApiKeyAttribute(apiKey, "cloudDatabasePassword", cloudDatabasePassword);
        connectorUpdateService.updateConnector(apiKey, false);

        ModelAndView mav = new ModelAndView("connectors/mymee/success");
        return mav;
    }

    @RequestMapping(value="/setAuthInfo", method=RequestMethod.POST)
    public ModelAndView setAuthInfo(@RequestParam("username") String username,
                                    @RequestParam("password") String password,
                                    @RequestParam("activationCode") String activationCode)
    {
        List<String> required = new ArrayList<String>();
        if (username==null|| StringUtils.isEmpty(username)) required.add("username");
        if (password==null|| StringUtils.isEmpty(password)) required.add("password");
        if (activationCode==null|| StringUtils.isEmpty(activationCode)) required.add("activationCode");
        if (required.size()>0) {
            ModelAndView mav = new ModelAndView("forward:/mymee/enterAuthInfo");
            mav.addObject("username", username);
            mav.addObject("activationCode", activationCode);
            mav.addObject("required", required);
            return mav;
        }
        HttpClient client = new DefaultHttpClient();
        CookieStore cookieStore = new BasicCookieStore();
        HttpContext httpContext = new BasicHttpContext();
        httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        String payload;
        try {
            boolean sessionEstablished = establishSession(client, httpContext, username, password);
            if (! sessionEstablished)
                throw new RuntimeException("Could not establish a session with the couchdb server");
            String userSignature = encrypt(username+password+activationCode);
            payload = getActivationInfo(client, httpContext, userSignature);

            ModelAndView mav = new ModelAndView("connectors/mymee/decrypt");
            mav.addObject("key", activationCode);
            mav.addObject("payload", payload);
            return mav;
        } catch (Exception e) {
            ModelAndView mav = new ModelAndView("forward:/mymee/enterAuthInfo");
            mav.addObject("username", username);
            mav.addObject("activationCode", activationCode);
            final String message = e.getMessage()!=null
                    ? e.getMessage()
                    : ExceptionUtils.getStackTrace(e);
            mav.addObject("errorMessage", message);
            return mav;
        }
        finally {
            client.getConnectionManager().shutdown();
        }

        //long guestId = AuthHelper.getGuestId();
        //final Connector connector = Connector.getConnector("mymee");
        //final ApiKey apiKey = guestService.createApiKey(guestId, connector);
        //connectorUpdateService.updateConnector(apiKey, false);
    }

    private String getActivationInfo(final HttpClient client, final HttpContext httpContext, final String userSignature) throws IOException, UnexpectedHttpResponseCodeException {
        HttpGet get = new HttpGet("http://mymee.iriscouch.com/activation/" + userSignature);
        final HttpResponse response = client.execute(get, httpContext);
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == HttpStatus.SC_OK) {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String content = responseHandler.handleResponse(response);
            JSONObject json = JSONObject.fromObject(content);
            if (json.has("payload")) {
                return json.getString("payload");
            }
            else throw new RuntimeException("Could not get payload from Couch server");
        }
        else {
            throw new UnexpectedHttpResponseCodeException(statusCode, response.getStatusLine().getReasonPhrase());
        }
    }

    private boolean establishSession(final HttpClient client, final HttpContext httpContext, final String username, final String password) throws IOException, UnexpectedHttpResponseCodeException {
        final String content;
        HttpPost post = new HttpPost("http://mymee.iriscouch.com/_session");
        post.addHeader("Content-Type", "application/json");
        post.addHeader("Accept", "*/*");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", username);
        jsonObject.put("password", password);
        post.setEntity(new StringEntity(jsonObject.toString(),"utf-8"));
//        HttpHost proxy = new HttpHost("localhost",8899);
//        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,proxy);
        HttpResponse response = client.execute(post, httpContext);

        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == HttpStatus.SC_OK) {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            content = responseHandler.handleResponse(response);
            JSONObject json = JSONObject.fromObject(content);
            return json.getBoolean("ok");
        }
        else {
            throw new UnexpectedHttpResponseCodeException(statusCode, response.getStatusLine().getReasonPhrase());
        }
    }

    private String encrypt(String s) {
        Security.addProvider(new MymeeCrypto());
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-224", "MymeeCrypto");
            byte[] result;
            result = digest.digest(s.getBytes());

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < result.length; i++)
                sb.append(String.format("%02x", result[i]));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Couldn't encrypt ");
    }

}
