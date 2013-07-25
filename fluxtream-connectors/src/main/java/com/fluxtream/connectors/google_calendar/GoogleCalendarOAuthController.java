package com.fluxtream.connectors.google_calendar;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fluxtream.Configuration;
import com.fluxtream.services.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value="/calendar")
public class GoogleCalendarOAuthController {

	private static final String GOOGLE_CALENDAR_SCOPE = "https://www.google.com/calendar/feeds/";
	private static final String CALENDAR_TOKEN_SECRET = "calendarTokenSecret";

	@Autowired
	Configuration env;

	@Autowired
	GuestService guestService;

	@RequestMapping(value = "/token")
	public String getLatitudeToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		//return super.accessToken(request, CALENDAR_TOKEN_SECRET,
		//		GOOGLE_CALENDAR_SCOPE, env.get("homeBaseUrl")
		//				+ "calendar/upgradeToken");
        return null;
	}

	@RequestMapping(value = "/upgradeToken")
	public String upgradeToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		//return super.authorizeToken(request, CALENDAR_TOKEN_SECRET,
		//		Connector.getConnector("GOOGLE_CALENDAR"), null);
        return null;
	}

	protected GuestService guestService() {
		return guestService;
	}


}
