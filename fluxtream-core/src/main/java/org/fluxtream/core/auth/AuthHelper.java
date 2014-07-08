package org.fluxtream.core.auth;

import org.fluxtream.core.domain.CoachingBuddy;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.CoachingService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class AuthHelper {

    private static Map<Long,Set<CoachingBuddy>> viewees = new Hashtable<Long,Set<CoachingBuddy>>();

	public static long getGuestId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		long guestId = ((FlxUserDetails)auth.getPrincipal()).guestId;
		return guestId;
	}

    public static boolean isViewingGranted(String connectorName, CoachingService coachingService) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
        if (principal.coachee==null)
            return true;
        else {
            return coachingService.isViewingGranted(principal.guestId, principal.coachee.guestId, connectorName);
        }
    }

    public static void as(CoachingBuddy coachee) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
        addViewee(principal.guestId, coachee);
        principal.coachee = coachee;
    }

    private static void addViewee(final Long id, final CoachingBuddy coachee) {
        if (viewees.get(id)==null);
            viewees.put(id, new HashSet<CoachingBuddy>());
        if (!viewees.get(id).contains(coachee))
            viewees.get(id).add(coachee);
    }

    /**
     * This is called by coachingService when a coachee no longer wants to be coached by
     * some coach
     * @param id coach id
     * @param coachee The user who just revoked the coach
     */
    public static void revokeCoach(final Long id, final CoachingBuddy coachee) {
        final Set<CoachingBuddy> buddies = viewees.get(id);
        if (buddies==null)
            return;
        CoachingBuddy toRemove = null;
        for (CoachingBuddy buddy : buddies) {
            if (buddy.getId()==coachee.getId()) {
                toRemove = buddy;
                break;
            }
        }
        buddies.remove(toRemove);
    }

    public static long getVieweeId() throws CoachRevokedException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
        if (principal.coachee==null)
            return principal.guestId;
        else {
            final Set<CoachingBuddy> guestsCoachees = viewees.get(principal.guestId);
            if (guestsCoachees.contains(principal.coachee))
                return principal.coachee.guestId;
            else {
                principal.coachee = null;
                throw new CoachRevokedException();
            }
        }
    }

    public static CoachingBuddy getCoachee(String coacheeUsernameHeader, CoachingService coachingService) throws CoachRevokedException {
        if (coacheeUsernameHeader!=null&&coacheeUsernameHeader.equals("self"))
            as(null);
        if (coacheeUsernameHeader !=null&&!coacheeUsernameHeader.equals("self")) {
            final CoachingBuddy coachee = coachingService.getCoachee(getGuestId(), coacheeUsernameHeader);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
            if (coachee!=null) {
                addViewee(principal.guestId, coachee);
                principal.coachee = coachee;
                return coachee;
            }
            else {
                principal.coachee = null;
                throw new CoachRevokedException();
            }
        } else
            return getCoachee();
    }

    public static CoachingBuddy getCoachee() throws CoachRevokedException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
        if (principal.coachee!=null)
            if (viewees.get(principal.guestId).contains(principal.coachee))
                return principal.coachee;
            else {
                principal.coachee = null;
                throw new CoachRevokedException();
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
