package org.fluxtream.core.services;

import org.fluxtream.core.domain.*;

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

    public boolean isViewingGranted(long guestId, long coacheeId, String connectorName);

    public List<Guest> getTrustingBuddies(long guestId);

    public List<Guest> getTrustedBuddies(long guestId);

    public CoachingBuddy getTrustedBuddy(long guestId, String username);

    public CoachingBuddy getTrustingBuddy(long guestId, String username);

    public CoachingBuddy getTrustingBuddy(long guestId, long coacheeId);

    public <T extends AbstractFacet> List<T> filterFacets(long viewerId, long apiKeyId, List<T> facets);

    SharedConnector getSharedConnector(long apiKeyId, long viewerId);

    List<SharedConnector> getSharedConnectors(long trustedBuddyId, long trustingBuddyId);

    List<SharedConnector> getSharedConnectors(ApiKey apiKey);

    void setSharedConnectorFilter(long sharedConnectorId, String filterJson);
}