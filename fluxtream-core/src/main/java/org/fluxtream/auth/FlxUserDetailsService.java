package org.fluxtream.auth;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.fluxtream.domain.Guest;
import org.fluxtream.utils.JPAUtils;

@Service
public class FlxUserDetailsService implements UserDetailsService {

	@PersistenceContext
	EntityManager em;
	
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException {
		final Guest guest = JPAUtils.findUnique(em, Guest.class,
				"guest.byUsername", username);
		if (guest == null)
			throw new UsernameNotFoundException(username + " Not Found");
		FlxUserDetails user = new FlxUserDetails(guest);
		return user;
	}

}
