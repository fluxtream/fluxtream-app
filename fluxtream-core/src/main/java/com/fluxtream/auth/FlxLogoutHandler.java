package com.fluxtream.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import com.fluxtream.services.GuestService;

public class FlxLogoutHandler implements LogoutHandler {

	Logger logger = Logger.getLogger(FlxLogoutHandler.class);
	
	@Autowired
	GuestService guestService;
	
	@Override
	public void logout(HttpServletRequest request,
			HttpServletResponse response, Authentication authentication) {
		if (authentication!=null)
			authentication.setAuthenticated(false);
		SecurityContextHolder.getContext().setAuthentication(null);
	}

}
