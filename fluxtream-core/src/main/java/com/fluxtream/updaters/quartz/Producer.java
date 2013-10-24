package com.fluxtream.updaters.quartz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.auth.FlxUserDetails;
import com.fluxtream.domain.Guest;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * This class' scheduleIncrementalUpdates is meant to be call at regular intervals by
 * a quartz trigger (see spring-quartz.xml)
 */
public class Producer {

    FlxLogger logger = FlxLogger.getLogger(Producer.class);

    @Autowired
    private ConnectorUpdateService connectorUpdateService;

    @Autowired
    private GuestService guestService;

    private boolean contextStarted = false;

    public void setContextStarted() {
        contextStarted = true;
    }

    /**
     * bluntly go through the list of all guests and attempt to update all of their connectors
     */
    public void scheduleIncrementalUpdates() throws InterruptedException {
        while (!contextStarted) {
            Thread.sleep(1000);
            System.out.println("Context not started, delaying queue consumption...");
        }
        logger.debug("module=updateQueue component=producer action=scheduleIncrementalUpdates");
        List<String> roles = new ArrayList<String>();
        roles.add("ROLE_ADMIN");
        roles.add("ROLE_ROOT");
        as(roles);
        try {
            List<Guest> guests = guestService.getAllGuests();
            for (Guest g : guests) {
                connectorUpdateService.updateAllConnectors(g.getId(), false);
            }
        }
        catch (Exception e) {
            String stackTrace = Utils.stackTrace(e);
            logger.error("module=updateQueue component=producer message=Could not update all connectors stackTrace=" + stackTrace);
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