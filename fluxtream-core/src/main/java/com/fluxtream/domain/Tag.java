package com.fluxtream.domain;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.annotations.Index;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name = "Tags")
@NamedQueries({
  @NamedQuery(name = "tags.all", query = "SELECT tag "
                                       + "FROM Tags tag "
                                       + "WHERE tag.guestId=?"),
  @NamedQuery(name = "tags.byName", query = "SELECT tag "
                                          + "FROM Tags tag "
                                          + "WHERE tag.guestId=? "
                                          + "AND tag.name=?"),
  @NamedQuery(name = "tags.delete.all",
              query = "DELETE FROM Tags tag WHERE tag.guestId=?")
})
public class Tag extends AbstractEntity {

    public Tag() {}

    public long guestId;

    @Index(name = "name")
    public String name;
}
