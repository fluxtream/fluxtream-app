package glacier.instagram;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fluxtream.auth.AuthHelper;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.utils.UnexpectedHttpResponseCodeException;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import org.fluxtream.Configuration;
import org.fluxtream.connectors.Connector;
import org.fluxtream.domain.Guest;
import org.fluxtream.services.GuestService;
import org.fluxtream.services.SystemService;
import org.fluxtream.utils.HttpUtils;

@Controller
@RequestMapping(value = "/instagram")
public class InstagramOAuth2Controller {

	@Autowired
	Configuration env;
	
	@Autowired
	SystemService systemService;
	
	@Autowired
	GuestService guestService;

	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException{
		
		String scope = request.getParameter("scope");
		request.getSession().setAttribute("oauth2Scope", scope);
		String redirectUri = env.get("homeBaseUrl") + "instagram/swapToken";
		
		String clientId = env.get("instagram.client.id");

		String authorizeUrl = "https://api.instagram.com/oauth/authorize/?client_id=" + clientId + 
			"&redirect_uri=" + redirectUri +
			"&response_type=code";
		
		return "redirect:" + authorizeUrl;
	}
	
	@RequestMapping(value = "/swapToken")
	public String upgradeToken(HttpServletRequest request) throws IOException, UnexpectedHttpResponseCodeException {
		
		String swapTokenUrl = "https://api.instagram.com/oauth/access_token";
		String code = request.getParameter("code");
		String redirectUri = env.get("homeBaseUrl") + "instagram/swapToken";
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("code", code);
		params.put("client_id", env.get("instagram.client.id"));
		params.put("client_secret", env.get("instagram.client.secret"));
		params.put("redirect_uri", redirectUri);
		params.put("grant_type", "authorization_code");

		String fetched = HttpUtils.fetch(swapTokenUrl, params);

		JSONObject token = JSONObject.fromObject(fetched);
		
//		String scope = (String) request.getSession().getAttribute("oauth2Scope");
		Guest guest = AuthHelper.getGuest();

        final Connector connector = Connector.getConnector("instagram");
        final ApiKey apiKey = guestService.createApiKey(guest.getId(), connector);

		guestService.setApiKeyAttribute(apiKey,
				"accessToken", token.getString("access_token"));
		
		JSONObject userObject = token.getJSONObject("user");

        guestService.setApiKeyAttribute(apiKey, "id", userObject.getString("id"));
		guestService.setApiKeyAttribute(apiKey,
			"username", userObject.getString("username"));
		guestService.setApiKeyAttribute(apiKey,
			"full_name", userObject.getString("full_name"));
		guestService.setApiKeyAttribute(apiKey,
			"profile_picture", userObject.getString("profile_picture"));

		return "redirect:/app/from/"+connector.getName();
	}
}
