package org.fluxtream.core.services.impl;

import org.fluxtream.core.domain.oauth2.Application;
import org.fluxtream.core.domain.oauth2.AuthorizationCode;
import org.fluxtream.core.domain.oauth2.AuthorizationCodeResponse;
import org.fluxtream.core.domain.oauth2.AuthorizationToken;
import org.fluxtream.core.mvc.models.AuthorizationTokenModel;
import org.fluxtream.core.services.OAuth2MgmtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: candide
 * Date: 10/04/14
 * Time: 14:04
 */
@Service
@Transactional(readOnly=true)
public class OAuth2MgmtServiceImpl implements OAuth2MgmtService {

    @PersistenceContext
    EntityManager em;

    @Override
    public Application getApplicationForClientId(final String clientId) {
        final TypedQuery<Application> query = em.createQuery(
                "SELECT application FROM Application application " +
                "WHERE application.uid=?", Application.class);
        query.setParameter(1, clientId);
        final List<Application> resultList = query.getResultList();
        if (resultList.size()>0)
            return resultList.get(0);
        return null;
    }

    @Override
    @Transactional(readOnly=false)
    public AuthorizationCode issueAuthorizationCode(final Long id, final Set<String> scopes, final String state) {
        AuthorizationCode code = new AuthorizationCode(id, scopes, state);
        em.persist(code);
        return code;
    }

    @Override
    public AuthorizationCode getCode(final String code) {
        final TypedQuery<AuthorizationCode> query = em.createQuery(
                "SELECT authorizationCode FROM AuthorizationCode authorizationCode " +
                "WHERE authorizationCode.code=?", AuthorizationCode.class);
        query.setParameter(1, code);
        final List<AuthorizationCode> resultList = query.getResultList();
        if (resultList.size()>0)
            return resultList.get(0);
        return null;
    }

    @Override
    public AuthorizationCodeResponse getResponse(final String code) {
        AuthorizationCode authCode = getCode(code);
        if (authCode==null)
            return null;
        final TypedQuery<AuthorizationCodeResponse> query = em.createQuery(
                "SELECT authorizationCodeResponse FROM AuthorizationCodeResponse authorizationCodeResponse " +
                "WHERE authorizationCodeResponse.authorizationCodeId=?", AuthorizationCodeResponse.class);
        query.setParameter(1, authCode.getId());
        final List<AuthorizationCodeResponse> resultList = query.getResultList();
        if (resultList.size()>0)
            return resultList.get(0);
        return null;
    }

    @Override
    @Transactional(readOnly=false)
    public void storeVerification(final AuthorizationCodeResponse codeResponse) {
        em.persist(codeResponse);
    }

    @Override
    @Transactional(readOnly=false)
    public void storeToken(final AuthorizationToken token) {
        // discard old tokens with the same authorization code
        final Query query = em.createQuery("DELETE FROM AuthorizationToken token WHERE token.authorizationCodeId=?");
        query.setParameter(1, token.authorizationCodeId);
        query.executeUpdate();
        em.persist(token);
    }

    @Override
    @Transactional(readOnly=false)
    public AuthorizationToken issueAuthorizationToken(long guestId, long applicationId)
    {
        AuthorizationCode code = new AuthorizationCode(guestId, null, null);
        code.applicationId = applicationId;
        em.persist(code);
        AuthorizationToken token = new AuthorizationToken(guestId);
        token.authorizationCodeId = code.getId();
        em.persist(token);
        return token;
    }

    @Override
    public AuthorizationToken getTokenFromRefreshToken(final String refreshToken) {
        final TypedQuery<AuthorizationToken> query = em.createQuery(
                "SELECT authorizationToken FROM AuthorizationToken authorizationToken " +
                "WHERE authorizationToken.refreshToken=?", AuthorizationToken.class);
        query.setParameter(1, refreshToken);
        final List<AuthorizationToken> resultList = query.getResultList();
        if (resultList.size()>0)
            return resultList.get(0);
        return null;
    }

