package com.fluxtream.updaters.quartz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.fluxtream.auth.FlxUserDetails;
import com.fluxtream.domain.Guest;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class Producer {

    @Qualifier("connectorUpdateServiceImpl")
    @Autowired
    private ConnectorUpdateService connectorUpdateService;

    @Qualifier("guestServiceImpl")
    @Autowired
    private GuestService guestService;

    public void scheduleIncrementalUpdates() {
        List<String> roles = new ArrayList<String>();
        roles.add("ROLE_ADMIN");
        roles.add("ROLE_ROOT");
        as(roles);
        try {
            List<Guest> guests = guestService.getAllGuests();
            for (Guest g : guests) {
                //connectorUpdateService.updateAllConnectors(g.getId());
            }
        }
        catch (Exception e) {
            System.out.println(e.getClass());
        }
    }

    private static void as(final List<String> roles) {
    	@SuppressWarnings("serial") Authentication auth = new Authentication() {

    		@Override
    		public String getName() {
    			return null;
    		}

    		@Override
    		public void setAuthenticated(boolean isAuthenticated)
    				throws IllegalArgumentException {
    		}

    		@Override
    		public boolean isAuthenticated() {
    			return true;
    		}

    		@Override
    		public Object getPrincipal() {
    			Guest guest = new Guest();
    			return new FlxUserDetails(guest);
    		}

    		@Override
    		public Object getDetails() {
    			return null;
    		}

    		@Override
    		public Object getCredentials() {
    			return null;
    		}

    		@Override
    		public Collection<GrantedAuthority> getAuthorities() {
    			List<GrantedAuthority> l = new ArrayList<GrantedAuthority>();
    			for (String role : roles) l.add(new SimpleGrantedAuthority(role));
    			return l;
    		}
    	};
    	SecurityContextHolder.getContext().setAuthentication(auth);
    }

}