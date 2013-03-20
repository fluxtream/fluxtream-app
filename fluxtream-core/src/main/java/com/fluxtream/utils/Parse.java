package com.fluxtream.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.StringTokenizer;
import com.fluxtream.Configuration;
import com.fluxtream.utils.gson.DateDeserializer;
import com.fluxtream.utils.gson.DateSerializer;
import com.fluxtream.utils.parse.AbstractConstraint;
import com.fluxtream.utils.parse.NewUser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.sf.json.JSONObject;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Candide Kemmler (candide@fluxtream.com)
 */

@Component
public class Parse {

    @Autowired
    Configuration config;

    Gson gson;
    Gson createGson;

    public Parse(){
        gson = getGson();
        createGson = getCreateGson();
    }

    public boolean isParseConfigurationPresent() {
        return config.get("parse.applicationID")!=null;
    }

    public static Gson getCreateGson() {
        GsonBuilder gsonBuilder = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateSerializer())
                .registerTypeAdapter(Date.class, new DateDeserializer());
        return gsonBuilder.create();
    }

    public static Gson getGson() {
        GsonBuilder gsonBuilder = new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateSerializer())
                .registerTypeAdapter(Date.class, new DateDeserializer())
                .serializeNulls();
        return gsonBuilder.create();
    }

    public String fetch(String url) throws RestCallException {
        return executeQueryWithParams(url);
    }

    public String executeQuery(String url, String JSONWhereClause) throws RestCallException {
        HttpClient client = getHttpClient();
        String response = null;
        if (JSONWhereClause!=null) {
            String query = null;
            try {
                query = "?" + URLEncoder.encode("where=" + JSONWhereClause, "UTF-8");
                url += query;
            } catch (UnsupportedEncodingException e)
            { System.out.println(ExceptionUtils.getStackTrace(e)); }
        }
        try {
            HttpGet get = new HttpGet(url);
            final String applicationID = config.get("parse.applicationID");
            get.addHeader("X-Parse-Application-Id", applicationID);
            final String restAPIKey = config.get("parse.RestAPIKey");
            get.addHeader("X-Parse-REST-API-Key", restAPIKey);

            HttpResponse httpResponse = client.execute(get);
            final int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                response = responseHandler.handleResponse(httpResponse);
            }
            else
                throw new RestCallException(url, "Unexpected status code: " + statusCode);
        } catch (UnsupportedEncodingException e) {
            throw new RestCallException(e, url);
        } catch (ClientProtocolException e) {
            throw new RestCallException(e, url);
        } catch (IOException e) {
            throw new RestCallException(e, url);
        } finally {
            client.getConnectionManager().shutdown();
        }
        return response;
    }

    /**
     *
     * @param url parse.com REST endpoint
     * @param constraints a list of AbstractConstraint objects
     * @return
     * @throws RestCallException
     */
    public String executeQueryWithParams(String url, AbstractConstraint... constraints)
            throws RestCallException {
        if (constraints!=null&&constraints.length>0) {
            return executeQuery(url, toJSONMap(constraints));
        } else
            return executeQuery(url, null);
    }

    private String toJSONMap(AbstractConstraint... constraints) {
        JsonObject whereClause = new JsonObject();
        for (AbstractConstraint constraint : constraints) {
            constraint.addToWhereClause(whereClause);
        }
        return gson.toJson(whereClause);
    }

    private HttpClient getHttpClient() {
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        return httpClient;
    }

    private String update(final String url, String objectJson)
            throws RestCallException {
        HttpClient client = getHttpClient();
        String response = null;
        try {
            HttpPut put = new HttpPut(url);
            put.addHeader("X-Parse-Application-Id", config.get("parse.applicationID"));
            put.addHeader("X-Parse-REST-API-Key", config.get("parse.RestAPIKey"));
            put.addHeader("Content-Type", "application/json");

            put.setEntity(new StringEntity(objectJson));

            HttpResponse httpResponse = client.execute(put);

            final int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                response = responseHandler.handleResponse(httpResponse);
            }
            else
                throw new RestCallException(url, "Unexpected status code: " + statusCode);
        } catch (UnsupportedEncodingException e) {
            throw new RestCallException(e, url);
        } catch (ClientProtocolException e) {
            throw new RestCallException(e, url);
        } catch (IOException e) {
            throw new RestCallException(e, url);
        } finally {
            client.getConnectionManager().shutdown();
        }
        return response;
    }

    public String update(final String url, JSONObject jsonObject)
            throws RestCallException
    {
        String objectJson = jsonObject.toString();
        return update(url, objectJson);
    }

    public String update(String className, String objectId, Object o)
            throws RestCallException
    {
        final String url = new StringBuilder("https://api.parse.com/1/classes/")
                .append(className).append("/").append(objectId).toString();
        String objectJson = getGson().toJson(o);
        return update(url, objectJson);
    }

    public String delete(String className, String objectId) throws RestCallException {
        HttpClient client = getHttpClient();
        final String url = "https://api.parse.com/1/classes/" + className + "/" + objectId;
        String response = null;
        try {
            HttpDelete delete = new HttpDelete(url);
            delete.addHeader("X-Parse-Application-Id", config.get("parse.applicationID"));
            delete.addHeader("X-Parse-REST-API-Key", config.get("parse.RestAPIKey"));
            delete.addHeader("Content-Type", "application/json");

            HttpResponse httpResponse = client.execute(delete);

            final int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                final Header[] locations = httpResponse.getHeaders("Location");
                if (locations!=null && locations.length>0)
                    response = locations[0].getValue();
            }
            else
                throw new RestCallException(url, "Unexpected status code: " + statusCode);
        } catch (UnsupportedEncodingException e) {
            throw new RestCallException(e, url);
        } catch (ClientProtocolException e) {
            throw new RestCallException(e, url);
        } catch (IOException e) {
            throw new RestCallException(e, url);
        } finally {
            client.getConnectionManager().shutdown();
        }
        return response;
    }

    private String post(String url, Object o) throws RestCallException {
        HttpClient client = getHttpClient();
        String response = null;
        try {
            HttpPost post = new HttpPost(url);
            post.addHeader("X-Parse-Application-Id", config.get("parse.applicationID"));
            post.addHeader("X-Parse-REST-API-Key", config.get("parse.RestAPIKey"));
            post.addHeader("Content-Type", "application/json");

            String objectJson = createGson.toJson(o);
            post.setEntity(new StringEntity(objectJson));

            HttpResponse httpResponse = client.execute(post);

            final int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_CREATED) {
                final Header[] locations = httpResponse.getHeaders("Location");
                if (locations!=null && locations.length>0) {
                    String location = locations[0].getValue();
                    response = location.substring(location.lastIndexOf("/")+1);
                }
            }
            else
                throw new RestCallException(url, "Unexpected status code: " + statusCode);
        } catch (UnsupportedEncodingException e) {
            throw new RestCallException(e, url);
        } catch (ClientProtocolException e) {
            throw new RestCallException(e, url);
        } catch (IOException e) {
            throw new RestCallException(e, url);
        } finally {
            client.getConnectionManager().shutdown();
        }
        return response;
    }

    public String createUser(String username, String password)
            throws RestCallException
    {
        final String url = "https://api.parse.com/1/users";
        NewUser user = new NewUser(username, password);
        return post(url, user);
    }

    public String create(String className, Object o)
            throws RestCallException
    {
        final String url = "https://api.parse.com/1/classes/" + className;
        return post(url, o);
    }

    public <T> T getObject(String className, String objectId, Class<T> clazz) throws RestCallException {
        final String json = fetch("https://api.parse.com/1/classes/" + className + "/" + objectId);
        return gson.fromJson(json, clazz);
    }

    public static String replacePlaceholders(String s, String[] args) {
        for(int i=0; i<args.length; i++) {
            s = s.replaceAll("\\{" + i + "\\}", args[i]);
        }
        return s;
    }

    public boolean isInParseGuestList(final long guestId) {
        final String s = config.get("parse.guests");
        if (s==null) return false;
        StringTokenizer st = new StringTokenizer(s, ",");
        while(st.hasMoreTokens()) {
            String token = st.nextToken();
            try {
                long id = Long.valueOf(token);
                if (id==guestId)
                    return true;
            } catch (Throwable t) {}
        }
        return false;
    }

    public String getParsePasswordForUsername(final String username) {
        try {
            final String salt = config.get("parse.salt");
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            final byte[] digest = md5.digest((salt + username).getBytes());
            return new String(digest);
        }
        catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

}
