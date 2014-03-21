package org.fluxtream.mvc.models;

import org.fluxtream.domain.Guest;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class GuestModel {

    public String fullname;
    public String username;

    public GuestModel(Guest guest) {
        this.fullname = guest.getGuestName();
        this.username = guest.username;
    }

}
