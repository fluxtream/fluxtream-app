package org.fluxtream.core.domain;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.annotations.Index;

@Entity(name="GrapherView")
@NamedQueries({
      @NamedQuery(name="grapherView.delete.all",
                  query="DELETE FROM GrapherView grapherView WHERE grapherView.guestId=? "),
      @NamedQuery(name="grapherView",
                  query="SELECT view FROM GrapherView view WHERE view.guestId=?"),
      @NamedQuery(name="grapherView.byName",
                  query="SELECT view FROM GrapherView view WHERE view.guestId=? AND view.name=?"),
      @NamedQuery(name="grapherView.byId",
              query="SELECT view FROM GrapherView view WHERE view.guestId=? AND view.id=?")
})
public class GrapherView extends AbstractEntity {

    public GrapherView(){}

    @Index(name="guestId")
    public long guestId;

    @Index(name="name")
    public String name;

    public long lastUsed;

    @Lob
    public String json;
}