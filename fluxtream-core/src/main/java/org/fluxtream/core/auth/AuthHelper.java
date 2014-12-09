package org.fluxtream.core.auth;

import org.apache.commons.lang.StringUtils;
import org.fluxtream.core.domain.TrustingBuddy;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.BuddiesService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class AuthHelper {

    private static Map<Long,Set<TrustingBuddy>> viewees = new Hashtable<Long,Set<TrustingBuddy>>();

	public static long getGuestId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		long guestId = ((FlxUserDetails)auth.getPrincipal()).guestId;
		return guestId;
	}

    public static boolean isFullyAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();
        return (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof FlxUserDetails);
    }

    public static boolean isViewingGranted(String connectorName, BuddiesService buddiesService) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
        if (principal.trustingBuddy==null)
            return true;
        else {
            return buddiesService.isViewingGranted(principal.guestId, principal.trustingBuddy.guestId, connectorName);
        }
    }

    public static void as(TrustingBuddy trustingBuddy) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
        addViewee(principal.guestId, trustingBuddy);
        principal.trustingBuddy = trustingBuddy;
    }

    private static void addViewee(final Long id, final TrustingBuddy trustingBuddy) {
        if (viewees.get(id)==null);
            viewees.put(id, new HashSet<TrustingBuddy>());
        if (!viewees.get(id).contains(trustingBuddy))
            viewees.get(id).add(trustingBuddy);
    }

    /**
     * This is called by coachingService when a trustingBuddy no longer wants to be coached by
     * some coach
     * @param id coach id
     * @param trustingBuddy The user who just revoked the coach
     */
    public static void revokeCoach(final Long id, final TrustingBuddy trustingBuddy) {
        final Set<TrustingBuddy> buddies = viewees.get(id);
        if (buddies==null)
            return;
        TrustingBuddy toRemove = null;
        for (TrustingBuddy buddy : buddies) {
            if (buddy==null) continue;
            if (buddy.getId()==trustingBuddy.getId()) {
                toRemove = buddy;
                break;
            }
        }
        buddies.remove(toRemove);
    }

    public static long getVieweeId() throws TrustRelationshipRevokedException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
        if (principal.trustingBuddy==null)
            return principal.guestId;
        else {
            final Set<TrustingBuddy> gueststrustingBuddies = viewees.get(principal.guestId);
            if (gueststrustingBuddies.contains(principal.trustingBuddy))
                return principal.trustingBuddy.guestId;
            else {
                principal.trustingBuddy = null;
                throw new TrustRelationshipRevokedException();
            }
        }
    }

    public static TrustingBuddy getTrustingBuddy(String buddyToAccessParameter, BuddiesService buddiesService) throws TrustRelationshipRevokedException {
        if (buddyToAccessParameter==null || buddyToAccessParameter!=null&&buddyToAccessParameter.equals("self")) {
            as(null);
            return null;
        } else if (buddyToAccessParameter !=null&&!buddyToAccessParameter.equals("self")) {
            TrustingBuddy trustingBuddy;
            if (StringUtils.isNumeric(buddyToAccessParameter)) {
                final Long trustingBuddyId = Long.valueOf(buddyToAccessParameter, 10);
                if (trustingBuddyId==AuthHelper.getGuestId())
                    return null;
                trustingBuddy = buddiesService.getTrustingBuddy(getGuestId(), trustingBuddyId);
            } else
                trustingBuddy = buddiesService.getTrustingBuddy(getGuestId(), buddyToAccessParameter);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
            if (trustingBuddy!=null) {
                addViewee(principal.guestId, trustingBuddy);
                principal.trustingBuddy = trustingBuddy;
                return trustingBuddy;
            }
            else {
                principal.trustingBuddy = null;
                throw new TrustRelationshipRevokedException();
            }
        } else return getTrustingBuddy();
    }

    public static TrustingBuddy getTrustingBuddy() throws TrustRelationshipRevokedException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
        if (principal.trustingBuddy!=null && viewees.size()>0)
            if (viewees.get(principal.guestId).contains(principal.trustingBuddy))
                return principal.trustingBuddy;
            else {
                principal.trustingBuddy = null;
                throw new TrustRelationshipRevokedException();
            }
        return null;
    }

	public static Guest getGuest() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth==null)
			return null;
        final Object authPrincipal = auth.getPrincipal();
        if (authPrincipal instanceof FlxUserDetails) {
            final FlxUserDetails principal = (FlxUserDetails) authPrincipal;
            Guest guest = principal.getGuest();
            // set the guest's ID in case we got an instance that was deserialized from
            // disk (in which case it will be null)
            guest.setId(principal.guestId);
            return guest;
        } else return null;
	}
}
