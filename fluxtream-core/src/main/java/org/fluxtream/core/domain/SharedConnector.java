package org.fluxtream.core.domain;

import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.io.Serializable;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name = "SharedConnectors")
@NamedQueries({
    @NamedQuery (name="sharedConnector.byTrustedBuddyId", query="SELECT sconn FROM SharedConnectors sconn WHERE sconn.buddy.guestId=? AND sconn.buddy.buddyId=?"),
    @NamedQuery (name="sharedConnector.byConnectorNameAndViewerId", query="SELECT sconn FROM SharedConnectors sconn WHERE sconn.connectorName=? AND sconn.buddy.buddyId=?"),
    @NamedQuery (name="sharedConnector.byConnectorNameAndVieweeId", query="SELECT sconn FROM SharedConnectors sconn WHERE sconn.connectorName=? AND sconn.buddy.guestId=?"),
    @NamedQuery (name="sharedConnectors.delete.all", query="DELETE FROM SharedConnectors sharedConnector WHERE sharedConnector.buddy.guestId=? OR sharedConnector.buddy.buddyId=?")
})
public class SharedConnector extends AbstractEntity implements Serializable {

    @ManyToOne
    public TrustedBuddy buddy;

    @Index(name = "connectorName")
    public String connectorName;

    @Lob
    public String filterJson;

}
