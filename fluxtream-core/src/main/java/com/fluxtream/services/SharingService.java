package com.fluxtream.services;

import java.util.List;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.SharingBuddy;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface SharingService {

    public void addSharingBuddy(long guestId, String username);

    public void removeSharingBuddy(long guestId, String username);

    public void addSharedConnector(long guestId, String username, String connectorName, String filterJson);

    public void removeSharedConnector(long guestId, String username, String connectorName);

    public List<Guest> getBuddies(long guestId);

    public SharingBuddy getSharingBuddy(long guestId, String username);

}