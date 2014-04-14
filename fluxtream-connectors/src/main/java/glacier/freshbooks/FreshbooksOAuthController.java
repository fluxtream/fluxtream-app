package glacier.freshbooks;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.ApiKey;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.signature.PlainTextMessageSigner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import org.fluxtream.core.Configuration;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.GuestService;
import com.google.gdata.client.authn.oauth.OAuthException;

@Controller
@RequestMapping(value="/freshbooks")
public class FreshbooksOAuthController {

	@Autowired
	GuestService guestService;
	
	@Autowired
	Configuration env;
	private static final String FRESHBOOKS_OAUTH_CONSUMER = "freshbooksOAuthConsumer";
	private static final String FRESHBOOKS_OAUTH_PROVIDER = "freshbooksOAuthProvider";
	
	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException, OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
		
		String oauthCallback = env.get("homeBaseUrl")
		+ "freshbooks/upgradeToken";
		if (request.getParameter("guestId")!=null)
			oauthCallback += "?guestId=" + request.getParameter("guestId");

		String userUrl = "palacehotelsoftware";
		
        OAuthConsumer consumer = new DefaultOAuthConsumer(
                userUrl,
                getConsumerSecret());
        consumer.setMessageSigner(new PlainTextMessageSigner());
                
        OAuthProvider provider = new DefaultOAuthProvider(
        		"https://" + userUrl + ".freshbooks.com/oauth/oauth_request.php",
        		"https://" + userUrl + ".freshbooks.com/oauth/oauth_access.php",
        		"https://" + userUrl + ".freshbooks.com/oauth/oauth_authorize.php");
        
		request.getSession().setAttribute(FRESHBOOKS_OAUTH_CONSUMER, consumer);
		request.getSession().setAttribute(FRESHBOOKS_OAUTH_PROVIDER, provider);
		System.out.println("the token secret is: " + consumer.getTokenSecret());
		
		provider.setOAuth10a(true);
		String approvalPageUrl = provider.retrieveRequestToken(consumer, oauthCallback);
		
		return "redirect:" + approvalPageUrl;
	}

	@RequestMapping(value = "/upgradeToken")
	public String upgradeToken(HttpServletRequest request,
			HttpServletResponse response) throws OAuthException, OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
		OAuthConsumer consumer = (OAuthConsumer) request.getSession().getAttribute(FRESHBOOKS_OAUTH_CONSUMER);
		OAuthProvider provider = (OAuthProvider) request.getSession().getAttribute(FRESHBOOKS_OAUTH_PROVIDER);
		String verifier = request.getParameter("oauth_verifier");
		provider.retrieveAccessToken(consumer, verifier);
		Guest guest = AuthHelper.getGuest();

        final Connector connector = Connector.getConnector("freshbooks");
        final ApiKey apiKey = guestService.createApiKey(guest.getId(), connector);

		guestService.setApiKeyAttribute(apiKey, "accessToken", consumer.getToken());
		guestService.setApiKeyAttribute(apiKey, "tokenSecret", consumer.getTokenSecret());

		return "redirect:/app/from/"+connector.getName();
	}

	String getConsumerKey() {
		return env.get("freshbooksConsumerKey");
	}

	String getConsumerSecret() {
		return env.get("freshbooksConsumerSecret");
	}

}
