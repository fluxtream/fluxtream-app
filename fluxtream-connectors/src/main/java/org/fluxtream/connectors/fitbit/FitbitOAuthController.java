package org.fluxtream.connectors.fitbit;

import net.sf.json.JSONObject;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.SignpostOAuthHelper;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.Notification;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.NotificationsService;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

@Controller
@RequestMapping(value = "/fitbit")
public class FitbitOAuthController {

	public final static String HAS_OAUTH2 = "has_oauth2";
	FlxLogger logger = FlxLogger.getLogger(FitbitOAuthController.class);

	@Autowired
	GuestService guestService;

	@Autowired
	ApiDataService apiDataService;

	@Autowired
	SignpostOAuthHelper signpostHelper;

	@Autowired
	NotificationsService notificationsService;

	@Autowired
	Configuration env;
	private static final String FITBIT_OAUTH_CONSUMER = "fitbitOAuthConsumer";
	private static final String FITBIT_OAUTH_PROVIDER = "fitbitOAuthProvider";
    private static final String FITBIT_RENEWTOKEN_APIKEYID = "fitbit.renewtoken.apiKeyId";
	public static final String GET_USER_PROFILE_CALL = "FITBIT_GET_USER_PROFILE_CALL";

	static {
		ObjectType.registerCustomObjectType(GET_USER_PROFILE_CALL);
	}

	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request) throws IOException, ServletException,
			OAuthMessageSignerException, OAuthNotAuthorizedException,
			OAuthExpectationFailedException, OAuthCommunicationException {

		String redirectUri = getRedirectUri();

		// Here we know that the redirectUri will work
		String approvalPageUrl = String.format("https://www.fitbit.com/oauth2/authorize?" +
				"prompt=consent&" +
				"redirect_uri=%s&" +
				"response_type=code&client_id=%s",
				redirectUri, env.get("fitbit.client.id"));
		approvalPageUrl += "&scope=" + URLEncoder.encode("activity nutrition profile settings sleep weight", "utf-8");
		final String apiKeyIdParameter = request.getParameter("apiKeyId");
		if (apiKeyIdParameter !=null && !StringUtils.isEmpty(apiKeyIdParameter))
			approvalPageUrl += "&state=" + apiKeyIdParameter;

		return "redirect:" + approvalPageUrl;
	}

	@NotNull
	private String getRedirectUri() {
		return env.get("homeBaseUrl") + "fitbit/oauth2/swapToken";
	}

	@RequestMapping(value = "/oauth2/swapToken")
	public String upgradeToken(HttpServletRequest request) throws Exception {
		final String errorMessage = request.getParameter("error");
		final Guest guest = AuthHelper.getGuest();
		Connector connector = Connector.getConnector("fitbit");
		if (errorMessage!=null) {
			notificationsService.addNamedNotification(guest.getId(),
					Notification.Type.ERROR, connector.statusNotificationName(),
					"There was an error while setting you up with the fitbit service: " + errorMessage);
			return "redirect:/app";
		}
		final String code = request.getParameter("code");

		Map<String,String> parameters = new HashMap<String,String>();
		parameters.put("grant_type", "authorization_code");
		parameters.put("code", code);
		parameters.put("client_id", env.get("fitbit.client.id"));
		parameters.put("redirect_uri", getRedirectUri());
		final String json = fetch("https://api.fitbit.com/oauth2/token", parameters);

		JSONObject token = JSONObject.fromObject(json);

		if (token.has("error")) {
			String errorCode = token.getString("error");
			notificationsService.addNamedNotification(guest.getId(),
					Notification.Type.ERROR,
					connector.statusNotificationName(),
					errorCode);
			// NOTE: In the future if we implement renew for the Fitbit connector
			// we will potentially need to mark the connector as permanently failed.
			// The way to do this is to get hold of the existing apiKey and do:
			//  guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, null);
			return "redirect:/app";
		}

		final String refresh_token = token.getString("refresh_token");

		// Create the entry for this new apiKey in the apiKey table and populate
		// ApiKeyAttributes with all of the keys fro oauth.properties needed for
		// subsequent update of this connector instance.
		ApiKey apiKey;
		final String stateParameter = request.getParameter("state");
		if (stateParameter !=null&&!StringUtils.isEmpty(stateParameter)) {
			long apiKeyId = Long.valueOf(stateParameter);
			apiKey = guestService.getApiKey(apiKeyId);
		} else {
			apiKey = guestService.createApiKey(guest.getId(), Connector.getConnector("fitbit"));
		}

		guestService.populateApiKey(apiKey.getId());
		guestService.setApiKeyAttribute(apiKey,
				HAS_OAUTH2, "true");
		guestService.setApiKeyAttribute(apiKey,
				"accessToken", token.getString("access_token"));
		guestService.setApiKeyAttribute(apiKey,
				"tokenExpires", String.valueOf(System.currentTimeMillis() + (token.getLong("expires_in")*1000)));
		guestService.setApiKeyAttribute(apiKey,
				"refreshToken", refresh_token);
		guestService.setApiKeyAttribute(apiKey,
				"fitbit.client.id", env.get("fitbit.client.id"));
		if (token.has("user_id"))
			guestService.setApiKeyAttribute(apiKey,
					"userId", token.getString("user_id"));

		// Record that this connector is now up
		guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_UP, null, null);

		if (stateParameter !=null&&!StringUtils.isEmpty(stateParameter))
			return "redirect:/app/tokenRenewed/fitbit";
		else
			return "redirect:/app/from/fitbit";

	}

	public String fetch(String url, Map<String, String> params) throws UnexpectedHttpResponseCodeException, IOException {
		HttpClient client = new DefaultHttpClient();
		String content = "";
		try {
			HttpPost post = new HttpPost(url);

			Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(params.size());
			while (iterator.hasNext()) {
				Map.Entry<String, String> entry = iterator.next();
				nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
			}
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs, "utf-8"));
			byte[] fitbitConsumerSecrets = Base64.encodeBase64((env.get("fitbit.client.id") + ":" + env.get("fitbitConsumerSecret")).getBytes());
			String encodedSecrets = new String(fitbitConsumerSecrets);
			post.setHeader("Authorization", "Basic " + encodedSecrets);

			HttpResponse response = client.execute(post);

			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				content = responseHandler.handleResponse(response);
			}
			else {
				throw new UnexpectedHttpResponseCodeException(response.getStatusLine().getStatusCode(),
						response.getStatusLine().getReasonPhrase());
			}
		}
		finally {
			client.getConnectionManager().shutdown();
		}
		return content;
	}

	public String getAccessToken(ApiKey apiKey) throws UpdateFailedException {
		final String expiresString = guestService.getApiKeyAttribute(apiKey, "tokenExpires");
		long expires = Long.valueOf(expiresString);
		if (expires<System.currentTimeMillis())
			refreshToken(apiKey, false);
		return guestService.getApiKeyAttribute(apiKey, "accessToken");
	}

	private void refreshToken(ApiKey apiKey, boolean isOAuth2Upgrade) throws UpdateFailedException {
		// Check to see if we are running on a mirrored test instance
		// and should therefore refrain from swapping tokens lest we
		// invalidate an existing token instance
		String disableTokenSwap = env.get("disableTokenSwap");
		Connector connector = Connector.getConnector("fitbit");
		if(disableTokenSwap!=null && disableTokenSwap.equals("true")) {
			String msg = "**** Skipping refreshToken for fitbit connector instance because disableTokenSwap is set on this server";
			StringBuilder sb2 = new StringBuilder("module=FitbitOauthController component=FitbitController action=refreshToken apiKeyId=" + apiKey.getId())
					.append(" message=\"").append(msg).append("\"");
			logger.info(sb2.toString());
			System.out.println(msg);

			// Notify the user that the tokens need to be manually renewed
			notificationsService.addNamedNotification(apiKey.getGuestId(), Notification.Type.WARNING, connector.statusNotificationName(),
					"Heads Up. This server cannot automatically refresh your Fitbit authentication tokens.<br>" +
							"Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
							"scroll to the Fitbit connector, delete the connector, and re-add<br>" +
							"<p>We apologize for the inconvenience</p>");

			// Record permanent failure since this connector won't work again until
			// it is reauthenticated
			guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, null, ApiKey.PermanentFailReason.NEEDS_REAUTH);
			throw new UpdateFailedException("requires token reauthorization", true, ApiKey.PermanentFailReason.NEEDS_REAUTH);
		} else if (isOAuth2Upgrade){
			// if we are upgrading to oauth2, the Api key used with this user
			// must match with the one this server is setup with
			if (!env.get("fitbitConsumerKey").equals(guestService.getApiKeyAttribute(apiKey, "fitbitConsumerKey"))) {
				String msg = "**** Skipping refreshToken for fitbit connector instance because we are upgrading to oauth2 and user and server keys don't match";
				StringBuilder sb2 = new StringBuilder("module=FitbitOauthController component=FitbitController action=refreshToken apiKeyId=" + apiKey.getId())
						.append(" message=\"").append(msg).append("\"");
				logger.info(sb2.toString());
				System.out.println(msg);

				// Notify the user that the tokens need to be manually renewed
				notificationsService.addNamedNotification(apiKey.getGuestId(), Notification.Type.WARNING, connector.statusNotificationName(),
						"Heads Up. This server cannot automatically upgrade your keys to oauth2.<br>" +
								"Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
								"scroll to the Fitbit connector, delete the connector, and re-add<br>" +
								"<p>We apologize for the inconvenience</p>");

				// Record permanent failure since this connector won't work again until
				// it is reauthenticated
				guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, null, ApiKey.PermanentFailReason.NEEDS_REAUTH);
				throw new UpdateFailedException("requires token reauthorization", true, ApiKey.PermanentFailReason.NEEDS_REAUTH);
			}
		}
		// We're not on a mirrored test server.  Try to swap the expired
		// access token for a fresh one.  Typically fitbit access tokens are good for
		// 1 hour from time of issue.
		String swapTokenUrl = "https://api.fitbit.com/oauth2/token";

		String refreshToken;
		Map<String,String> params = new HashMap<String,String>();
		// there is a one-off upgrade path to oauth2 for existing oauth1 users
		// that allows to avoid forcing them to re-authorize fluxtream...
		if (isOAuth2Upgrade) {
			// refresh_token parameter:
			// The user's OAuth 1.0a access token and access token secret concatenated with a colon.
			String oauth1AccessToken = guestService.getApiKeyAttribute(apiKey, "accessToken");
			refreshToken = oauth1AccessToken + ":" + guestService.getApiKeyAttribute(apiKey, "tokenSecret");
		} else
			refreshToken = guestService.getApiKeyAttribute(apiKey, "refreshToken");
		params.put("refresh_token", refreshToken);
		params.put("grant_type", "refresh_token");

		String fetched;
		try {
			fetched = fetch(swapTokenUrl, params);
			if (isOAuth2Upgrade)
				guestService.setApiKeyAttribute(apiKey, HAS_OAUTH2, "true");
			// Record that this connector is now up
			guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_UP, null, null);
		} catch (Exception e) {
			// Notify the user that the tokens need to be manually renewed
			notificationsService.addNamedNotification(apiKey.getGuestId(), Notification.Type.WARNING, connector.statusNotificationName(),
					"Heads Up. We failed in our attempt to automatically refresh your Fitbit authentication tokens.<br>" +
							"Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
							"scroll to the Fitbit connector, delete the connector, and re-add<br>" +
							"<p>We apologize for the inconvenience</p>");

			// Record permanent update failure since this connector is never
			// going to succeed
			guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, null, ApiKey.PermanentFailReason.NEEDS_REAUTH);
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

	void upgrade2OAuth2(ApiKey apiKey) throws UpdateFailedException {
		refreshToken(apiKey, true);
	}
}
