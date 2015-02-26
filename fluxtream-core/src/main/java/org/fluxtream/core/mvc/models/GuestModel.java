package org.fluxtream.core.mvc.models;

import org.fluxtream.core.domain.Guest;

import static org.fluxtream.core.utils.Utils.hash;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class GuestModel {

    public String fullname;
    public String username;
    public String photoURL;

    public GuestModel(Guest guest) {
        this.fullname = guest.getGuestName();
        this.username = guest.username;
        String emailHash = hash(guest.email.toLowerCase().trim()); //gravatar specifies the email should be trimmed, taken to lowercase, and then MD5 hashed
        this.photoURL = String.format("http://www.gravatar.com/avatar/%s?s=256&d=retro", emailHash);
    }

}
