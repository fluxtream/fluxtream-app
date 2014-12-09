package org.fluxtream.core.api;

import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.TrustingBuddy;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.GuestService;

/**
 * User: candide
 * Date: 09/07/14
 * Time: 12:05
 */
public class ApiHelper {

    static Guest getBuddyToAccess(GuestService guestService, TrustingBuddy trustingBuddy) {
        Guest guest = AuthHelper.getGuest();
        if (guest==null)
            return null;
        if (trustingBuddy!=null)
            return guestService.getGuestById(trustingBuddy.guestId);
        return guest;
    }

}
