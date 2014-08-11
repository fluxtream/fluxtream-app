package org.fluxtream.connectors.lastfm;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.connectors.controllers.ControllerSupport;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.Notification;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.NotificationsService;
import org.fluxtream.core.utils.HttpUtils;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.joda.time.DateTimeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.fluxtream.core.utils.Utils.hash;

@Controller
@RequestMapping(value = "/lastfm")
public class LastFmController {

    private static final String LASTFM_RENEWTOKEN_APIKEYID = "lastfm.renewtoken.apiKeyId";
    @Autowired
	GuestService guestService;

	@Autowired
	Configuration env;

    @Autowired
    NotificationsService notificationsService;

	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request) throws IOException, ServletException {

		String oauthCallback = ControllerSupport.getLocationBase(request, env)
				+ "lastfm/upgradeToken";
		if (request.getParameter("guestId") != null)
			oauthCallback += "?guestId=" + request.getParameter("guestId");

		String approvalPageUrl = "http://www.last.fm/api/auth/?api_key="
				+ env.get("lastfmConsumerKey") + "&cb=" + oauthCallback;

        if (request.getParameter("apiKeyId")!=null)
            request.getSession().setAttribute(LASTFM_RENEWTOKEN_APIKEYID,
                                              request.getParameter("apiKeyId"));

		return "redirect:" + approvalPageUrl;
	}

	@RequestMapping(value = "/upgradeToken")
	public String upgradeToken(HttpServletRequest request) throws NoSuchAlgorithmException,
			IOException {

		String token = request.getParameter("token");
		String api_key = env.get("lastfmConsumerKey");
		String api_sig = getApiSig(toMap("token", token, "api_key", api_key, "method",
				"auth.getsession"));

		Map<String, String> params = toMap("method", "auth.getsession",
				"format", "json",
				"token", token, "api_key", api_key, "api_sig", api_sig);
        String jsonResponse;
        try {
            jsonResponse = HttpUtils.fetch("https://ws.audioscrobbler.com/2.0/", params);
        }
        catch (UnexpectedHttpResponseCodeException e) {
            e.printStackTrace();
            notificationsService.addNamedNotification(AuthHelper.getGuestId(), Notification.Type.ERROR, Connector.getConnector("lastfm").statusNotificationName(),
                                                      String.format("Oops, we couldn't link your LastFM account (reason: '%s', http code: %s)" +
                                                                    "<br>Please contact your administrator.",
                                                                    e.getHttpResponseMessage(),
                                                                    e.getHttpResponseCode()));
            return "redirect:/app";
        }

        String sessionKey = LastfmHelper.getSessionKey(jsonResponse);
		String username = LastfmHelper.getUsername(jsonResponse);
		Guest guest = AuthHelper.getGuest();

        final Connector connector = Connector.getConnector("lastfm");
        ApiKey apiKey;
        if (request.getSession().getAttribute(LASTFM_RENEWTOKEN_APIKEYID)!=null) {
            final String apiKeyIdString = (String) request.getSession().getAttribute(LASTFM_RENEWTOKEN_APIKEYID);
            long apiKeyId = Long.valueOf(apiKeyIdString);
            apiKey = guestService.getApiKey(apiKeyId);
        } else
            apiKey = guestService.createApiKey(guest.getId(), connector);

        guestService.populateApiKey(apiKey.getId());
		guestService.setApiKeyAttribute(apiKey, "sessionKey", sessionKey);
		guestService.setApiKeyAttribute(apiKey,  "username", username);

        if (request.getSession().getAttribute(LASTFM_RENEWTOKEN_APIKEYID)!=null) {
            request.getSession().removeAttribute(LASTFM_RENEWTOKEN_APIKEYID);
            return "redirect:/app/tokenRenewed/"+connector.getName();
        }
		return "redirect:/app/from/"+connector.getName();
	}

	Map<String, String> toMap(String... params) {
		HashMap<String, String> map = new HashMap<String, String>();
		for (int i = 0; i < params.length;)
			map.put(params[i++], params[i++]);
		return map;
	}

	String getApiSig(Map<String, String> params)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		Object[] key = params.keySet().toArray();
		Arrays.sort(key);
		String toHash = "";
		for (int i = 0; i < key.length; i++)
			toHash += key[i] + new String(params.get(key[i]).getBytes(), "UTF-8");
		toHash += env.get("lastfmConsumerSecret");
		String hashed = hash(toHash);
		return hashed;
	}

    @RequestMapping(value="/img")
    public void getLastfmImageResource(@RequestParam("url") String url,
                                       final HttpServletResponse response) throws IOException {
        HttpClient client = new DefaultHttpClient();
        response.setDateHeader("Expires", System.currentTimeMillis() + DateTimeConstants.MILLIS_PER_WEEK*30);
        try {
            HttpGet get = new HttpGet(url);

            HttpResponse lastfmResponse = client.execute(get);

            if (lastfmResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                new ResponseHandler<byte[]>() {
                    /** Returns the contents of the response as a <code>byte[]</code>. */
                    @Override
                    public byte[] handleResponse(final HttpResponse lastfmResponse) throws IOException {
                        final HttpEntity entity = lastfmResponse.getEntity();
                        if (entity != null) {
                            final InputStream in = entity.getContent();
                            IOUtils.copy(in, response.getOutputStream());
                        }
                        return new byte[0];
                    }
                }.handleResponse(lastfmResponse);
            }
        }
        finally {
            client.getConnectionManager().shutdown();
        }
    }

}
