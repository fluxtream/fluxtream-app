package org.fluxtream.core.api;

import org.apache.commons.codec.binary.Base64;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.services.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;

/**
 * User: candide
 * Date: 23/09/14
 * Time: 15:39
 */
@Path("/v1/couch")
@Component("RESTCouchDBController")
@Scope("request")
public class CouchDBController {

    private final String COUCH_DB_USER_TOKEN_ATTRIBUTE_KEY = "couchDB.userToken";
    @Autowired
    GuestService guestService;

    @Autowired
    Configuration env;

    class CouchDBCredentials {
        public String user_login, user_token;
        public int status;
    }

    @PUT
    @Path("/")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response init()
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException, NoSuchAlgorithmException, UnsupportedEncodingException {
        try {
            final StringBuffer userTokenBuffer = new StringBuffer();
            final int status = getFluxtreamCaptureCouchDBUserToken(userTokenBuffer, true);
            final CouchDBCredentials couchDBCredentials = new CouchDBCredentials();
            couchDBCredentials.user_login = getBase64URLSafeUsername();
            couchDBCredentials.user_token = userTokenBuffer.toString();
            couchDBCredentials.status = status;
            return Response.ok().entity(couchDBCredentials).build();
        } catch (Throwable t) {
            final CouchDBCredentials couchDBCredentials = new CouchDBCredentials();
            couchDBCredentials.status = 2;
            return Response.serverError().entity(couchDBCredentials).build();
        }
    }

    private String getBase64URLSafeUsername() {
        return Base64.encodeBase64URLSafeString(AuthHelper.getGuest().username.getBytes());
    }

    @GET
    @Path("/")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response read()
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException, NoSuchAlgorithmException, UnsupportedEncodingException {
        try {
            final StringBuffer userTokenBuffer = new StringBuffer();
            final int status = getFluxtreamCaptureCouchDBUserToken(userTokenBuffer, false);
            final CouchDBCredentials couchDBCredentials = new CouchDBCredentials();
            couchDBCredentials.user_login = getBase64URLSafeUsername();
            couchDBCredentials.user_token = userTokenBuffer.toString();
            couchDBCredentials.status = status;
            return Response.ok().entity(couchDBCredentials).build();
        } catch (Throwable t) {
            final CouchDBCredentials couchDBCredentials = new CouchDBCredentials();
            couchDBCredentials.status = 2;
            return Response.serverError().entity(couchDBCredentials).build();
        }
    }

    @POST
    @Path("/reset")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response reset()
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException, NoSuchAlgorithmException, UnsupportedEncodingException {
        try {
            final StringBuffer userTokenBuffer = new StringBuffer();
            final ApiKey apiKey = getFluxtreamCaptureApiKey(AuthHelper.getGuestId());
            final String couchDBUserToken = guestService.getApiKeyAttribute(apiKey, COUCH_DB_USER_TOKEN_ATTRIBUTE_KEY);
            final CouchDBCredentials couchDBCredentials = new CouchDBCredentials();
            couchDBCredentials.user_login = getBase64URLSafeUsername();
            if (couchDBUserToken==null) {
                couchDBCredentials.status = 1;
                return Response.ok().entity(couchDBCredentials).build();
            } else {
                guestService.removeApiKeyAttribute(apiKey.getId(), COUCH_DB_USER_TOKEN_ATTRIBUTE_KEY);
                couchDBCredentials.status = getFluxtreamCaptureCouchDBUserToken(userTokenBuffer, true);
                return Response.ok().entity(couchDBCredentials).build();
            }
        } catch (Throwable t) {
            final CouchDBCredentials couchDBCredentials = new CouchDBCredentials();
            couchDBCredentials.status = 2;
            return Response.serverError().entity(couchDBCredentials).build();
        }
    }

    final ApiKey getFluxtreamCaptureApiKey(final long guestId) {
        Connector connector = Connector.getConnector("fluxtream_capture");
        final ApiKey apiKey;
        List<ApiKey> apiKeys = guestService.getApiKeys(guestId, connector);
        if (apiKeys != null && !apiKeys.isEmpty()) {
            apiKey = apiKeys.get(0);
        }
        else {
            apiKey = guestService.createApiKey(guestId, connector);
        }
        return apiKey;
    }

    private int getFluxtreamCaptureCouchDBUserToken(final StringBuffer saltBuffer, final boolean createIfNotExists) {
        final ApiKey apiKey = getFluxtreamCaptureApiKey(AuthHelper.getGuestId());
        final String couchDBUserToken = guestService.getApiKeyAttribute(apiKey, COUCH_DB_USER_TOKEN_ATTRIBUTE_KEY);
        if (couchDBUserToken==null && createIfNotExists) {
            byte[] b = new byte[20];
            new Random().nextBytes(b);
            final String userToken = Base64.encodeBase64URLSafeString(b);
            guestService.setApiKeyAttribute(apiKey, COUCH_DB_USER_TOKEN_ATTRIBUTE_KEY, userToken);
            saltBuffer.append(userToken);
            return 0;
        }
        saltBuffer.append(couchDBUserToken);
        return createIfNotExists ? 1 : 0;
    }

}
