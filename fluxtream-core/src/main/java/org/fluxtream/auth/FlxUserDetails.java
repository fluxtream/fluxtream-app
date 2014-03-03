package org.fluxtream.auth;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.fluxtream.domain.CoachingBuddy;
import org.fluxtream.domain.Guest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@SuppressWarnings("serial")
public class FlxUserDetails implements UserDetails, Serializable {

	private Guest guest;
    public long guestId;
    public CoachingBuddy coachee;
	
	public FlxUserDetails(Guest guest) {
		this.guest = guest;
        this.guestId = guest.getId();
	}
	
	public Guest getGuest() {
		return this.guest;
	}
	
	public boolean isEnabled() {
		return true;
	}
	
	public boolean isCredentialsNonExpired() {
		return true;
	}
	
	public boolean isAccountNonLocked() {
		return true;
	}
	
	public boolean isAccountNonExpired() {
		return true;
	}
	
	public String getUsername() {
		return guest!=null?guest.username:null;
	}
	
	public String getSalt() {
		return guest!=null?guest.salt:null;
	}
	
	public String getPassword() {
		return guest!=null?guest.password:null;
	}
	
	public Collection<GrantedAuthority> getAuthorities() {
		Collection<GrantedAuthority> result = new ArrayList<GrantedAuthority>();
		List<String> userRoles = guest.getUserRoles();
 		for (String userRole : userRoles)
			result.add(new SimpleGrantedAuthority(userRole));
		return result;
	}

}
