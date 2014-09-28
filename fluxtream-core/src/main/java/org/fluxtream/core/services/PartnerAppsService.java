package org.fluxtream.core.services;

import org.fluxtream.core.domain.oauth2.Application;

import java.util.List;

/**
 * User: candide
 * Date: 16/04/14
 * Time: 10:02
 */
public interface PartnerAppsService {

    void createApplication(final long guestId, final String organization, String name, String description, String website);

    void deleteApplication(final long guestId, final String uid);

    List<Application> getApplications(final long guestId);

    Application getApplication(final long guestId, final String uid);

    Application getApplication(final String appSecret);

    void updateApplication(final long guestId, final String uid, final String organization, final String name, final String description, final String website);
}
