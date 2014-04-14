package glacier.foursquare;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import org.fluxtream.core.Configuration;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.SystemService;
import org.fluxtream.core.utils.HttpUtils;

@Controller
@RequestMapping(value = "/foursquare")
public class FoursquareConnectorController {

	@Autowired
	Configuration env;
	
	@Autowired
	SystemService systemService;
	
	@Autowired
	GuestService guestService;

	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException{
		
		String redirectUri = env.get("foursquare.redirect_uri");
		String clientId = env.get("foursquare.client.id");

		String authorizeUrl = "https://foursquare.com/oauth2/authenticate" +
			"?client_id=" + clientId +
			"&response_type=code" +
			"&redirect_uri=" + redirectUri;
		
		return "redirect:" + authorizeUrl;
	}
	
	@RequestMapping(value = "/swapToken")
	public String upgradeToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, UnexpectedHttpResponseCodeException {
		
		String swapTokenUrl = "https://foursquare.com/oauth2/access_token";
		String code = request.getParameter("code");
		String redirectUri = env.get("foursquare.redirect_uri");
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("code", code);
		params.put("client_id", env.get("foursquare.client.id"));
		params.put("client_secret", env.get("foursquare.client.secret"));
		params.put("redirect_uri", redirectUri);
		params.put("grant_type", "authorization_code");
		
		String fetched = HttpUtils.fetch(swapTokenUrl, params);
		
		JSONObject token = JSONObject.fromObject(fetched);
		Connector scopedApi = Connector.getConnector("foursquare");
		Guest guest = AuthHelper.getGuest();

        final ApiKey apiKey = guestService.createApiKey(guest.getId(), scopedApi);

		guestService.setApiKeyAttribute(apiKey,
				"accessToken", token.getString("access_token"));
		
		return "redirect:/app/from/"+scopedApi.getName();
	}
}
