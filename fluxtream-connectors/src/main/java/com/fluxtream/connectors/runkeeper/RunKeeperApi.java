package com.fluxtream.connectors.runkeeper;

import org.scribe.builder.api.DefaultApi20;
import org.scribe.model.OAuthConfig;
import org.scribe.utils.OAuthEncoder;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class RunKeeperApi extends DefaultApi20 {

    private static final String AUTHORIZATION_URL = "https://runkeeper.com/apps/authorize?client_id=%s&response_type=code&redirect_uri=%s";

    @Override
    public String getAccessTokenEndpoint() {
        return "https://runkeeper.com/apps/token?grant_type=authorization_code";
    }

    @Override
    public String getAuthorizationUrl(final OAuthConfig config) {
        return String.format(AUTHORIZATION_URL, config.getApiKey(), OAuthEncoder.encode(config.getCallback()));
    }

    //@Override
    //public AccessTokenExtractor getAccessTokenExtractor()
    //{
    //    return new JsonTokenExtractor();
    //}
}
