package com.fluxtream.connectors.moves;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import com.fluxtream.Configuration;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.Notification;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.NotificationsService;
import com.fluxtream.utils.HttpUtils;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * User: candide
 * Date: 17/06/13
 * Time: 16:49
 */
@Controller
@RequestMapping(value = "/moves/oauth2")
public class MovesController {

    @Autowired
    Configuration env;

    @Autowired
    NotificationsService notificationsService;

    @Autowired
    GuestService guestService;

    @RequestMapping(value = "/token")
    public String getToken() throws IOException, ServletException {

        String redirectUri = getRedirectUri();

        String approvalPageUrl = String.format("https://api.moves-app.com/oauth/v1/authorize?" +
                                               "redirect_uri=%s&" +
                                               "response_type=code&client_id=%s&" +
                                               "scope=activity location",
                                               redirectUri, env.get("moves.client.id"));

        return "redirect:" + approvalPageUrl;
    }

    private String getRedirectUri() {
        return env.get("homeBaseUrl") + "moves/oauth2/swapToken";
    }

    @RequestMapping(value="swapToken")
    public String swapToken(HttpServletRequest request) throws IOException {
        final String errorMessage = request.getParameter("error");
        final Guest guest = AuthHelper.getGuest();
        if (errorMessage!=null) {
            notificationsService.addNotification(guest.getId(),
                                                 Notification.Type.ERROR,
                                                 errorMessage);
            return "redirect:/app";
        }
        final String code = request.getParameter("code");

        Map<String,String> parameters = new HashMap<String,String>();
        parameters.put("grant_type", "authorization_code");
        parameters.put("code", code);
        parameters.put("client_id", env.get("moves.client.id"));
        parameters.put("client_secret", env.get("moves.client.secret"));
        parameters.put("redirect_uri", getRedirectUri());
        final String json = HttpUtils.fetch("https://api.moves-app.com/oauth/v1/access_token", parameters);

        JSONObject token = JSONObject.fromObject(json);

        if (token.has("error")) {
            String errorCode = token.getString("error");
            notificationsService.addNotification(guest.getId(),
                                                 Notification.Type.ERROR,
                                                 errorCode);
            return "redirect:/app";
        }

        final String refresh_token = token.getString("refresh_token");
        ApiKey apiKey = guestService.createApiKey(guest.getId(), Connector.getConnector("moves"));

        guestService.setApiKeyAttribute(apiKey,
                                        "accessToken", token.getString("access_token"));
        guestService.setApiKeyAttribute(apiKey,
                                        "tokenExpires", String.valueOf(System.currentTimeMillis() + (token.getLong("expires_in")*1000)));
        guestService.setApiKeyAttribute(apiKey,
                                        "refreshToken", refresh_token);

        return "redirect:/app/from/moves";
    }

    String getAccessToken(final ApiKey apiKey) throws IOException {
        final String expiresString = guestService.getApiKeyAttribute(apiKey, "tokenExpires");
        long expires = Long.valueOf(expiresString);
        if (expires<System.currentTimeMillis())
            refreshToken(apiKey);
        return guestService.getApiKeyAttribute(apiKey, "accessToken");
    }

    private void refreshToken(final ApiKey apiKey) throws IOException {
        String swapTokenUrl = "https://api.moves-app.com/oauth/v1/access_token";

        final String refreshToken = guestService.getApiKeyAttribute(apiKey, "refreshToken");
        Map<String,String> params = new HashMap<String,String>();
        params.put("refresh_token", refreshToken);
        params.put("client_id", env.get("google.client.id"));
        params.put("client_secret", env.get("google.client.secret"));
        params.put("grant_type", "refresh_token");

        String fetched;
        try {
            fetched = HttpUtils.fetch(swapTokenUrl, params);
        } catch (IOException e) {
            throw e;
        }

        JSONObject token = JSONObject.fromObject(fetched);
        final long expiresIn = token.getLong("expires_in");
        final String access_token = token.getString("access_token");

        final long now = System.currentTimeMillis();
        long tokenExpires = now + (expiresIn*1000);

        guestService.setApiKeyAttribute(apiKey,
                                        "accessToken", access_token);
        guestService.setApiKeyAttribute(apiKey,
                                        "tokenExpires", String.valueOf(tokenExpires));

    }


}
