package org.fluxtream.core.domain.oauth2;

import org.fluxtream.core.domain.AbstractEntity;
import org.hibernate.annotations.Index;

import javax.persistence.Entity;
import javax.persistence.Lob;
import java.util.UUID;

/**
 * User: candide
 * Date: 10/04/14
 * Time: 11:55
 */
@Entity(name="Application")
public class Application extends AbstractEntity {

    @Index(name="guestId")
    public long guestId;
    @Index(name="uid")
    public String uid;
    public String sharedSecret;
    public String name;

    @Lob
    public String description;

    public Application(){}

    public Application(final long guestId, final String name, final String description) {
        this.guestId = guestId;
        this.name = name;
        this.description = description;
        this.uid = UUID.randomUUID().toString();
        this.sharedSecret = UUID.randomUUID().toString();
    }

}
