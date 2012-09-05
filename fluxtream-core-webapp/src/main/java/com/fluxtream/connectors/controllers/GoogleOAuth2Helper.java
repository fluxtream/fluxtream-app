package com.fluxtream.connectors.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.services.GuestService;
import com.fluxtream.utils.HttpUtils;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component
public class GoogleOAuth2Helper {

    @Autowired
    GuestService guestService;

    @Autowired
    Configuration env;
    Logger logger = Logger.getLogger(GoogleOAuth2Helper.class);

    public String getAccessToken(long guestId, Connector connector) throws IOException {
        final String expiresString = guestService.getApiKeyAttribute(guestId, connector, "tokenExpires");
        long expires = Long.valueOf(expiresString);
        if (expires<System.currentTimeMillis())
            refreshToken(guestId, connector);
        return guestService.getApiKeyAttribute(guestId, connector, "accessToken");
    }

    private void refreshToken(final long guestId, final Connector connector) throws IOException {
        String swapTokenUrl = "https://accounts.google.com/o/oauth2/token";

        final String refreshToken = guestService.getApiKeyAttribute(guestId, connector, "refreshToken");
        Map<String,String> params = new HashMap<String,String>();
        params.put("refresh_token", refreshToken);
        params.put("client_id", env.get("google.client.id"));
        params.put("client_secret", env.get("google.client.secret"));
        params.put("grant_type", "refresh_token");

        String fetched = HttpUtils.fetch(swapTokenUrl, params, env);

        JSONObject token = JSONObject.fromObject(fetched);
        final long expiresIn = token.getLong("expires_in");
        final String access_token = token.getString("access_token");

        final long now = System.currentTimeMillis();
        long tokenExpires = now + (expiresIn*1000);

        guestService.setApiKeyAttribute(guestId, connector,
                                        "accessToken", access_token);
        guestService.setApiKeyAttribute(guestId, connector,
                                        "tokenExpires", String.valueOf(tokenExpires));

        String storedAccessToken = guestService.getApiKeyAttribute(guestId, connector, "accessToken");
        boolean areEqual = storedAccessToken.equals(access_token);
    }
}
