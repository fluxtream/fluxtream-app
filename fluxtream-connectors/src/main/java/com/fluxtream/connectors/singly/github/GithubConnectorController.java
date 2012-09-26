package com.fluxtream.connectors.singly.github;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.controllers.AuthHelper;
import com.fluxtream.services.GuestService;
import com.fluxtream.utils.HttpUtils;
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
    public String getToken(HttpServletRequest request) throws IOException {
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

            final String tokenJSON = HttpUtils.fetch("https://api.singly.com/oauth/access_token", params, env);
            JSONObject jsonToken = JSONObject.fromObject(tokenJSON);

            String accessToken = jsonToken.getString("access_token");
            String account = jsonToken.getString("account");

            guestService.setApiKeyAttribute(guest.getId(), Connector.getConnector("github"), "accessToken", accessToken);
            guestService.setApiKeyAttribute(guest.getId(), Connector.getConnector("github"), "account", account);

            getUserLogin(guest.getId(), accessToken);

            return "redirect:/app/from/github";
        }

        return "redirect:/app/from/github?error=" + error;
    }

    private void getUserLogin(final long guestId, final String accessToken) throws IOException {
        final String profileJson = HttpUtils.fetch("https://api.singly.com/services/github/self?access_token=" + accessToken, env);
        JSONArray jsonProfileArray = JSONArray.fromObject(profileJson);
        JSONObject jsonProfile = jsonProfileArray.getJSONObject(0);
        final JSONObject profileData = jsonProfile.getJSONObject("data");
        final String login = profileData.getString("login");

        guestService.setApiKeyAttribute(guestId, Connector.getConnector("github"), "login", login);
    }
}
