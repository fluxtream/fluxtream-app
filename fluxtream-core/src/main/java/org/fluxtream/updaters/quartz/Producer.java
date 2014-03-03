package org.fluxtream.updaters.quartz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.fluxtream.Configuration;
import org.fluxtream.aspects.FlxLogger;
import org.fluxtream.auth.FlxUserDetails;
import org.fluxtream.domain.Guest;
import org.fluxtream.services.ConnectorUpdateService;
import org.fluxtream.services.GuestService;
import org.fluxtream.utils.Utils;
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

    @Autowired
    private Configuration env;

    private boolean contextStarted = false;

    public void setContextStarted() {
        contextStarted = true;
    }

    /**
     * bluntly go through the list of all guests and attempt to update all of their connectors
     * spacing them evenly around 3/4 of producer.trigger.repeatInterval so they don't all happen at once.
     * The reason to use only 3/4 of the producer.trigger.repeatInterval is to allow the later users' connectors
     * some time to complete before the next time scheduleIncrementalUpdates is called.
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
            // Prepare to calculate when to start the connector updates for each guest.
            // updateTimespan is the span of time over which to space the guests' updates
            // guestUpdateSpacing is updateTimespan/# guests, for how much to increment the
            //    time for each guest
            // nextUpdateTime is the time to update the next guest.  It starts at now,
            // and is incremented for each guest
            String producerRepeatInterval = env.get("producer.trigger.repeatInterval");
            long updateTimespan = (producerRepeatInterval!=null)?((long)(Double.valueOf(producerRepeatInterval)*0.75)):0;
            long guestUpdateSpacing = (guests.size()>0)?(updateTimespan/guests.size()):0;
            long nextUpdateTime = System.currentTimeMillis();
            for (Guest g : guests) {
                connectorUpdateService.updateAllConnectors(g.getId(), false, nextUpdateTime);
                nextUpdateTime += guestUpdateSpacing;
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