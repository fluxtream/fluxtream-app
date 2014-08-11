package org.fluxtream.core.services;

import org.fluxtream.core.domain.*;

import java.util.List;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface CoachingService {

    public static final String BUDDY_TO_ACCESS_HEADER = "X-FLX-BUDDY-TO-ACCESS";

    public void addCoach(long guestId, String username);

    public void removeCoach(long guestId, String username);

    public SharedConnector addSharedConnector(long guestId, String username, String connectorName, String filterJson);

    public void removeSharedConnector(long guestId, String username, String connectorName);

    public boolean isViewingGranted(long guestId, long coacheeId, String connectorName);

    public List<Guest> getCoaches(long guestId);

    public List<Guest> getCoachees(long guestId);

    public CoachingBuddy getCoach(long guestId, String username);

    public CoachingBuddy getCoachee(long guestId, String username);

    public CoachingBuddy getCoachee(long guestId, long coacheeId);

    public <T extends AbstractFacet> List<T> filterFacets(long viewerId, long apiKeyId, List<T> facets);

    SharedConnector getSharedConnector(long apiKeyId, long viewerId);

    List<SharedConnector> getSharedConnectors(ApiKey apiKey);

    void setSharedConnectorFilter(long sharedConnectorId, String filterJson);
}