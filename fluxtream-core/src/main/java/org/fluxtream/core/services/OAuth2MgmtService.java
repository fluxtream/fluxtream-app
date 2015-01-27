package org.fluxtream.core.services;

import org.fluxtream.core.domain.oauth2.Application;
import org.fluxtream.core.domain.oauth2.AuthorizationCode;
import org.fluxtream.core.domain.oauth2.AuthorizationCodeResponse;
import org.fluxtream.core.domain.oauth2.AuthorizationToken;
import org.fluxtream.core.mvc.models.AuthorizationTokenModel;

import java.util.List;
import java.util.Set;

/**
 * User: candide
 * Date: 10/04/14
 * Time: 11:59
 */
public interface OAuth2MgmtService {

    Application getApplicationForClientId(final String clientId);

    Application getApplicationForToken(AuthorizationToken token);

    AuthorizationCode issueAuthorizationCode(Long id, Set<String> scopes, String state);

    AuthorizationCode getCode(String code);

    AuthorizationCodeResponse getResponse(String code);

    void storeVerification(AuthorizationCodeResponse codeResponse);

    void storeToken(AuthorizationToken token);

    AuthorizationToken issueAuthorizationToken(long guestId, long applicationId);

    AuthorizationToken getTokenFromRefreshToken(String refreshToken);

    AuthorizationToken getTokenFromAccessToken(String accessToken);

    List<AuthorizationTokenModel> getTokens(final long guestId);

    void revokeAccessToken(final long guestId, final String accessToken);

    AuthorizationToken getAuthorizationToken(long guestId, String deviceId, long millis);
}
