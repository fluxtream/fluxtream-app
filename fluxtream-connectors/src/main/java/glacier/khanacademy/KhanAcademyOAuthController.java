package glacier.khanacademy;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.ApiKey;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import org.fluxtream.core.Configuration;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.GuestService;
import com.google.gdata.client.authn.oauth.OAuthException;

@Controller
@RequestMapping(value="/khanacademy")
public class KhanAcademyOAuthController {

	@Autowired
	GuestService guestService;
	
	@Autowired
	Configuration env;
	private static final String KHAN_OAUTH_CONSUMER = "khanOAuthConsumer";
	private static final String KHAN_OAUTH_PROVIDER = "khanOAuthProvider";
	
	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException, OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
		
		String oauthCallback = env.get("homeBaseUrl")
		+ "khanacademy/upgradeToken";
		if (request.getParameter("guestId")!=null)
			oauthCallback += "?guestId=" + request.getParameter("guestId");

        String consumerKey = getConsumerKey();
		String consumerSecret = getConsumerSecret();
		
		OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
                consumerKey,
                consumerSecret);
        
        OAuthProvider provider = new CommonsHttpOAuthProvider(
        		"http://www.khanacademy.org/api/auth/request_token",
        		"http://www.khanacademy.org/api/auth/access_token",
        		"http://www.khanacademy.org/api/auth/authorize");
        
		request.getSession().setAttribute(KHAN_OAUTH_CONSUMER, consumer);
		request.getSession().setAttribute(KHAN_OAUTH_PROVIDER, provider);

		try {
			provider.retrieveRequestToken(consumer, oauthCallback);
		} catch (Throwable e) {
			//TODO: a redirection happens here, and it should be handled
			System.out.println("redirection here");
		}
		System.out.println("the token secret is (musn't be null): " + consumer.getTokenSecret());
		
		return "redirect:" + "/home";
	}

	@RequestMapping(value = "/upgradeToken")
	public String upgradeToken(HttpServletRequest request,
			HttpServletResponse response) throws OAuthException, OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
		OAuthConsumer consumer = (OAuthConsumer) request.getSession().getAttribute(KHAN_OAUTH_CONSUMER);
		OAuthProvider provider = (OAuthProvider) request.getSession().getAttribute(KHAN_OAUTH_PROVIDER);
		String verifier = request.getParameter("oauth_verifier");
		provider.retrieveAccessToken(consumer, verifier);
		Guest guest = AuthHelper.getGuest();

        final Connector connector = Connector.getConnector("khanacademy");
        final ApiKey apiKey = guestService.createApiKey(guest.getId(), connector);

		guestService.setApiKeyAttribute(apiKey, "accessToken", consumer.getToken());
		guestService.setApiKeyAttribute(apiKey, "tokenSecret", consumer.getTokenSecret());

		return "redirect:/app/from/"+connector.getName();
	}

	String getConsumerKey() {
		return env.get("khanacademyConsumerKey");
	}

	String getConsumerSecret() {
		return env.get("khanacademyConsumerSecret");
	}

}
