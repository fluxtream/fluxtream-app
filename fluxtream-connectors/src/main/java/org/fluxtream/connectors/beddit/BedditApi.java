package org.fluxtream.connectors.beddit;

import org.scribe.builder.api.DefaultApi20;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.extractors.JsonTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Verb;
import org.scribe.utils.OAuthEncoder;

/**
 * Created by candide on 09/02/15.
 */
public class BedditApi extends DefaultApi20 {

    public BedditApi() {}

    public String getAuthorizationUrl(OAuthConfig config) {
        return String.format("https://cloudapi.beddit.com/api/v1/auth/authorize_web?client_id=%s&response_type=code&redirect_uri=%s",
            new Object[]{config.getApiKey(),
                    OAuthEncoder.encode(config.getCallback())
            }
        );
    }

    public String getAccessTokenEndpoint() {
        return "https://cloudapi.beddit.com/api/v1/auth/authorize?grant_type=authorization_code";
    }

    public Verb getAccessTokenVerb() {
        return Verb.POST;
    }

    public AccessTokenExtractor getAccessTokenExtractor() {
        return new JsonTokenExtractor();
    }

}
