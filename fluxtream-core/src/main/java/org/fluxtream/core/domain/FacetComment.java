package org.fluxtream.core.domain;

import org.hibernate.annotations.Type;

import javax.persistence.*;

/**
 * Created by candide on 24/12/14.
 */
@Entity(name="FacetComments")
public class FacetComment extends AbstractEntity {

    @Type(type="yes_no")
    public boolean isNote;
    public long apiKeyId;
    public long facetId;

    public FacetComment() {}
    public FacetComment(long apiKeyId, long facetId, Guest guest, String body, boolean isNote) {
        this.apiKeyId = apiKeyId;
        this.facetId = facetId;
        this.guest = guest;
        this.body = body;
        this.isNote = isNote;
        created = System.currentTimeMillis();
        updated = System.currentTimeMillis();
    }

    @OneToOne(fetch=FetchType.EAGER)
    public Guest guest;

    public String body;

    public long created;
    public long updated;

}
