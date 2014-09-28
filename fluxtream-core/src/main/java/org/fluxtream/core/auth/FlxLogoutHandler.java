package org.fluxtream.core.auth;

import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.services.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FlxLogoutHandler implements LogoutHandler {

	FlxLogger logger = FlxLogger.getLogger(FlxLogoutHandler.class);
	
	@Autowired
	GuestService guestService;
	
	@Override
	public void logout(HttpServletRequest request,
			HttpServletResponse response, Authentication authentication) {
        if (authentication != null)
            authentication.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(null);
        if (request.getParameter("r") != null) {
            try {
                response.sendRedirect(request.getParameter("r"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
