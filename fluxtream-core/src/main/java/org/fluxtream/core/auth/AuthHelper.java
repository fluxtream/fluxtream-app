package org.fluxtream.core.auth;

import org.apache.commons.lang.StringUtils;
import org.fluxtream.core.domain.TrustedBuddy;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.BuddiesService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class AuthHelper {

    private static Map<Long,Set<TrustedBuddy>> viewees = new Hashtable<Long,Set<TrustedBuddy>>();

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
        if (principal.trustedBuddy ==null)
            return true;
        else {
            return buddiesService.isViewingGranted(principal.guestId, principal.trustedBuddy.guestId, connectorName);
        }
    }

    public static void as(TrustedBuddy trustedBuddy) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
        addViewee(principal.guestId, trustedBuddy);
        principal.trustedBuddy = trustedBuddy;
    }

    private static void addViewee(final Long id, final TrustedBuddy trustedBuddy) {
        if (viewees.get(id)==null);
            viewees.put(id, new HashSet<TrustedBuddy>());
        if (!viewees.get(id).contains(trustedBuddy))
            viewees.get(id).add(trustedBuddy);
    }

    /**
     * This is called by coachingService when a trustingBuddy no longer wants to be coached by
     * some coach
     * @param id coach id
     * @param trustedBuddy The user who just revoked the coach
     */
    public static void revokeCoach(final Long id, final TrustedBuddy trustedBuddy) {
        final Set<TrustedBuddy> buddies = viewees.get(id);
        if (buddies==null)
            return;
        TrustedBuddy toRemove = null;
        for (TrustedBuddy buddy : buddies) {
            if (buddy==null) continue;
            if (buddy.getId()== trustedBuddy.getId()) {
                toRemove = buddy;
                break;
            }
        }
        buddies.remove(toRemove);
    }

    public static long getVieweeId() throws TrustRelationshipRevokedException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
        if (principal.trustedBuddy ==null)
            return principal.guestId;
        else {
            final Set<TrustedBuddy> trustedBuddies = viewees.get(principal.guestId);
            if (trustedBuddies.contains(principal.trustedBuddy))
                return principal.trustedBuddy.guestId;
            else {
                principal.trustedBuddy = null;
                throw new TrustRelationshipRevokedException();
            }
        }
    }

    public static TrustedBuddy getTrustedBuddy(String buddyToAccessParameter, BuddiesService buddiesService) throws TrustRelationshipRevokedException {
        if (buddyToAccessParameter==null || buddyToAccessParameter!=null&&buddyToAccessParameter.equals("self")) {
            as(null);
            return null;
        } else if (buddyToAccessParameter !=null&&!buddyToAccessParameter.equals("self")) {
            TrustedBuddy trustedBuddy;
            if (StringUtils.isNumeric(buddyToAccessParameter)) {
                final Long trustedBuddyId = Long.valueOf(buddyToAccessParameter, 10);
                if (trustedBuddyId==AuthHelper.getGuestId())
                    return null;
                trustedBuddy = buddiesService.getTrustedBuddy(getGuestId(), trustedBuddyId);
            } else
                trustedBuddy = buddiesService.getTrustedBuddy(getGuestId(), buddyToAccessParameter);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
            if (trustedBuddy !=null) {
                addViewee(principal.guestId, trustedBuddy);
                principal.trustedBuddy = trustedBuddy;
                return trustedBuddy;
            }
            else {
                principal.trustedBuddy = null;
                throw new TrustRelationshipRevokedException();
            }
        } else return getTrustedBuddy();
    }

    public static TrustedBuddy getTrustedBuddy() throws TrustRelationshipRevokedException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
        if (principal.trustedBuddy !=null && viewees.size()>0)
            if (viewees.get(principal.guestId).contains(principal.trustedBuddy))
                return principal.trustedBuddy;
            else {
                principal.trustedBuddy = null;
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
