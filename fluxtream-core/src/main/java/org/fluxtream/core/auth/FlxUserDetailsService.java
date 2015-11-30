package org.fluxtream.core.auth;

import javax.persistence.PersistenceContext;

import org.fluxtream.core.services.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.fluxtream.core.domain.Guest;

@Service
public class FlxUserDetailsService implements UserDetailsService {

	@Autowired
	GuestService guestService;
	
	public UserDetails loadUserByUsername(String usernameOrEmail)
			throws UsernameNotFoundException {
		Guest guest = guestService.getGuest(usernameOrEmail);
		if (guest == null)
			guest = guestService.getGuestByEmail(usernameOrEmail);
		if (guest == null)
			throw new UsernameNotFoundException(usernameOrEmail + " Not Found");
		FlxUserDetails user = new FlxUserDetails(guest);
		return user;
	}

}
