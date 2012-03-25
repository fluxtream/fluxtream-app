package com.fluxtream.connectors.linkedin;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SystemService;

@Controller
@RequestMapping(value = "/linkedin")
public class LinkedInConnectorController {

	@Autowired
	Configuration env;
	
	@Autowired
	SystemService systemService;
	
	@Autowired
	GuestService guestService;

	private static final String LINKEDIN_OAUTH_CONSUMER = "linkedinOAuthConsumer";
	private static final String LINKEDIN_OAUTH_PROVIDER = "linkedinOAuthProvider";

	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException, OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
		String oauthCallback = env.get("homeBaseUrl")
		+ "linkedin/upgradeToken";
		if (request.getParameter("guestId")!=null)
			oauthCallback += "?guestId=" + request.getParameter("guestId");

        String consumerKey = getConsumerKey();
		String consumerSecret = getConsumerSecret();
		
		OAuthConsumer consumer = new DefaultOAuthConsumer(
                consumerKey,
                consumerSecret);
        
        OAuthProvider provider = new DefaultOAuthProvider(
        		"https://api.linkedin.com/uas/oauth/requestToken",
        		"https://api.linkedin.com/uas/oauth/accessToken",
        		"https://api.linkedin.com/uas/oauth/authorize");
        
		request.getSession().setAttribute(LINKEDIN_OAUTH_CONSUMER, consumer);
		request.getSession().setAttribute(LINKEDIN_OAUTH_PROVIDER, provider);
		System.out.println("the token secret is: " + consumer.getTokenSecret());
		
		String approvalPageUrl = provider.retrieveRequestToken(consumer, oauthCallback);
		
		return "redirect:" + approvalPageUrl;
	}
	
	@RequestMapping(value="/upgradeToken")
	public String upgradeToken(HttpServletRequest request,
			HttpServletResponse response) throws OAuthMessageSignerException, OAuthNotAuthorizedException, OAuthExpectationFailedException, OAuthCommunicationException {
		OAuthConsumer consumer = (OAuthConsumer) request.getSession().getAttribute(LINKEDIN_OAUTH_CONSUMER);
		OAuthProvider provider = (OAuthProvider) request.getSession().getAttribute(LINKEDIN_OAUTH_PROVIDER);
		String verifier = request.getParameter("oauth_verifier");
		provider.retrieveAccessToken(consumer, verifier);
		Guest guest = ControllerHelper.getGuest();
				
		guestService.setApiKeyAttribute(guest.getId(), Connector.getConnector("linkedin"), "accessToken", consumer.getToken());
		guestService.setApiKeyAttribute(guest.getId(), Connector.getConnector("linkedin"), "tokenSecret", consumer.getTokenSecret());

		return "redirect:/app/from/"+Connector.getConnector("linkedin").getName();
	}


	String getConsumerKey() {
		return env.get("linkedinConsumerKey");
	}

	String getConsumerSecret() {
		return env.get("linkedinConsumerSecret");
	}
}
