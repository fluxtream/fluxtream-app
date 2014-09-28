package org.fluxtream.connectors.controllers;

import net.sf.json.JSONObject;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Notification;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.NotificationsService;
import org.fluxtream.core.utils.HttpUtils;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.fluxtream.core.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
            guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, null, ApiKey.PermanentFailReason.NEEDS_REAUTH);
            throw new UpdateFailedException("requires token reauthorization", true, ApiKey.PermanentFailReason.NEEDS_REAUTH);
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
            guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_UP, null, null);
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
            guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, Utils.stackTrace(e), ApiKey.PermanentFailReason.NEEDS_REAUTH);
            throw new UpdateFailedException("refresh token attempt failed", e, true, ApiKey.PermanentFailReason.NEEDS_REAUTH);
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
