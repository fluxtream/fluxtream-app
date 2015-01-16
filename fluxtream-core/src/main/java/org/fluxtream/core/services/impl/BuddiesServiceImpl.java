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
import javax.persistence.Query;
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
            TrustedBuddy buddy = new TrustedBuddy();
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
        final TrustedBuddy trustedBuddy = JPAUtils.findUnique(em, TrustedBuddy.class,
                                                              "trustedBuddies.byGuestAndBuddyId",
                                                              guestId, buddyGuest.getId());
        if (trustedBuddy ==null) return;
        Query nativeQuery = em.createNativeQuery(String.format("DELETE sc from SharedChannels sc JOIN TrustedBuddies tb on sc.buddy_id=tb.id WHERE tb.guestId=%s", guestId));
        nativeQuery.executeUpdate();
        nativeQuery = em.createNativeQuery(String.format("DELETE sc from SharedConnectors sc JOIN TrustedBuddies tb on sc.buddy_id=tb.id WHERE tb.guestId=%s", guestId));
        nativeQuery.executeUpdate();
        Guest buddy = guestService.getGuest(username);
        nativeQuery = em.createNativeQuery(String.format("DELETE tb FROM TrustedBuddies tb WHERE tb.guestId=%s AND tb.buddyId=%s", guestId, buddy.getId()));
        nativeQuery.executeUpdate();
    }

    @Override
    @Transactional(readOnly=false)
    public SharedConnector addSharedConnector(final long guestId, final String username, final String connectorName, final String filterJson) {
        final Guest buddyGuest = guestService.getGuest(username);
        if (buddyGuest==null) throw new RuntimeException("No such guest: " + username);
        final TrustedBuddy trustedBuddy = JPAUtils.findUnique(em, TrustedBuddy.class,
                                                              "trustedBuddies.byGuestAndBuddyId",
                                                              guestId, buddyGuest.getId());
        if (trustedBuddy ==null) throw new RuntimeException("Guest doesn't have a coaching buddy for this connector");
        for(SharedConnector sharedConnector : trustedBuddy.sharedConnectors) {
            if (sharedConnector.connectorName.equals(connectorName))
                return null;
        }
        SharedConnector sharedConnector = new SharedConnector();
        sharedConnector.connectorName = connectorName;
        sharedConnector.filterJson = filterJson;
        trustedBuddy.sharedConnectors.add(sharedConnector);
        sharedConnector.buddy = trustedBuddy;
        em.persist(sharedConnector);
        em.merge(trustedBuddy);
        return sharedConnector;
    }

    @Override
    @Transactional(readOnly=false)
    public void removeSharedConnector(final long guestId, final String username, final String connectorName) {
        final Guest buddyGuest = guestService.getGuest(username);
        if (buddyGuest==null) return;
        final TrustedBuddy trustedBuddy = JPAUtils.findUnique(em, TrustedBuddy.class,
                                                              "trustedBuddies.byGuestAndBuddyId",
                                                              guestId, buddyGuest.getId());
        if (trustedBuddy ==null) return;
        SharedConnector toRemove = null;
        for(SharedConnector sharedConnector : trustedBuddy.sharedConnectors) {
            if (sharedConnector.connectorName.equals(connectorName)) {
                toRemove = sharedConnector;
                break;
            }
        }
        if (toRemove!=null) {
            trustedBuddy.sharedConnectors.remove(toRemove);
            toRemove.buddy = null;
            em.remove(toRemove);
            em.merge(trustedBuddy);
        }
    }

    @Override
    @Transactional(readOnly=false)
    public void removeSharedConnectors(long apiKeyId) {
        ApiKey apiKey = guestService.getApiKey(apiKeyId);
        String connectorName = apiKey.getConnector().getName();
        String queryString = String.format("DELETE sc from SharedConnectors sc JOIN TrustedBuddies tb " +
                "ON sc.buddy_id=tb.id WHERE sc.connectorName=\"%s\" AND tb.guestId=%s",
                connectorName, apiKey.getGuestId());
        Query nativeQuery = em.createNativeQuery(queryString);
        nativeQuery.executeUpdate();
    }

    @Override
    public boolean isViewingGranted(final long guestId, final long trustingBuddyId, final String connectorName) {
        final TrustedBuddy trustedBuddy = JPAUtils.findUnique(em, TrustedBuddy.class, "trustedBuddies.byGuestAndBuddyId", trustingBuddyId, guestId);
        boolean granted = trustedBuddy.hasAccessToConnector(connectorName);
        return granted;
    }

    @Override
    public List<Guest> getTrustedBuddies(final long guestId) {
        final List<TrustedBuddy> coachingBuddies = JPAUtils.find(em, TrustedBuddy.class, "trustedBuddies.byGuestId", guestId);
        final List<Guest> coaches = new ArrayList<Guest>();
        for (TrustedBuddy sharingBuddy : coachingBuddies) {
            final Guest buddyGuest = guestService.getGuestById(sharingBuddy.buddyId);
            if (buddyGuest!=null)
                coaches.add(buddyGuest);
        }
        return coaches;
    }

    @Override
    public List<Guest> getTrustingBuddies(final long guestId) {
        final List<TrustedBuddy> trustedBuddies = JPAUtils.find(em, TrustedBuddy.class, "trustedBuddies.byBuddyId", guestId);
        final List<Guest> trustingBuddies = new ArrayList<Guest>();
        for (TrustedBuddy sharingBuddy : trustedBuddies) {
            final Guest buddyGuest = guestService.getGuestById(sharingBuddy.guestId);
            trustingBuddies.add(buddyGuest);
        }
        return trustingBuddies;
    }

    @Override
    public TrustedBuddy getTrustedBuddy(final long guestId, final String username) {
        final Guest buddyGuest = guestService.getGuest(username);
        if (buddyGuest==null) return null;
        final TrustedBuddy trustedBuddy = JPAUtils.findUnique(em, TrustedBuddy.class,
                                                              "trustedBuddies.byGuestAndBuddyId",
                                                              guestId, buddyGuest.getId());
        return trustedBuddy;
    }

    @Override
    public TrustedBuddy getTrustedBuddy(final long guestId, final long trustingBuddyId) {
        final TrustedBuddy trustedBuddy = JPAUtils.findUnique(em, TrustedBuddy.class, "trustedBuddies.byGuestAndBuddyId", trustingBuddyId, guestId);
        return trustedBuddy;
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

    @Override
    public List<SharedChannel> getSharedChannels(long trustedBuddyId, long trustingBuddyId) {
        List<SharedChannel> sharedChannels = JPAUtils.find(em, SharedChannel.class, "sharedChannel.byTrustedBuddyId", trustingBuddyId, trustedBuddyId);
        return sharedChannels;
    }

    @Override
    public List<SharedChannel> getSharedChannels(long trustedBuddyId, long trustingBuddyId, long apiKeyId) {
        List<SharedChannel> sharedChannels = JPAUtils.find(em, SharedChannel.class, "sharedChannel.byApiKeyId", trustingBuddyId, trustedBuddyId, apiKeyId);
        return sharedChannels;
    }

    @Override
    @Transactional(readOnly=false)
    public SharedChannel addSharedChannel(long trustedBuddyId, long trustingBuddyId, long channelMappingId) {
        ChannelMapping channelMapping = em.find(ChannelMapping.class, channelMappingId);
        final TrustedBuddy trustedBuddy = JPAUtils.findUnique(em, TrustedBuddy.class, "trustedBuddies.byGuestAndBuddyId", trustingBuddyId, trustedBuddyId);
        List<SharedChannel> alreadyShared = JPAUtils.find(em, SharedChannel.class, "sharedChannel.byBuddyAndChannelMapping", trustingBuddyId, trustedBuddyId, channelMappingId);
        if (alreadyShared==null||alreadyShared.size()>0)
            return null;
        SharedChannel sharedChannel = new SharedChannel(trustedBuddy, channelMapping);
        em.persist(sharedChannel);
        return sharedChannel;
    }

    @Override
    @Transactional(readOnly=false)
    public void removeSharedChannel(long trustedBuddyId, long trustingBuddyId, long channelMappingId) {
        SharedChannel sharedChannel = JPAUtils.findUnique(em, SharedChannel.class, "sharedChannel.byBuddyAndChannelMapping", trustingBuddyId, trustedBuddyId, channelMappingId);
        em.remove(sharedChannel);
    }

    @Override
    @Transactional(readOnly=false)
    public void removeSharedChannels(long apiKeyId) {
        Query nativeQuery = em.createNativeQuery("delete sc from SharedChannels sc JOIN ChannelMapping  cm on sc.channelMapping_id=cm.id WHERE cm.apiKeyId=" + apiKeyId);
        nativeQuery.executeUpdate();
    }

    @Override
    @Transactional(readOnly=false)
    public void removeAllSharedChannels(long guestId) {
        Query nativeQuery = em.createNativeQuery(String.format("DELETE sc from SharedChannels sc JOIN TrustedBuddies tb on sc.buddy_id=tb.id WHERE tb.buddyId=%s OR tb.guestId=%s", guestId, guestId));
        nativeQuery.executeUpdate();
    }

    @Override
    @Transactional(readOnly=false)
    public void removeAllSharedConnectors(long guestId) {
        Query nativeQuery = em.createNativeQuery(String.format("DELETE sc from SharedConnectors sc JOIN TrustedBuddies tb on sc.buddy_id=tb.id WHERE tb.buddyId=%s OR tb.guestId=%s", guestId, guestId));
        nativeQuery.executeUpdate();
    }
}
