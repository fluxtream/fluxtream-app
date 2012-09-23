package com.fluxtream.services.impl;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.SharedConnector;
import com.fluxtream.domain.SharingBuddy;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SharingService;
import com.fluxtream.utils.JPAUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Service
@Transactional(readOnly=true)
public class SharingServiceImpl implements SharingService {

    @Autowired
    GuestService guestService;

    @PersistenceContext
    EntityManager em;

    @Override
    @Transactional(readOnly=false)
    public void addSharingBuddy(final long guestId, final String username) {
        final Guest buddyGuest = guestService.getGuest(username);
        SharingBuddy buddy = new SharingBuddy();
        buddy.guestId = guestId;
        buddy.buddyId = buddyGuest.getId();
        em.persist(buddy);
    }

    @Override
    @Transactional(readOnly=false)
    public void removeSharingBuddy(final long guestId, final String username) {
        final Guest buddyGuest = guestService.getGuest(username);
        if (buddyGuest==null) return;
        final SharingBuddy sharingBuddy = JPAUtils.findUnique(em, SharingBuddy.class,
                                                              "sharingBuddies.byGuestAndBuddyId",
                                                              guestId, buddyGuest.getId());
        if (sharingBuddy==null) return;
        em.remove(sharingBuddy);
    }

    @Override
    @Transactional(readOnly=false)
    public void addSharedConnector(final long guestId, final String username, final String connectorName, final String filterJson) {
        final Guest buddyGuest = guestService.getGuest(username);
        if (buddyGuest==null) return;
        final SharingBuddy sharingBuddy = JPAUtils.findUnique(em, SharingBuddy.class,
                                                              "sharingBuddies.byGuestAndBuddyId",
                                                              guestId, buddyGuest.getId());
        if (sharingBuddy==null) return;
        for(SharedConnector sharedConnector : sharingBuddy.sharedConnectors) {
            if (sharedConnector.connectorName.equals(connectorName))
                return;
        }
        SharedConnector sharedConnector = new SharedConnector();
        sharedConnector.connectorName = connectorName;
        sharedConnector.filterJson = filterJson;
        sharingBuddy.sharedConnectors.add(sharedConnector);
        sharedConnector.buddy = sharingBuddy;
        em.persist(sharedConnector);
        em.merge(sharingBuddy);
    }

    @Override
    @Transactional(readOnly=false)
    public void removeSharedConnector(final long guestId, final String username, final String connectorName) {
        final Guest buddyGuest = guestService.getGuest(username);
        if (buddyGuest==null) return;
        final SharingBuddy sharingBuddy = JPAUtils.findUnique(em, SharingBuddy.class,
                                                              "sharingBuddies.byGuestAndBuddyId",
                                                              guestId, buddyGuest.getId());
        if (sharingBuddy==null) return;
        SharedConnector toRemove = null;
        for(SharedConnector sharedConnector : sharingBuddy.sharedConnectors) {
            if (sharedConnector.connectorName.equals(connectorName)) {
                toRemove = sharedConnector;
                break;
            }
        }
        if (toRemove!=null) {
            sharingBuddy.sharedConnectors.remove(toRemove);
            toRemove.buddy = null;
            em.remove(toRemove);
            em.merge(sharingBuddy);
        }
    }

    @Override
    public List<Guest> getBuddies(final long guestId) {
        final List<SharingBuddy> sharingBuddies = JPAUtils.find(em, SharingBuddy.class, "sharingBuddies.byGuestId", guestId);
        final List<Guest> buddies = new ArrayList<Guest>();
        for (SharingBuddy sharingBuddy : sharingBuddies) {
            final Guest buddyGuest = guestService.getGuestById(sharingBuddy.buddyId);
            buddies.add(buddyGuest);
        }
        return buddies;
    }

    @Override
    public SharingBuddy getSharingBuddy(final long guestId, final String username) {
        final Guest buddyGuest = guestService.getGuest(username);
        if (buddyGuest==null) return null;
        final SharingBuddy sharingBuddy = JPAUtils.findUnique(em, SharingBuddy.class,
                                                              "sharingBuddies.byGuestAndBuddyId",
                                                              guestId, buddyGuest.getId());
        return sharingBuddy;
    }
}
