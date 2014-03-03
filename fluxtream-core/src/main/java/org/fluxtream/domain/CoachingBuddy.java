package org.fluxtream.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import org.fluxtream.Configuration;
import org.hibernate.annotations.Index;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name = "CoachingBuddies")
@NamedQueries({
      @NamedQuery(name = "coachingBuddies.byGuestId",
                  query = "SELECT buddy FROM CoachingBuddies buddy WHERE buddy.guestId=?"),
      @NamedQuery(name = "coachingBuddies.byBuddyId",
                  query = "SELECT buddy FROM CoachingBuddies buddy WHERE buddy.buddyId=?"),
      @NamedQuery(name = "coachingBuddies.byGuestAndBuddyId",
                  query = "SELECT buddy FROM CoachingBuddies buddy WHERE buddy.guestId=? AND buddy.buddyId=?"),
      @NamedQuery(name = "coachingBuddies.delete.all",
                  query = "DELETE FROM CoachingBuddies buddy WHERE buddy.guestId=?")
})
public class CoachingBuddy extends AbstractEntity implements Serializable {

    @Index(name = "guestId")
    public long guestId;

    @Index(name = "buddyId")
    public long buddyId;

    @OneToMany(mappedBy="buddy", fetch= FetchType.EAGER, cascade= CascadeType.ALL)
    public Set<SharedConnector> sharedConnectors = new HashSet<SharedConnector>();

    public boolean hasAccessToConnector(String connectorName) {
        boolean access = false;
        for (SharedConnector sharedConnector : sharedConnectors) {
            if (sharedConnector.connectorName.equals(connectorName))
                return true;
        }
        return access;
    }

    public boolean hasAccessToDevice(String deviceName, Configuration env) {
        String fluxtreamConnectorName = env.bodytrackToFluxtreamConnectorNames.get(deviceName);
        if (fluxtreamConnectorName==null)
            return true;
        return hasAccessToConnector(fluxtreamConnectorName);
    }

    public boolean equals(Object o) {
        if (!(o instanceof CoachingBuddy))
            return false;
        CoachingBuddy buddy = (CoachingBuddy) o;
        return buddy.guestId == guestId && buddy.buddyId == buddyId;
    }

}
