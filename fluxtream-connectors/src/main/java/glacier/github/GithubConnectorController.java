package glacier.github;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.fluxtream.Configuration;
import org.fluxtream.connectors.Connector;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.domain.Guest;
import org.fluxtream.auth.AuthHelper;
import org.fluxtream.services.GuestService;
import org.fluxtream.utils.HttpUtils;
import org.fluxtream.utils.UnexpectedHttpResponseCodeException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller()
@RequestMapping("/singly/github")
public class GithubConnectorController {

    @Autowired
    GuestService guestService;

    @Autowired
    Configuration env;

    @RequestMapping(value = "/callback")
    public String getToken(HttpServletRequest request) throws IOException, UnexpectedHttpResponseCodeException {
        String code = request.getParameter("code");
        String error = request.getParameter("error");

        Guest guest = AuthHelper.getGuest();

        if (StringUtils.isEmpty(error)) {
            String clientId = env.get("singly.client.id");
            String clientSecret = env.get("singly.client.secret");
            Map<String, String> params = new HashMap<String, String>();
            params.put("client_id", clientId);
            params.put("client_secret", clientSecret);
            params.put("code", code);

            final String tokenJSON = HttpUtils.fetch("https://api.singly.com/oauth/access_token", params);
            JSONObject jsonToken = JSONObject.fromObject(tokenJSON);

            String accessToken = jsonToken.getString("access_token");
            String account = jsonToken.getString("account");

            final Connector connector = Connector.getConnector("github");
            final ApiKey apiKey = guestService.createApiKey(guest.getId(), connector);

            guestService.setApiKeyAttribute(apiKey, "accessToken", accessToken);
            guestService.setApiKeyAttribute(apiKey, "account", account);

            getUserLogin(apiKey, accessToken);

            return "redirect:/app/from/github";
        }

        return "redirect:/app/from/github?error=" + error;
    }

    private void getUserLogin(final ApiKey apiKey, final String accessToken) throws IOException, UnexpectedHttpResponseCodeException {
        final String profileJson = HttpUtils.fetch("https://api.singly.com/services/github/self?access_token=" + accessToken);
        JSONArray jsonProfileArray = JSONArray.fromObject(profileJson);
        JSONObject jsonProfile = jsonProfileArray.getJSONObject(0);
        final JSONObject profileData = jsonProfile.getJSONObject("data");
        final String login = profileData.getString("login");

        guestService.setApiKeyAttribute(apiKey, "login", login);
    }
}
