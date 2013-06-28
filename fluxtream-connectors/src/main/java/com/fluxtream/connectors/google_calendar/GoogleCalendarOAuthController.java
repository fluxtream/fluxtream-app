package com.fluxtream.connectors.google_calendar;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.controllers.BaseGoogleOAuthController;
import com.fluxtream.connectors.controllers.ControllerSupport;
import com.fluxtream.services.GuestService;
import com.google.gdata.client.authn.oauth.GoogleOAuthHelper;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value="/calendar")
public class GoogleCalendarOAuthController extends BaseGoogleOAuthController {

	private static final String GOOGLE_CALENDAR_SCOPE = "https://www.google.com/calendar/feeds/";
	private static final String CALENDAR_TOKEN_SECRET = "calendarTokenSecret";

	@Autowired
	Configuration env;

	@Autowired
	GuestService guestService;

	@RequestMapping(value = "/token")
	public String getLatitudeToken(HttpServletRequest request) throws IOException, ServletException {
		return super.accessToken(request, CALENDAR_TOKEN_SECRET,
				GOOGLE_CALENDAR_SCOPE, ControllerSupport.getLocationBase(request)
						+ "calendar/upgradeToken");
	}

	@RequestMapping(value = "/upgradeToken")
	public String upgradeToken(HttpServletRequest request) throws IOException {
		return super.authorizeToken(request, CALENDAR_TOKEN_SECRET,
				Connector.getConnector("GOOGLE_CALENDAR"), null);
	}

	protected GuestService guestService() {
		return guestService;
	}

	@Override
	protected String getConsumerKey() {
		return env.get("googleConsumerKey");
	}

	@Override
	protected String getConsumerSecret() {
		return env.get("googleConsumerSecret");
	}

	@Override
	protected GoogleOAuthHelper getOAuthHelper() {
		return new GoogleOAuthHelper(new OAuthHmacSha1Signer());
	}
	
}
