package org.fluxtream.core.domain;

import org.hibernate.annotations.Index;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

/**
 * Created by candide on 15/01/15.
 */
@Entity(name = "PostComment")
public class PostComment extends AbstractEntity {

    @Index(name = "lastUpdateTime")
    public long lastUpdateTime;

    @Index(name = "creationTime")
    public long creationTime;

    @Lob
    public String body;

    public long fromGuestId, toGuestId;

    @ManyToOne
    public Post post;

    public PostComment() {}

}