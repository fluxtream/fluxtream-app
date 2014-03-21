package org.fluxtream.domain;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.annotations.Index;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name = "SharedConnectors")
@NamedQueries({
    @NamedQuery (name="sharedConnector.byConnectorNameAndViewerId", query="SELECT sconn FROM SharedConnectors sconn WHERE sconn.connectorName=? AND sconn.buddy.buddyId=?"),
    @NamedQuery (name="sharedConnector.byConnectorNameAndVieweeId", query="SELECT sconn FROM SharedConnectors sconn WHERE sconn.connectorName=? AND sconn.buddy.guestId=?")
})
public class SharedConnector extends AbstractEntity implements Serializable {

    @ManyToOne
    public CoachingBuddy buddy;

    @Index(name = "connectorName")
    public String connectorName;

    @Lob
    public String filterJson;

}
