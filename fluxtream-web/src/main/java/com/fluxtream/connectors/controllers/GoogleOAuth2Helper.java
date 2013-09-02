package com.fluxtream.connectors.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.fluxtream.Configuration;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.GuestService;
import com.fluxtream.utils.HttpUtils;
import com.fluxtream.utils.UnexpectedHttpResponseCodeException;
import net.sf.json.JSONObject;
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
    FlxLogger logger = FlxLogger.getLogger(GoogleOAuth2Helper.class);

    public String getAccessToken(final ApiKey apiKey) throws IOException, UnexpectedHttpResponseCodeException {
        final String expiresString = guestService.getApiKeyAttribute(apiKey, "tokenExpires");
        long expires = Long.valueOf(expiresString);
        if (expires<System.currentTimeMillis())
            refreshToken(apiKey);
        return guestService.getApiKeyAttribute(apiKey, "accessToken");
    }

    private void refreshToken(final ApiKey apiKey) throws IOException, UnexpectedHttpResponseCodeException {
        // Check to see if we are running on a mirrored test instance
        // and should therefore refrain from swapping tokens lest we
        // invalidate an existing token instance
        String disableTokenSwap = env.get("disableTokenSwap");
        if(disableTokenSwap!=null && disableTokenSwap.equals("true")) {
            String msg = "**** Skipping refreshToken for google latitude connector instance because disableTokenSwap is set on this server";
                                            ;
            StringBuilder sb2 = new StringBuilder("module=GoogleOAuth2Helper component=GoogleOAuth2Helper action=replaceToken apiKeyId=" + apiKey.getId())
            			    .append(" message=\"").append(msg).append("\"");
            logger.info(sb2.toString());
            System.out.println(msg);
            return;
        }

        // We're not on a mirrored test server.  Try to swap the expired
        // access token for a fresh one.
        String swapTokenUrl = "https://accounts.google.com/o/oauth2/token";

        final String refreshToken = guestService.getApiKeyAttribute(apiKey, "refreshToken");
        Map<String,String> params = new HashMap<String,String>();
        params.put("refresh_token", refreshToken);
        params.put("client_id", env.get("google.client.id"));
        params.put("client_secret", env.get("google.client.secret"));
        params.put("grant_type", "refresh_token");

        String fetched;
        try {
            fetched = HttpUtils.fetch(swapTokenUrl, params);
            logger.info("component=background_updates action=refreshToken" +
                        " connector="
                        + apiKey.getConnector().getName()
                        + " guestId=" + apiKey.getGuestId()
                        + " status=success");
        } catch (IOException e) {
            logger.warn("component=background_updates action=refreshToken" +
                        " connector=" + apiKey.getConnector().getName()
                        + " guestId=" + apiKey.getGuestId()
                        + " status=failed");
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
