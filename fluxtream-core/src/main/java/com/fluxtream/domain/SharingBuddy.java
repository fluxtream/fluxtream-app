package com.fluxtream.domain;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import org.hibernate.annotations.Index;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Entity(name = "SharingBuddies")
@NamedQueries({
      @NamedQuery(name = "sharingBuddies.byGuestId",
                  query = "SELECT buddy FROM SharingBuddies buddy WHERE buddy.guestId=?"),
      @NamedQuery(name = "sharingBuddies.byGuestAndBuddyId",
                  query = "SELECT buddy FROM SharingBuddies buddy WHERE buddy.guestId=? AND buddy.buddyId=?"),
      @NamedQuery(name = "sharingBuddies.delete.all",
                  query = "DELETE FROM SharingBuddies buddy WHERE buddy.guestId=?")
})
public class SharingBuddy extends AbstractEntity {

    @Index(name = "guestId")
    public long guestId;

    @Index(name = "buddyId")
    public long buddyId;

    @OneToMany(mappedBy="buddy", fetch= FetchType.EAGER, cascade= CascadeType.ALL)
    public Set<SharedConnector> sharedConnectors = new HashSet<SharedConnector>();


}
