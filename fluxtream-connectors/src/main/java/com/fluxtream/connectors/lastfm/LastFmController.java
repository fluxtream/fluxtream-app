package com.fluxtream.connectors.lastfm;

import static com.fluxtream.utils.Utils.hash;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fluxtream.auth.AuthHelper;
import com.fluxtream.domain.ApiKey;
import org.apache.commons.httpclient.HttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.Guest;
import com.fluxtream.services.GuestService;
import com.fluxtream.utils.HttpUtils;

@Controller
@RequestMapping(value = "/lastfm")
public class LastFmController {

	@Autowired
	GuestService guestService;

	@Autowired
	Configuration env;

	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {

		String oauthCallback = env.get("homeBaseUrl")
				+ "lastfm/upgradeToken";
		if (request.getParameter("guestId") != null)
			oauthCallback += "?guestId=" + request.getParameter("guestId");

		String approvalPageUrl = "http://www.last.fm/api/auth/?api_key="
				+ env.get("lastfmConsumerKey") + "&cb=" + oauthCallback;

		return "redirect:" + approvalPageUrl;
	}

	@RequestMapping(value = "/upgradeToken")
	public String upgradeToken(HttpServletRequest request,
			HttpServletResponse response) throws NoSuchAlgorithmException,
			HttpException, IOException {

		String token = request.getParameter("token");
		String api_key = env.get("lastfmConsumerKey");
		String api_sig = getApiSig(toMap("token", token, "api_key", api_key, "method",
				"auth.getsession"));

		Map<String, String> params = toMap("method", "auth.getsession",
				"format", "json",
				"token", token, "api_key", api_key, "api_sig", api_sig);
		String jsonResponse = HttpUtils.fetch(
				"https://ws.audioscrobbler.com/2.0/", params);
		
		String sessionKey = LastfmHelper.getSessionKey(jsonResponse);
		String username = LastfmHelper.getUsername(jsonResponse);
		Guest guest = AuthHelper.getGuest();

        final Connector connector = Connector.getConnector("lastfm");
        final ApiKey apiKey = guestService.createApiKey(guest.getId(), connector);

		guestService.setApiKeyAttribute(apiKey, "sessionKey", sessionKey);
		guestService.setApiKeyAttribute(apiKey,  "username", username);
		
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
}
