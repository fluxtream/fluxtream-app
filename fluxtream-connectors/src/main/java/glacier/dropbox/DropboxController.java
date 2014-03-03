package glacier.dropbox;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fluxtream.auth.AuthHelper;
import org.fluxtream.domain.ApiKey;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import org.fluxtream.Configuration;
import org.fluxtream.connectors.Connector;
import org.fluxtream.domain.Guest;
import org.fluxtream.services.GuestService;
import com.google.gdata.client.authn.oauth.OAuthException;

@Controller
@RequestMapping(value="/dropbox")
public class DropboxController {

	@Autowired
	GuestService guestService;
	
	@Autowired
	Configuration env;
	private static final String DROPBOX_OAUTH_CONSUMER = "dropboxOAuthConsumer";
	private static final String DROPBOX_OAUTH_PROVIDER = "dropboxOAuthProvider";
	
	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException, OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
		
		String oauthCallback = env.get("homeBaseUrl")
		+ "dropbox/upgradeToken";
		if (request.getParameter("guestId")!=null)
			oauthCallback += "?guestId=" + request.getParameter("guestId");

        OAuthConsumer consumer = new DefaultOAuthConsumer(
                getConsumerKey(),
                getConsumerSecret());
        
        OAuthProvider provider = new DefaultOAuthProvider(
        		"http://api.getdropbox.com/0/oauth/request_token",
        		"http://api.getdropbox.com/0/oauth/access_token",
        		"http://api.getdropbox.com/0/oauth/authorize");
        
		request.getSession().setAttribute(DROPBOX_OAUTH_CONSUMER, consumer);
		request.getSession().setAttribute(DROPBOX_OAUTH_PROVIDER, provider);
		System.out.println("the token secret is: " + consumer.getTokenSecret());
		
		String approvalPageUrl = provider.retrieveRequestToken(consumer, oauthCallback);
		
		return "redirect:" + approvalPageUrl;
	}

	@RequestMapping(value = "/upgradeToken")
	public String upgradeToken(HttpServletRequest request,
			HttpServletResponse response) throws OAuthException, OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
		OAuthConsumer consumer = (OAuthConsumer) request.getSession().getAttribute(DROPBOX_OAUTH_CONSUMER);
		OAuthProvider provider = (OAuthProvider) request.getSession().getAttribute(DROPBOX_OAUTH_PROVIDER);
		String verifier = request.getParameter("oauth_verifier");
		provider.retrieveAccessToken(consumer, verifier);
		Guest guest = AuthHelper.getGuest();
        final Connector connector = Connector.getConnector("dropbox");
        final ApiKey apiKey = guestService.createApiKey(guest.getId(), connector);

		guestService.setApiKeyAttribute(apiKey, "accessToken", consumer.getToken());
		guestService.setApiKeyAttribute(apiKey, "tokenSecret", consumer.getTokenSecret());
		
		return "redirect:/app/from/"+connector.getName() ;
	}
	
	String getConsumerKey() {
		return env.get("dropboxConsumerKey");
	}

	String getConsumerSecret() {
		return env.get("dropboxConsumerSecret");
	}
	
}
