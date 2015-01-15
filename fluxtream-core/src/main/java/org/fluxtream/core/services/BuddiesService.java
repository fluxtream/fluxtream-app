package org.fluxtream.core.services;

import org.fluxtream.core.domain.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface BuddiesService {

    public static final String BUDDY_TO_ACCESS_PARAM = "buddyToAccess";

    public void addTrustedBuddy(long guestId, String username);

    public void removeTrustedBuddy(long guestId, String username);

    public SharedConnector addSharedConnector(long guestId, String username, String connectorName, String filterJson);

    public void removeSharedConnector(long guestId, String username, String connectorName);

    public void removeSharedConnectors(long apiKeyId);

    public boolean isViewingGranted(long guestId, long trustingBuddyId, String connectorName);

    public List<Guest> getTrustingBuddies(long guestId);

    public List<Guest> getTrustedBuddies(long guestId);

    public TrustedBuddy getTrustedBuddy(long guestId, String username);

    public TrustedBuddy getTrustedBuddy(long guestId, long trustingBuddyId);

    public <T extends AbstractFacet> List<T> filterFacets(long viewerId, long apiKeyId, List<T> facets);

    SharedConnector getSharedConnector(long apiKeyId, long viewerId);

    List<SharedConnector> getSharedConnectors(long trustedBuddyId, long trustingBuddyId);

    List<SharedConnector> getSharedConnectors(ApiKey apiKey);

    void setSharedConnectorFilter(long sharedConnectorId, String filterJson);

    List<SharedChannel> getSharedChannels(long trustedBuddyId, long trustingBuddyId);

    List<SharedChannel> getSharedChannels(long trustedBuddyId, long trustingBuddyId, long apiKeyId);

    public SharedChannel addSharedChannel(long trustedBuddyId, long trustingBuddyId, long channelMappingId);

    void removeSharedChannel(long trustedBuddyId, long trustingBuddyId, long channelMappingId);

    public void removeSharedChannels(long apiKeyId);

    @Transactional(readOnly=false)
    void removeAllSharedChannels(long guestId);

    @Transactional(readOnly=false)
    void removeAllSharedConnectors(long guestId);
}