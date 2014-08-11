package org.fluxtream.core.mvc.models;

/**
 * User: candide
 * Date: 11/06/14
 * Time: 12:14
 */
public class AuthorizationTokenModel {

    public String accessToken, name, organization, website;
    public long creationTime;

    public AuthorizationTokenModel(final String accessToken, final String name, final String organization,
                                   final String website, final long creationTime) {
        this.accessToken = accessToken;
        this.name = name;
        this.organization = organization;
        this.website = website;
        this.creationTime = creationTime;
    }

}
