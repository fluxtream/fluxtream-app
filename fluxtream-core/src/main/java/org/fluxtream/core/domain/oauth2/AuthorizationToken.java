package org.fluxtream.core.domain.oauth2;

import org.fluxtream.core.domain.AbstractEntity;
import org.hibernate.annotations.Index;
import org.joda.time.DateTime;

import javax.persistence.Entity;
import java.util.UUID;

/**
 * User: candide
 * Date: 10/04/14
 * Time: 12:57
 */
@Entity(name="AuthorizationToken")
public class AuthorizationToken extends AbstractEntity {

    /**
     * The default number of milliseconds that a token should live.
     */
    public static final long DEFAULT_TOKEN_LIFETIME_MILLIS = 1000 * 60 * 60;

    @Index(name="guestId")
    public long guestId;

    @Index(name="authorizationCodeId")
    public long authorizationCodeId;

    @Index(name="accessToken")
    public String accessToken;

    @Index(name="refreshToken")
    public String refreshToken;
    public long expirationTime;
    public long creationTime;

    public AuthorizationToken() {}

    public AuthorizationToken(final AuthorizationCodeResponse response) {
        // Validate the parameters.
        if(response == null) {
            throw new RuntimeException("The response is null.");
        }
        else if(! response.granted) {
            throw new RuntimeException(
                    "An authorization token cannot be created for an " +
                    "authorization code that was denied.");
        }

        // Store the relevant information.
        this.authorizationCodeId = response.authorizationCodeId;
        this.accessToken = UUID.randomUUID().toString();
        this.refreshToken = UUID.randomUUID().toString();
        this.creationTime = DateTime.now().getMillis();
        this.guestId = response.guestId;
        this.expirationTime =
                this.creationTime + DEFAULT_TOKEN_LIFETIME_MILLIS;
    }

    public AuthorizationToken(final AuthorizationToken oldToken) {
        this.authorizationCodeId = oldToken.authorizationCodeId;
        this.guestId = oldToken.guestId;
        this.accessToken = UUID.randomUUID().toString();
        this.refreshToken = UUID.randomUUID().toString();
        this.creationTime = DateTime.now().getMillis();
        this.expirationTime =
                this.creationTime + DEFAULT_TOKEN_LIFETIME_MILLIS;
    }

    /**
     * Returns the number of milliseconds before the access token expires.
     *
     * @return The number of milliseconds before the access token expires. This
     *         may be negative if the token has already expired.
     */
    public long getExpirationIn() {
        return expirationTime - DateTime.now().getMillis();
    }

}
