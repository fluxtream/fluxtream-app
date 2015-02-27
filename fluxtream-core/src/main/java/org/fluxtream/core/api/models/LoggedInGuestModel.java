package org.fluxtream.core.api.models;

import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.GuestDetails;

/**
 * Created by candide on 22/12/14.
 */
public class LoggedInGuestModel extends BasicGuestModel {

    public String access_token;

    public LoggedInGuestModel(Guest guest, GuestDetails details, String accessToken) {
        super(guest, details);
        access_token = accessToken;
    }

}
