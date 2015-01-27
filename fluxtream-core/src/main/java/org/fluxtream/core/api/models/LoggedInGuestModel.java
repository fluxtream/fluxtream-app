package org.fluxtream.core.api.models;

import org.fluxtream.core.domain.Guest;

/**
 * Created by candide on 22/12/14.
 */
public class LoggedInGuestModel extends BasicGuestModel {

    public String access_token;

    public LoggedInGuestModel(Guest guest, String accessToken) {
        super(guest);
        access_token = accessToken;
    }

}
