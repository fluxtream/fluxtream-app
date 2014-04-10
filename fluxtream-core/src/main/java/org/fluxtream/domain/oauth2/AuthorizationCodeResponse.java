package org.fluxtream.domain.oauth2;

import javax.persistence.Entity;
import org.fluxtream.domain.AbstractEntity;

/**
 * User: candide
 * Date: 10/04/14
 * Time: 12:47
 */
@Entity(name="AuthorizationCodeResponse")
public class AuthorizationCodeResponse extends AbstractEntity {

    public long guestId;
    public long authorizationCodeId;
    public boolean granted;

    public AuthorizationCodeResponse() {}

    public AuthorizationCodeResponse(final AuthorizationCode authCode, final long guestId, final boolean granted) {
        authorizationCodeId = authCode.getId();
        this.guestId = guestId;
        this.granted = granted;
    }
}
