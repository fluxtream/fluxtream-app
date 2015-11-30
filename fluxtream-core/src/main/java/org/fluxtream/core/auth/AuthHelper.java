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

    public static boolean isViewingGranted(String connectorName, long buddyId, BuddiesService buddiesService) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
        if (principal.guestId==buddyId) return true;
        return buddiesService.isViewingGranted(principal.guestId, buddyId, connectorName);
    }

    public static TrustedBuddy getBuddyTrustedBuddy(String buddyToAccessParameter, BuddiesService buddiesService) throws TrustRelationshipRevokedException {
        if (buddyToAccessParameter==null || buddyToAccessParameter!=null&&buddyToAccessParameter.equals("self")) {
            return null;
        } else {
            TrustedBuddy trustedBuddy;
            if (StringUtils.isNumeric(buddyToAccessParameter)) {
                final Long trustedBuddyId = Long.valueOf(buddyToAccessParameter, 10);
                if (trustedBuddyId==AuthHelper.getGuestId())
                    return null;
                trustedBuddy = buddiesService.getTrustedBuddy(getGuestId(), trustedBuddyId);
            } else
                trustedBuddy = buddiesService.getTrustedBuddy(getGuestId(), buddyToAccessParameter);
            if (trustedBuddy !=null)
                return trustedBuddy;
            else
                throw new TrustRelationshipRevokedException();
        }
    }

    public static TrustedBuddy getOwnTrustedBuddy(String buddyToAccessParameter, BuddiesService buddiesService) throws TrustRelationshipRevokedException {
        if (buddyToAccessParameter==null || buddyToAccessParameter!=null&&buddyToAccessParameter.equals("self")) {
            return null;
        } else {
            TrustedBuddy trustedBuddy = null;
            if (StringUtils.isNumeric(buddyToAccessParameter)) {
                final Long trustedBuddyId = Long.valueOf(buddyToAccessParameter, 10);
                if (trustedBuddyId==AuthHelper.getGuestId())
                    return null;
                trustedBuddy = buddiesService.getTrustedBuddy(trustedBuddyId, getGuestId());
            }
            if (trustedBuddy !=null)
                return trustedBuddy;
            else
                throw new TrustRelationshipRevokedException();
        }
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
