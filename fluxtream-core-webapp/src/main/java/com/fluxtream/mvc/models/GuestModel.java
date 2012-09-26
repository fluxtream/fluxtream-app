package com.fluxtream.mvc.models;

import com.fluxtream.domain.Guest;

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
