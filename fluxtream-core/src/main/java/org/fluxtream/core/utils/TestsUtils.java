package org.fluxtream.core.utils;

import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.fluxtream.core.auth.FlxUserDetails;
import org.fluxtream.core.domain.Guest;

public class TestsUtils {

	@SuppressWarnings("serial")
	public static void asGuest(String username) {
		Guest guest = new Guest();
		guest.username = username;
		final FlxUserDetails loggedUser = new FlxUserDetails(guest);
//		loggedUser.setDaylightSaving(false);
//		loggedUser.setTzOffset(1.0f);
		Authentication authToken = new Authentication() {
			public Collection<GrantedAuthority> getAuthorities() {return null;}
			public Object getCredentials() {return null;}
			public Object getDetails() { return loggedUser; }
			public Object getPrincipal() { return loggedUser; }
			public boolean isAuthenticated() { return true; }
			public void setAuthenticated(boolean isAuthenticated)
					throws IllegalArgumentException {}
			public String getName() { return null; }
		};
		SecurityContextHolder.getContext().setAuthentication(authToken);
	}
	
}
