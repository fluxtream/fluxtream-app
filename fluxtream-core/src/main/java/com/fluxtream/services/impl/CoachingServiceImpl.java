package com.fluxtream.services.impl;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.domain.CoachingBuddy;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.SharedConnector;
import com.fluxtream.services.CoachingService;
import com.fluxtream.services.GuestService;
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
public class CoachingServiceImpl implements CoachingService {

    @Autowired
    GuestService guestService;

    @PersistenceContext
    EntityManager em;

    @Override
    @Transactional(readOnly=false)
    public void addCoach(final long guestId, final String username) {
        final Guest buddyGuest = guestService.getGuest(username);
        CoachingBuddy buddy = new CoachingBuddy();
        buddy.guestId = guestId;
        buddy.buddyId = buddyGuest.getId();
        em.persist(buddy);
    }

    @Override
    @Transactional(readOnly=false)
    public void removeCoach(final long guestId, final String username) {
        final Guest buddyGuest = guestService.getGuest(username);
        if (buddyGuest==null) return;
        final CoachingBuddy coachingBuddy = JPAUtils.findUnique(em, CoachingBuddy.class,
                                                              "coachingBuddies.byGuestAndBuddyId",
                                                              guestId, buddyGuest.getId());
        if (coachingBuddy==null) return;
        AuthHelper.revokeCoach(coachingBuddy.buddyId, coachingBuddy);
        em.remove(coachingBuddy);
    }

    @Override
    @Transactional(readOnly=false)
    public void addSharedConnector(final long guestId, final String username, final String connectorName, final String filterJson) {
        final Guest buddyGuest = guestService.getGuest(username);
        if (buddyGuest==null) return;
        final CoachingBuddy coachingBuddy = JPAUtils.findUnique(em, CoachingBuddy.class,
                                                              "coachingBuddies.byGuestAndBuddyId",
                                                              guestId, buddyGuest.getId());
        if (coachingBuddy==null) return;
        for(SharedConnector sharedConnector : coachingBuddy.sharedConnectors) {
            if (sharedConnector.connectorName.equals(connectorName))
                return;
        }
        SharedConnector sharedConnector = new SharedConnector();
        sharedConnector.connectorName = connectorName;
        sharedConnector.filterJson = filterJson;
        coachingBuddy.sharedConnectors.add(sharedConnector);
        sharedConnector.buddy = coachingBuddy;
        em.persist(sharedConnector);
        em.merge(coachingBuddy);
    }

    @Override
    @Transactional(readOnly=false)
    public void removeSharedConnector(final long guestId, final String username, final String connectorName) {
        final Guest buddyGuest = guestService.getGuest(username);
        if (buddyGuest==null) return;
        final CoachingBuddy coachingBuddy = JPAUtils.findUnique(em, CoachingBuddy.class,
                                                              "coachingBuddies.byGuestAndBuddyId",
                                                              guestId, buddyGuest.getId());
        if (coachingBuddy==null) return;
        SharedConnector toRemove = null;
        for(SharedConnector sharedConnector : coachingBuddy.sharedConnectors) {
            if (sharedConnector.connectorName.equals(connectorName)) {
                toRemove = sharedConnector;
                break;
            }
        }
        if (toRemove!=null) {
            coachingBuddy.sharedConnectors.remove(toRemove);
            toRemove.buddy = null;
            em.remove(toRemove);
            em.merge(coachingBuddy);
        }
    }

    @Override
    public boolean isViewingGranted(final long guestId, final long coacheeId, final String connectorName) {
        final CoachingBuddy coachingBuddy = JPAUtils.findUnique(em, CoachingBuddy.class, "coachingBuddies.byGuestAndBuddyId", coacheeId, guestId);
        boolean granted = coachingBuddy.hasAccessToConnector(connectorName);
        return granted;
    }

    @Override
    public List<Guest> getCoaches(final long guestId) {
        final List<CoachingBuddy> coachingBuddies = JPAUtils.find(em, CoachingBuddy.class, "coachingBuddies.byGuestId", guestId);
        final List<Guest> coaches = new ArrayList<Guest>();
        for (CoachingBuddy sharingBuddy : coachingBuddies) {
            final Guest buddyGuest = guestService.getGuestById(sharingBuddy.buddyId);
            coaches.add(buddyGuest);
        }
        return coaches;
    }

    @Override
    public List<Guest> getCoachees(final long guestId) {
        final List<CoachingBuddy> coacheeBuddies = JPAUtils.find(em, CoachingBuddy.class, "coachingBuddies.byBuddyId", guestId);
        final List<Guest> coachees = new ArrayList<Guest>();
        for (CoachingBuddy sharingBuddy : coacheeBuddies) {
            final Guest buddyGuest = guestService.getGuestById(sharingBuddy.guestId);
            coachees.add(buddyGuest);
        }
        return coachees;
    }

    @Override
    public CoachingBuddy getCoach(final long guestId, final String username) {
        final Guest buddyGuest = guestService.getGuest(username);
        if (buddyGuest==null) return null;
        final CoachingBuddy coachingBuddy = JPAUtils.findUnique(em, CoachingBuddy.class,
                                                              "coachingBuddies.byGuestAndBuddyId",
                                                              guestId, buddyGuest.getId());
        return coachingBuddy;
    }

    @Override
    public CoachingBuddy getCoachee(final long guestId, final String username) {
        final Guest buddyGuest = guestService.getGuest(username);
        if (buddyGuest==null) return null;
        final CoachingBuddy coachingBuddy = JPAUtils.findUnique(em, CoachingBuddy.class,
                                                                "coachingBuddies.byGuestAndBuddyId",
                                                                buddyGuest.getId(), guestId);
        return coachingBuddy;
    }

    @Override
    public CoachingBuddy getCoachee(final long guestId, final long coacheeId) {
        final CoachingBuddy coachingBuddy = JPAUtils.findUnique(em, CoachingBuddy.class,
                                                                "coachingBuddies.byGuestAndBuddyId",
                                                                coacheeId, guestId);
        return coachingBuddy;
    }
}
