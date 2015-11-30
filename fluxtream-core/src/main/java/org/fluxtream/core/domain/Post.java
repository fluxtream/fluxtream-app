package org.fluxtream.core.domain;

import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by candide on 15/01/15.
 */
@Entity(name = "Post")
@NamedQueries({
        @NamedQuery(name = "posts.byGuestId",
                query = "SELECT post from Post post WHERE post.fromGuestId=? OR post.toGuestId=?")
})
public class Post extends AbstractEntity {

    @Index(name = "lastUpdateTime")
    public long lastUpdateTime;

    @Index(name = "creationTime")
    public long creationTime;

    public long fromGuestId, toGuestId;

    @Lob
    public String body;

    @OneToMany(mappedBy="post", orphanRemoval = true, fetch= FetchType.EAGER, cascade= CascadeType.ALL)
    public List<PostComment> comments = new ArrayList<PostComment>();

    public Post() {}

}
