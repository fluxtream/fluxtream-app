package org.fluxtream.core.domain;

import javax.persistence.*;

/**
 * Created by candide on 24/12/14.
 */
@Entity(name="FacetComments")
public class FacetComment extends AbstractEntity {

    public long apiKeyId;
    public long facetId;

    public FacetComment() {}
    public FacetComment(long apiKeyId, long facetId, Guest guest, String body) {
        this.apiKeyId = apiKeyId;
        this.facetId = facetId;
        this.guest = guest;
        this.body = body;
    }

    @OneToOne(fetch=FetchType.EAGER)
    public Guest guest;

    public String body;

    public long created;
    public long updated;

}
