package org.fluxtream.core.mvc.models;

import org.fluxtream.core.domain.oauth2.Application;

/**
 * User: candide
 * Date: 16/04/14
 * Time: 10:29
 */
public class ApplicationModel {

    public String name;
    public String description;
    public String sharedSecret;
    public String website;
    public String uid;

    public ApplicationModel() {}

    public ApplicationModel(Application app) {
        this.name = app.name;
        this.description = app.description;
        this.sharedSecret = app.sharedSecret;
        this.website= app.website;
        this.uid = app.uid;
    }

}
