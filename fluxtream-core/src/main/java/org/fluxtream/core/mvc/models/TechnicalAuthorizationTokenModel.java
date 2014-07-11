package org.fluxtream.core.mvc.models;

import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.oauth2.AuthorizationToken;

/**
 * User: candide
 * Date: 11/07/14
 * Time: 13:00
 */
public class TechnicalAuthorizationTokenModel {

    public String access_token;
    public String refresh_token;
    public long expires;
    public long guestId;
    public String username;

    public TechnicalAuthorizationTokenModel() {}

    public TechnicalAuthorizationTokenModel(AuthorizationToken authorizationToken, Guest guest) {
        this.access_token = authorizationToken.accessToken;
        this.refresh_token = authorizationToken.refreshToken;
        this.expires = authorizationToken.expirationTime;
        this.guestId = guest.getId();
        this.username = guest.username;
    }
}
