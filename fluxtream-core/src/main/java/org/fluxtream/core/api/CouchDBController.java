package org.fluxtream.core.api;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.fluxtream.core.utils.Utils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: candide
 * Date: 23/09/14
 * Time: 15:39
 */
@Path("/v1/couch")
@Component("RESTCouchDBController")
@Scope("request")
public class CouchDBController {

    FlxLogger logger = FlxLogger.getLogger(CouchDBController.class);

    private final String COUCH_DB_USER_TOKEN_ATTRIBUTE_KEY = "couchDB.userToken";
    @Autowired
    GuestService guestService;

    @Autowired
    Configuration env;

    static Pattern legalCouchDbNamePattern = Pattern.compile("^[a-z]([a-z0-9,_$()+\\-/])*$");

    public static void main(String[] args) {
        System.out.println(maybeHash("candide4@me.com"));
        System.out.println(maybeHash(" candide4@me.com"));
        System.out.println(maybeHash("candide4@me.com "));
    }

    public static String maybeHash(String username) {
        Matcher matcher = legalCouchDbNamePattern.matcher(username);
        if (!matcher.matches())
            return Utils.hash(username);
        return username;
    }

    class CouchDBCredentials {
        public String user_login, user_token;
        public int status;
        public String statusMessage;
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
            couchDBCredentials.user_login = maybeHash(AuthHelper.getGuest().username);
            couchDBCredentials.user_token = userTokenBuffer.toString();
            couchDBCredentials.status = status;
            couchDBCredentials.statusMessage = "OK";
            createCouchUser(maybeHash(AuthHelper.getGuest().username), couchDBCredentials.user_token);
            createUserDatabases(maybeHash(AuthHelper.getGuest().username));
            // If user already created need different strategy
            return Response.ok().entity(couchDBCredentials).build();
        } catch (UnexpectedHttpResponseCodeException e) {
          final CouchDBCredentials couchDBCredentials = new CouchDBCredentials();
          couchDBCredentials.status = e.getHttpResponseCode();
          couchDBCredentials.statusMessage = e.getHttpResponseMessage();
          return Response.serverError().entity(couchDBCredentials).build();
        }
        catch (Throwable t) {
            final CouchDBCredentials couchDBCredentials = new CouchDBCredentials();
            couchDBCredentials.status = 2;
            return Response.serverError().entity(couchDBCredentials).build();
        }
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
            couchDBCredentials.user_login = maybeHash(AuthHelper.getGuest().username);
            couchDBCredentials.user_token = userTokenBuffer.toString();
            couchDBCredentials.status = status;
            createUserDatabases(maybeHash(AuthHelper.getGuest().username));
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
            couchDBCredentials.user_login = maybeHash(AuthHelper.getGuest().username);
            if (couchDBUserToken==null) {
                couchDBCredentials.status = 1;
                return Response.ok().entity(couchDBCredentials).build();
            } else {
                destroyCouchDatabase(maybeHash(AuthHelper.getGuest().username));
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
            try {
              createUserDatabases(maybeHash(AuthHelper.getGuest().username));
            } catch (UnexpectedHttpResponseCodeException e) {
                return 2;
            }
            return 0;
        }
        saltBuffer.append(couchDBUserToken);
        return createIfNotExists ? 1 : 0;
    }

    private void destroyCouchDatabase(final String dbUsername) throws UnexpectedHttpResponseCodeException {
        HttpClient client = new DefaultHttpClient();
        try {
            final String couchdbHost = env.get("couchdb.host");
            final String couchdbPort = env.get("couchdb.port");
            HttpDelete delete = new HttpDelete(String.format("http://%s:%s/", couchdbHost, couchdbPort) + dbUsername);

            HttpResponse response = client.execute(delete);

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                logger.warn("Error communicating with CouchDB: " + response.getStatusLine().getReasonPhrase());
                throw new UnexpectedHttpResponseCodeException(response.getStatusLine().getStatusCode(),
                        response.getStatusLine().getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    private void createUserDatabases(final String dbUsername) throws UnexpectedHttpResponseCodeException {
      int statusObservations, statusTopics, statusDeleteObservations, statusDeleteTopics;

      // Create Observations database and add rights for the user
      statusObservations = createCouchDB(dbUsername, "self_report_db_observations_");

      // Create Topics database and add rights for the user
      statusTopics = createCouchDB(dbUsername, "self_report_db_topics_");

      // Create database of deleted Observations and add rights for the user
      statusDeleteObservations = createCouchDB(dbUsername, "self_report_db_deleted_observations_");

      // Create database of deleted Topics and add rights for the user
      statusDeleteTopics = createCouchDB(dbUsername, "self_report_db_deleted_topics_");
    }

    private int createCouchUser(final String username, final String password) throws UnexpectedHttpResponseCodeException {
      HttpClient client = new DefaultHttpClient();
      int status = 0;
      try {
        final String couchdbHost = env.get("couchdb.host");
        final String couchdbPort = env.get("couchdb.port");
        final String couchdbAdminLogin = env.get("couchdb.admin_login");
        final String couchdbAdminPasword = env.get("couchdb.admin_password");
        String userPassword = couchdbAdminLogin + ":" + couchdbAdminPasword;
        byte[] encodedCredentials = Base64.encodeBase64(userPassword.getBytes());

        final String request = String.format("http://%s:%s/_users/org.couchdb.user:%s", couchdbHost, couchdbPort, username);
        HttpPut put = new HttpPut(request);
        put.addHeader("Authorization", "Basic " + new String(encodedCredentials));
        put.addHeader("Content-Type", "application/json");
        put.addHeader("Accept", "application/json");

        JSONObject keyArg = new JSONObject();
        keyArg.put("_id", "org.couchdb.user:"+ username);
        keyArg.put("name", username);
        keyArg.put("password", password);
        keyArg.put("roles", new JSONArray());
        keyArg.put("type", "user");
        StringEntity input;
        input = new StringEntity(keyArg.toString());
        put.setEntity(input);

        HttpResponse response = client.execute(put);

        HttpEntity entity = response.getEntity();
        // TODO check if the user already exists
        String responseString = EntityUtils.toString(entity, "UTF-8");

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CONFLICT) {
          status = HttpStatus.SC_CONFLICT;
          return status;
        }

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
          throw new UnexpectedHttpResponseCodeException(response.getStatusLine().getStatusCode(), responseString);
        }

      } catch (ClientProtocolException e) {
        e.printStackTrace();
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      } catch (IOException e) {
        // Error handler should be here
        e.printStackTrace();
      }
      return status;
    }

  private int createCouchDB (final String dbUsername, final String dbName) throws UnexpectedHttpResponseCodeException {
    HttpClient client = new DefaultHttpClient();

    int status = 0;
    try {
      final String couchdbHost = env.get("couchdb.host");
      final String couchdbPort = env.get("couchdb.port");
      final String couchdbAdminLogin = env.get("couchdb.admin_login");
      final String couchdbAdminPasword = env.get("couchdb.admin_password");
      String userPassword = couchdbAdminLogin + ":" + couchdbAdminPasword;
      byte[] encodedCredentials = Base64.encodeBase64(userPassword.getBytes());

      // Create Database
      final String requestObservations = String.format("http://%s:%s/", couchdbHost, couchdbPort) + dbName + dbUsername;
      HttpPut put = new HttpPut(requestObservations);
      put.addHeader("Authorization", "Basic " + new String(encodedCredentials));

      HttpResponse response = client.execute(put);

      HttpEntity entity = response.getEntity();
      String responseString = EntityUtils.toString(entity, "UTF-8");

      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
        throw new UnexpectedHttpResponseCodeException(response.getStatusLine().getStatusCode(), responseString);
      }

      // Add user if Database was created
      final String userRequestObservations = String.format("http://%s:%s/%s/_security", couchdbHost, couchdbPort, dbName + dbUsername);
      put = new HttpPut(userRequestObservations);
      put.addHeader("Authorization", "Basic " + new String(encodedCredentials));
      put.addHeader("Content-Type", "application/json");
      put.addHeader("Accept", "application/json");

      JSONObject keyArgMembers = new JSONObject();
      JSONObject keyArgAdmins = new JSONObject();
      JSONObject keyArg = new JSONObject();
      JSONArray namesArray = new JSONArray();
      namesArray.put(dbUsername);
      keyArgMembers.put("names", namesArray);
      JSONArray rolesArray = new JSONArray();
      rolesArray.put("producer");
      rolesArray.put("consumer");
      keyArgMembers.put("roles", rolesArray);
      keyArg.put("admins", keyArgAdmins);
      keyArg.put("members", keyArgMembers);
      StringEntity input;
      input = new StringEntity(keyArg.toString());
      put.setEntity(input);

      response = client.execute(put);

      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
        throw new UnexpectedHttpResponseCodeException(response.getStatusLine().getStatusCode(), responseString);
      }

    } catch (ClientProtocolException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      return status;
    }
  }

}
