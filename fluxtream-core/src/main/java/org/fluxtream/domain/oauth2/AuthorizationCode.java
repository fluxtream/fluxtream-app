package org.fluxtream.domain.oauth2;

import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Lob;
import org.apache.commons.lang.StringUtils;
import org.fluxtream.domain.AbstractEntity;
import org.hibernate.annotations.Index;

/**
 * User: candide
 * Date: 10/04/14
 * Time: 12:04
 */
@Entity(name="AuthorizationCode")
public class AuthorizationCode extends AbstractEntity {

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
        this.scopes = StringUtils.join(scopes, ",");
        this.code = UUID.randomUUID().toString();
        this.applicationId = id;
        this.state = state;
        this.creationTime = System.currentTimeMillis();
    }
}
