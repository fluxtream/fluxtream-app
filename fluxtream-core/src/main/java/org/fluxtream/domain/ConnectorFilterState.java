package org.fluxtream.domain;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.annotations.Index;

@Entity(name="ConnectorFilterState")
@NamedQueries({
      @NamedQuery(name="connectorFilterState.delete.all",
                  query="DELETE from ConnectorFilterState state WHERE state.guestId=?"),
      @NamedQuery(name="connectorFilterState",
                  query="SELECT state FROM ConnectorFilterState state WHERE state.guestId=?")
})
public class ConnectorFilterState extends AbstractEntity {

    @Index(name="guestId")
    public long guestId;


    @Lob
    public String stateJSON;

}
