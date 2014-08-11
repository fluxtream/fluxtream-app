package org.fluxtream.core.api;

import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.CoachingBuddy;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.GuestService;

/**
 * User: candide
 * Date: 09/07/14
 * Time: 12:05
 */
public class ApiHelper {

    static Guest getBuddyToAccess(GuestService guestService, CoachingBuddy coachee) {
        Guest guest = AuthHelper.getGuest();
        if (guest==null)
            return null;
        if (coachee!=null)
            return guestService.getGuestById(coachee.guestId);
        return guest;
    }

}
