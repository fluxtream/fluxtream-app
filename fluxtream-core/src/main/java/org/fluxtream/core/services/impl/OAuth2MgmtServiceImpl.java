package org.fluxtream.core.services.impl;

import org.fluxtream.core.domain.oauth2.Application;
import org.fluxtream.core.domain.oauth2.AuthorizationCode;
import org.fluxtream.core.domain.oauth2.AuthorizationCodeResponse;
import org.fluxtream.core.domain.oauth2.AuthorizationToken;
import org.fluxtream.core.services.OAuth2MgmtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
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
        em.persist(token);
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
    public Long getApplicationIdForToken(final AuthorizationToken token) {
        final AuthorizationCode authorizationCode = em.find(AuthorizationCode.class, token.authorizationCodeId);
        if (authorizationCode!=null)
            return authorizationCode.applicationId;
        return null;
    }
}
