package org.fluxtream.connectors.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.fluxtream.Configuration;
import org.fluxtream.aspects.FlxLogger;
import org.fluxtream.connectors.updaters.UpdateFailedException;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.domain.Notification;
import org.fluxtream.services.GuestService;
import org.fluxtream.services.NotificationsService;
import org.fluxtream.utils.HttpUtils;
import org.fluxtream.utils.UnexpectedHttpResponseCodeException;
import org.fluxtream.utils.Utils;
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
	protected NotificationsService notificationsService;

    @Autowired
    Configuration env;
    FlxLogger logger = FlxLogger.getLogger(GoogleOAuth2Helper.class);

    public String getAccessToken(final ApiKey apiKey) throws IOException, UnexpectedHttpResponseCodeException, UpdateFailedException {
        final String expiresString = guestService.getApiKeyAttribute(apiKey, "tokenExpires");
        long expires = Long.valueOf(expiresString);
        if (expires<System.currentTimeMillis())
            refreshToken(apiKey);
        return guestService.getApiKeyAttribute(apiKey, "accessToken");
    }

    private void refreshToken(final ApiKey apiKey) throws IOException, UnexpectedHttpResponseCodeException, UpdateFailedException {
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

            // Notify the user that the tokens need to be manually renewed
            notificationsService.addNotification(apiKey.getGuestId(), Notification.Type.WARNING,
                                                 "Heads Up. This server cannot automatically refresh your authentication tokens.<br>" +
                                                 "Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
                                                 "scroll to the " + apiKey.getConnector().getName() + " connector, and renew your tokens (look for the <i class=\"icon-resize-small icon-large\"></i> icon)");

            // Record permanent failure since this connector won't work again until
            // it is reauthenticated
            guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, null);
            throw new UpdateFailedException("requires token reauthorization",true);
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
            // Record that this connector is now up
            guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_UP, null);
        } catch (IOException e) {
            logger.warn("component=background_updates action=refreshToken" +
                        " connector=" + apiKey.getConnector().getName() + " guestId=" + apiKey.getGuestId() + " status=failed");
            // Notify the user that the tokens need to be manually renewed
            notificationsService.addNotification(apiKey.getGuestId(), Notification.Type.WARNING,
                                                 "Heads Up. We failed in our attempt to automatically refresh your authentication tokens.<br>" +
                                                 "Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
                                                 "scroll to the " + apiKey.getConnector().getName() + " connector, and renew your tokens (look for the <i class=\"icon-resize-small icon-large\"></i> icon)");

            // Record permanent update failure since this connector is never
            // going to succeed
            guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, Utils.stackTrace(e));
            throw new UpdateFailedException("refresh token attempt failed", e, true);
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
