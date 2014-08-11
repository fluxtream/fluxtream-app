package org.fluxtream.core.domain.oauth2;

import org.fluxtream.core.domain.AbstractEntity;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;

/**
 * User: candide
 * Date: 10/04/14
 * Time: 12:47
 */
@Entity(name="AuthorizationCodeResponse")
public class AuthorizationCodeResponse extends AbstractEntity {

    public long guestId;
    public long authorizationCodeId;

    @Type(type="yes_no")
    public boolean granted;

    public AuthorizationCodeResponse() {}

    public AuthorizationCodeResponse(final AuthorizationCode authCode, final long guestId, final boolean granted) {
        authorizationCodeId = authCode.getId();
        this.guestId = guestId;
        this.granted = granted;
    }
}
