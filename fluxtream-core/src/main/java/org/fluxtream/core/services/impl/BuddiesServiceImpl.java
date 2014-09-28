package org.fluxtream.core.services.impl;

import org.apache.log4j.Logger;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.SharedConnectorFilter;
import org.fluxtream.core.domain.*;
import org.fluxtream.core.services.BuddiesService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.utils.JPAUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Service
@Transactional(readOnly=true)
public class BuddiesServiceImpl implements BuddiesService {

    Logger logger = Logger.getLogger(BuddiesServiceImpl.class);

    @Autowired
    GuestService guestService;

    @PersistenceContext
    EntityManager em;

    @Autowired
    BeanFactory beanFactory;

    @Override
    @Transactional(readOnly=false)
    public void addTrustedBuddy(final long guestId, final String username) {
        final Guest buddyGuest = guestService.getGuest(username);
        if (getTrustedBuddy(guestId, username)==null) {
            CoachingBuddy buddy = new CoachingBuddy();
            buddy.guestId = guestId;
            buddy.buddyId = buddyGuest.getId();
            em.persist(buddy);
        } else {
            logger.warn("attempt to add a coach that's already in the guest's coaching buddies list");
        }
    }

    @Override
    @Transactional(readOnly=false)
    public void removeTrustedBuddy(final long guestId, final String username) {
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
    public SharedConnector addSharedConnector(final long guestId, final String username, final String connectorName, final String filterJson) {
        final Guest buddyGuest = guestService.getGuest(username);
        if (buddyGuest==null) throw new RuntimeException("No such guest: " + username);
        final CoachingBuddy coachingBuddy = JPAUtils.findUnique(em, CoachingBuddy.class,
                                                              "coachingBuddies.byGuestAndBuddyId",
                                                              guestId, buddyGuest.getId());
        if (coachingBuddy==null) throw new RuntimeException("Guest doesn't have a coaching buddy for this connector");
        for(SharedConnector sharedConnector : coachingBuddy.sharedConnectors) {
            if (sharedConnector.connectorName.equals(connectorName))
                return null;
        }
        SharedConnector sharedConnector = new SharedConnector();
        sharedConnector.connectorName = connectorName;
        sharedConnector.filterJson = filterJson;
        coachingBuddy.sharedConnectors.add(sharedConnector);
        sharedConnector.buddy = coachingBuddy;
        em.persist(sharedConnector);
        em.merge(coachingBuddy);
        return sharedConnector;
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
    public List<Guest> getTrustedBuddies(final long guestId) {
        final List<CoachingBuddy> coachingBuddies = JPAUtils.find(em, CoachingBuddy.class, "coachingBuddies.byGuestId", guestId);
        final List<Guest> coaches = new ArrayList<Guest>();
        for (CoachingBuddy sharingBuddy : coachingBuddies) {
            final Guest buddyGuest = guestService.getGuestById(sharingBuddy.buddyId);
            if (buddyGuest!=null)
                coaches.add(buddyGuest);
        }
        return coaches;
    }

    @Override
    public List<Guest> getTrustingBuddies(final long guestId) {
        final List<CoachingBuddy> coacheeBuddies = JPAUtils.find(em, CoachingBuddy.class, "coachingBuddies.byBuddyId", guestId);
        final List<Guest> coachees = new ArrayList<Guest>();
        for (CoachingBuddy sharingBuddy : coacheeBuddies) {
            final Guest buddyGuest = guestService.getGuestById(sharingBuddy.guestId);
            coachees.add(buddyGuest);
        }
        return coachees;
    }

    @Override
    public CoachingBuddy getTrustedBuddy(final long guestId, final String username) {
        final Guest buddyGuest = guestService.getGuest(username);
        if (buddyGuest==null) return null;
        final CoachingBuddy coachingBuddy = JPAUtils.findUnique(em, CoachingBuddy.class,
                                                              "coachingBuddies.byGuestAndBuddyId",
                                                              guestId, buddyGuest.getId());
        return coachingBuddy;
    }

    @Override
    public CoachingBuddy getTrustingBuddy(final long guestId, final String username) {
        final Guest buddyGuest = guestService.getGuest(username);
        if (buddyGuest==null) return null;
        final CoachingBuddy coachingBuddy = JPAUtils.findUnique(em, CoachingBuddy.class,
                                                                "coachingBuddies.byGuestAndBuddyId",
                                                                buddyGuest.getId(), guestId);
        return coachingBuddy;
    }

    @Override
    public CoachingBuddy getTrustingBuddy(final long guestId, final long coacheeId) {
        final CoachingBuddy coachingBuddy = JPAUtils.findUnique(em, CoachingBuddy.class, "coachingBuddies.byGuestAndBuddyId", coacheeId, guestId);
        return coachingBuddy;
    }

    @Override
    public <T extends AbstractFacet> List<T> filterFacets(final long viewerId, final long apiKeyId, final List<T> facets) {
        final ApiKey apiKey = guestService.getApiKey(apiKeyId);
        final Connector connector = apiKey.getConnector();
        final boolean ownFacets = viewerId == apiKey.getGuestId();
        final boolean supportsFiltering = connector.supportsFiltering();
        if (ownFacets ||!supportsFiltering)
            return facets;
        else {
            // retrieve SharedConnector instance;
            SharedConnector sharedConnector = getSharedConnector(apiKeyId, viewerId);
            if (sharedConnector!=null) {
                final SharedConnectorFilter sharedConnectorFilter;
                try {
                    sharedConnectorFilter = beanFactory.getBean(connector.sharedConnectorFilterClass());
                    return sharedConnectorFilter.filterFacets(sharedConnector, facets);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return facets;
    }

    @Override
    public SharedConnector getSharedConnector(final long apiKeyId, final long viewerId) {
        ApiKey apiKey = guestService.getApiKey(apiKeyId);
        final SharedConnector sconn = JPAUtils.findUnique(em, SharedConnector.class,
                                                            "sharedConnector.byConnectorNameAndViewerId",
                                                                apiKey.getConnector().getName(), viewerId);
        return sconn;
    }

    @Override
    public List<SharedConnector> getSharedConnectors(long trustedBuddyId, long trustingBuddyId) {
        final List<SharedConnector> conns = JPAUtils.find(em, SharedConnector.class, "sharedConnector.byTrustedBuddyId", trustingBuddyId, trustedBuddyId);
        return conns;
    }

    @Override
    public List<SharedConnector> getSharedConnectors(final ApiKey apiKey) {
        final List<SharedConnector> conns = JPAUtils.find(em, SharedConnector.class, "sharedConnector.byConnectorNameAndVieweeId", apiKey.getConnector().getName(), apiKey.getGuestId());
        return conns;
    }

    @Override
    @Transactional(readOnly=false)
    public void setSharedConnectorFilter(final long sharedConnectorId, final String filterJson) {
        final SharedConnector sharedConnector = em.find(SharedConnector.class, sharedConnectorId);
        sharedConnector.filterJson = filterJson;
        em.persist(sharedConnector);
    }
}
