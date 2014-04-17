package org.fluxtream.core.services;

import org.fluxtream.core.domain.oauth2.Application;

import java.util.List;

/**
 * User: candide
 * Date: 16/04/14
 * Time: 10:02
 */
public interface PartnerAppsService {

    void createApplication(final long guestId, String name, String description);

    void deleteApplication(final long guestId, final String uid);

    List<Application> getApplications(final long guestId);

    Application getApplication(final long guestId, final String uid);

    void updateApplication(long guestId, String uid, String name, String description);
}
