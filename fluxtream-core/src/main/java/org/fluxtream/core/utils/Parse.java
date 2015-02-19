package org.fluxtream.core.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonRawValue;
import org.codehaus.jackson.map.ObjectMapper;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.utils.gson.DateDeserializer;
import org.fluxtream.core.utils.gson.DateSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
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

    private HttpClient getHttpClient() {
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        //HttpHost proxy = new HttpHost("localhost", 8899);
        //httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        return httpClient;
    }

    private String post(String url, Object o) throws RestCallException {
        HttpClient client = getHttpClient();
        String response = null;
        try {
            HttpPost post = new HttpPost(url);
            post.addHeader("X-Parse-Application-Id", config.get("parse.applicationID"));
            post.addHeader("X-Parse-REST-API-Key", config.get("parse.RestAPIKey"));
            post.addHeader("Content-Type", "application/json; charset=utf-8");

            String objectJson = createGson.toJson(o);
            post.setEntity(new StringEntity(objectJson, "utf-8"));

            HttpResponse httpResponse = client.execute(post);

            final int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_CREATED) {
                final Header[] locations = httpResponse.getHeaders("Location");
                if (locations!=null && locations.length>0) {
                    String location = locations[0].getValue();
                    response = location.substring(location.lastIndexOf("/")+1);
                }
            }
            else {
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                response = responseHandler.handleResponse(httpResponse);
                throw new RestCallException(url, "Unexpected status code: " + statusCode + "\n" + response);
            }
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

    public static class PushNotification {

        public PushNotification(Set<String> installationIds, String message, Map<String, String> params){
            List<String> list = new ArrayList<String>();
            for (String installationId : installationIds)
                list.add(new StringBuilder("\"").append(installationId).append("\"").toString());
            where = new StringBuilder("{\"installationId\":{\"$in\":[")
                    .append(StringUtils.join(list, ",")).append("]}}").toString();
            data.putAll(params);
            data.put("alert", message);
        }

        @JsonRawValue
        public String where;

        public Map<String,String> data = new HashMap<String,String>();
    }

    public String pushNotification(Set<String> installationIds, String message, Map<String,String> params) throws RestCallException {
        HttpClient client = getHttpClient();
        String response = null;
        PushNotification notification = new PushNotification(installationIds, message, params);
        final String url = "https://api.parse.com/1/push";
        try {
            HttpPost post = new HttpPost(url);
            post.addHeader("X-Parse-Application-Id", config.get("parse.applicationID"));
            post.addHeader("X-Parse-REST-API-Key", config.get("parse.RestAPIKey"));
            post.addHeader("Content-Type", "application/json; charset=utf-8");

            ObjectMapper objectMapper = new ObjectMapper();
            final String jsonString = objectMapper.writeValueAsString(notification);
            post.setEntity(new StringEntity(jsonString, "utf-8"));

            HttpResponse httpResponse = client.execute(post);

            final int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                final Header[] locations = httpResponse.getHeaders("Location");
                if (locations!=null && locations.length>0) {
                    String location = locations[0].getValue();
                    response = location.substring(location.lastIndexOf("/")+1);
                }
            }
            else {
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                response = responseHandler.handleResponse(httpResponse);
                throw new RestCallException(url, "Unexpected status code: " + statusCode + "\n" + response);
            }
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

    public String create(String className, Object o)
            throws RestCallException
    {
        final String url = "https://api.parse.com/1/classes/" + className;
        return post(url, o);
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

    public String getServerName() {
        return config.get("parse.serverName");
    }
}
