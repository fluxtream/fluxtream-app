package com.fluxtream.connectors.moves;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
@RequestMapping(value = "/moves")
public class MovesController {

    @Autowired
    Configuration env;

    @Autowired
    NotificationsService notificationsService;

    @Autowired
    GuestService guestService;

    @RequestMapping(value = "/token")
    public String getToken(HttpServletRequest request,
                           HttpServletResponse response) throws IOException, ServletException {

        String redirectUri = getRedirectUri();

        String approvalPageUrl = String.format("https://api.moves-app.com/oauth/v1/authorize?" +
                                               "redirect_uri=%s&" +
                                               "response_type=code&client_id=%s&scope=both",
                                               redirectUri,
                                               env.get("moves.client.id"));

        return "redirect:" + approvalPageUrl;
    }

    private String getRedirectUri() {
        return env.get("homeBaseUrl")
                                   + "moves/oauth2/swapToken";
    }

    @RequestMapping(value="swapToken")
    public String swapToken(HttpServletRequest request) throws IOException {
        final String errorMessage = request.getParameter("errorMessage");
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
        parameters.put("client_secret", env.get("movies.client.secret"));
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


}