    @Override
    public AuthorizationToken getTokenFromAccessToken(String accessToken) {
        final TypedQuery<AuthorizationToken> query = em.createQuery(
                "SELECT authorizationToken FROM AuthorizationToken authorizationToken " +
                        "WHERE authorizationToken.accessToken=?", AuthorizationToken.class);
        query.setParameter(1, accessToken);
        final List<AuthorizationToken> resultList = query.getResultList();
        if (resultList.size()>0) {
            final AuthorizationToken authorizationToken = resultList.get(0);
            return authorizationToken;
        }
        return null;
    }

    @Override
    public List<AuthorizationTokenModel> getTokens(long guestId) {
        final TypedQuery<AuthorizationToken> query = em.createQuery(
                "SELECT authorizationToken FROM AuthorizationToken authorizationToken " +
                        "WHERE authorizationToken.guestId=?", AuthorizationToken.class);
        query.setParameter(1, guestId);
        final List<AuthorizationToken> resultList = query.getResultList();
        final List<AuthorizationTokenModel> tokenModels = new ArrayList<AuthorizationTokenModel>();
        for (AuthorizationToken authorizationToken : resultList) {
            AuthorizationCode authCode = em.find(AuthorizationCode.class, authorizationToken.authorizationCodeId);
            if (authCode==null) continue;
            Application application = em.find(Application.class, authCode.applicationId);
            AuthorizationTokenModel tokenModel = new AuthorizationTokenModel(authorizationToken.accessToken,
                    application.name, application.organization, application.website, authCode.creationTime);
            tokenModels.add(tokenModel);
        }
        return tokenModels;
    }

    @Override
    @Transactional(readOnly=false)
    public void revokeAccessToken(final long guestId, final String accessToken) {
        final AuthorizationToken authorizationToken = getTokenFromAccessToken(accessToken);
        if (authorizationToken.guestId!=guestId)
            throw new RuntimeException("Attempt to revoke an authorizationToken by another user");

        // erase the token's associated authorization code response, if there is one
        Query query = em.createQuery("SELECT response FROM AuthorizationCodeResponse response WHERE authorizationCodeId=?");
        query.setParameter(1, authorizationToken.authorizationCodeId);
        List resultList = query.getResultList();
        if (resultList.size()>0)
            em.remove(resultList.get(0));

        // erase the token's associated authorization code, if there is one
        query = em.createQuery("SELECT code FROM AuthorizationCode code WHERE id=?");
        query.setParameter(1, authorizationToken.authorizationCodeId);
        resultList = query.getResultList();
        if (resultList.size()>0)
            em.remove(resultList.get(0));

        query = em.createNativeQuery("DELETE FROM AuthorizationToken WHERE accessToken=?");
        query.setParameter(1, accessToken);
        query.executeUpdate();
    }

    public AuthorizationToken getExistingDeviceToken(final String refreshToken, final long guestId) {
        final TypedQuery<AuthorizationToken> query = em.createQuery(
                "SELECT authorizationToken FROM AuthorizationToken authorizationToken " +
                        "WHERE authorizationToken.refreshToken=? AND authorizationToken.guestId=?", AuthorizationToken.class);
        query.setParameter(1, refreshToken);
        query.setParameter(2, guestId);
        final List<AuthorizationToken> resultList = query.getResultList();
        if (resultList.size()>0)
            return resultList.get(0);
        return null;
    }

    @Override
    @Transactional(readOnly=false)
    public AuthorizationToken getAuthorizationToken(final long guestId, final String deviceId, final long expirationTime) {
        if (deviceId==null)
            throw new RuntimeException("null deviceId when getting an authorizationToken");
        final AuthorizationToken existing = getExistingDeviceToken(deviceId, guestId);
        if (existing!=null && existing.guestId==guestId) {
            existing.expirationTime = expirationTime;
            return existing;
        }
        AuthorizationToken authorizationToken = new AuthorizationToken(guestId, deviceId, expirationTime);
        em.persist(authorizationToken);
        return authorizationToken;
    }

    @Override
    public Application getApplicationForToken(final AuthorizationToken token) {
        final AuthorizationCode authorizationCode = em.find(AuthorizationCode.class, token.authorizationCodeId);
        if (authorizationCode!=null) {
            Application application = em.find(Application.class, authorizationCode.applicationId);
            return application;
        }
        return null;
    }
}
