package glacier.picasa;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fluxtream.auth.AuthHelper;
import com.fluxtream.domain.ApiKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.Guest;
import com.fluxtream.services.GuestService;
import com.google.api.client.auth.oauth.OAuthAuthorizeTemporaryTokenUrl;
import com.google.api.client.auth.oauth.OAuthCredentialsResponse;
import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.auth.oauth.OAuthParameters;
import com.google.api.client.googleapis.GoogleTransport;
import com.google.api.client.googleapis.auth.oauth.GoogleOAuthGetAccessToken;
import com.google.api.client.googleapis.auth.oauth.GoogleOAuthGetTemporaryToken;
import com.google.api.client.http.HttpTransport;

@Controller
@RequestMapping(value = "/picasa")
public class PicasaOAuthController {

	private static final String PICASA_SCOPE = "http://picasaweb.google.com/data/";
	private static final String PICASA_TOKEN_SECRET = "picasaTokenSecret";

	@Autowired
	Configuration env;

	@Autowired
	GuestService guestService;

	private OAuthHmacSigner signer;

	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		String oauthCallback = env.get("homeBaseUrl") + "picasa/upgradeToken";

		GoogleOAuthGetTemporaryToken temporaryToken = new GoogleOAuthGetTemporaryToken();
		signer = new OAuthHmacSigner();
		signer.clientSharedSecret = getConsumerSecret();
		temporaryToken.signer = signer;
		temporaryToken.consumerKey = getConsumerKey();
		temporaryToken.scope = PICASA_SCOPE;
		temporaryToken.callback = oauthCallback;
		OAuthCredentialsResponse tempCredentials = temporaryToken.execute();
		signer.tokenSharedSecret = tempCredentials.tokenSecret;
		OAuthAuthorizeTemporaryTokenUrl authorizeUrl = new OAuthAuthorizeTemporaryTokenUrl(
				"https://www.google.com/accounts/OAuthAuthorizeToken");

		request.getSession().setAttribute(PICASA_TOKEN_SECRET,
				tempCredentials.token);

		authorizeUrl.temporaryToken = tempCredentials.token;
		String authorizationUrl = authorizeUrl.build();

		System.out.println("redirect url: " + authorizationUrl);

		return "redirect:" + authorizationUrl;
	}

	@RequestMapping(value = "/upgradeToken")
	public String upgradeToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		GoogleOAuthGetAccessToken accessToken = new GoogleOAuthGetAccessToken();
		String tempToken = (String) request.getSession().getAttribute(
				PICASA_TOKEN_SECRET);

		accessToken.temporaryToken = tempToken;
		accessToken.signer = signer;
		accessToken.consumerKey = getConsumerKey();
		accessToken.verifier = request.getParameter("oauth_verifier");
		OAuthCredentialsResponse credentials = accessToken.execute();
		signer.tokenSharedSecret = credentials.tokenSecret;
		HttpTransport transport = GoogleTransport.create();

		createOAuthParameters(credentials)
				.signRequestsUsingAuthorizationHeader(transport);

		Guest guest = AuthHelper.getGuest();

        final Connector connector = Connector.getConnector("picasa");
        final ApiKey apiKey = guestService.createApiKey(guest.getId(), connector);

		guestService().setApiKeyAttribute(apiKey, "accessToken", credentials.token);
		guestService().setApiKeyAttribute(apiKey, "tokenSecret", credentials.tokenSecret);

		return "redirect:/app/from/"+connector.getName();
	}

	private OAuthParameters createOAuthParameters(
			OAuthCredentialsResponse credentials) {
		OAuthParameters authorizer = new OAuthParameters();
		authorizer.consumerKey = getConsumerKey();
		authorizer.signer = signer;
		authorizer.token = credentials.token;
		return authorizer;
	}

	protected GuestService guestService() {
		return guestService;
	}

	protected String getConsumerKey() {
		return env.get("googleConsumerKey");
	}

	protected String getConsumerSecret() {
		return env.get("googleConsumerSecret");
	}

}
