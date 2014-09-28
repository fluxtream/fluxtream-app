package org.fluxtream.core.domain.oauth2;

import org.apache.commons.lang.StringUtils;
import org.fluxtream.core.domain.AbstractEntity;
import org.hibernate.annotations.Index;

import javax.persistence.Entity;
import javax.persistence.Lob;
import java.util.Set;
import java.util.UUID;

/**
 * User: candide
 * Date: 10/04/14
 * Time: 12:04
 */
@Entity(name="AuthorizationCode")
public class AuthorizationCode extends AbstractEntity {

    /**
     * The default number of milliseconds that a token should live.
     */
    public static final long DEFAULT_CODE_LIFETIME_MILLIS = 1000 * 60 * 5;

    @Index(name="code")
    public String code;

    @Index(name="applicationId")
    public long applicationId;
    public long creationTime;
    public long expirationTime;

    @Lob
    public String scopes;
    public String state;

    public AuthorizationCode() {}

    public AuthorizationCode(final Long id, final Set<String> scopes, final String state) {
        if (scopes!=null)
            this.scopes = StringUtils.join(scopes, ",");
        this.code = UUID.randomUUID().toString();
        this.applicationId = id;
        this.state = state;
        this.creationTime = System.currentTimeMillis();
        this.expirationTime = this.creationTime + DEFAULT_CODE_LIFETIME_MILLIS;
    }
}
