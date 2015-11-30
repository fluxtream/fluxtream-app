package org.fluxtream.connectors.misfit;

import org.scribe.builder.api.DefaultApi20;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.extractors.JsonTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Verb;
import org.scribe.utils.OAuthEncoder;

/**
 * Created by candide on 09/02/15.
 */
public class MisfitApi extends DefaultApi20 {

    private static final String AUTHORIZATION_URL = "https://api.misfitwearables.com/auth/dialog/authorize?client_id=%s&response_type=code&redirect_uri=%s";

    @Override
    public String getAccessTokenEndpoint() {
        return "https://api.misfitwearables.com/auth/tokens/exchange?grant_type=authorization_code";
    }

    @Override
    public String getAuthorizationUrl(final OAuthConfig config) {
        return String.format(AUTHORIZATION_URL, config.getApiKey(), OAuthEncoder.encode(config.getCallback()));
    }

    @Override
    public Verb getAccessTokenVerb()
    {
        return Verb.POST;
    }

    @Override
    public AccessTokenExtractor getAccessTokenExtractor()
    {
        return new JsonTokenExtractor();
    }
}
