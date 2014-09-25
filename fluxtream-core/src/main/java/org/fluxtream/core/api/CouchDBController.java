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

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
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

    @POST
    @Path("/init")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response init(@FormParam("userLogin") String userLogin)
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException, NoSuchAlgorithmException, UnsupportedEncodingException {
        try {
            final StringBuffer userTokenBuffer = new StringBuffer();
            final int status = getFluxtreamCaptureCouchDBUserToken(userTokenBuffer, true);
            final CouchDBCredentials couchDBCredentials = new CouchDBCredentials();
            couchDBCredentials.user_login = userLogin;
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
    @Path("/read")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response read(@FormParam("userLogin") String userLogin)
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException, NoSuchAlgorithmException, UnsupportedEncodingException {
        try {
            final StringBuffer userTokenBuffer = new StringBuffer();
            final int status = getFluxtreamCaptureCouchDBUserToken(userTokenBuffer, false);
            final CouchDBCredentials couchDBCredentials = new CouchDBCredentials();
            couchDBCredentials.user_login = userLogin;
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
    public Response reset(@FormParam("userLogin") String userLogin)
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException, NoSuchAlgorithmException, UnsupportedEncodingException {
        try {
            final StringBuffer userTokenBuffer = new StringBuffer();
            final ApiKey apiKey = guestService.getApiKey(AuthHelper.getGuestId(), Connector.getConnector("fluxtream_capture"));
            final String couchDBUserToken = guestService.getApiKeyAttribute(apiKey, COUCH_DB_USER_TOKEN_ATTRIBUTE_KEY);
            final CouchDBCredentials couchDBCredentials = new CouchDBCredentials();
            couchDBCredentials.user_login = userLogin;
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

    private int getFluxtreamCaptureCouchDBUserToken(final StringBuffer saltBuffer, final boolean createIfNotExists) {
        final ApiKey apiKey = guestService.getApiKey(AuthHelper.getGuestId(), Connector.getConnector("fluxtream_capture"));
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
