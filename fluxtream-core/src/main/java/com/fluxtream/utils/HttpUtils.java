package com.fluxtream.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.fluxtream.Configuration;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class HttpUtils {

    private static final ResponseHandler<byte[]> BINARY_RESPONSE_HANDLER = new ResponseHandler<byte[]>() {
        /** Returns the contents of the response as a <code>byte[]</code>. */
        @Override
        public byte[] handleResponse(final HttpResponse response) throws IOException {
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                final InputStream in = entity.getContent();
                if (in != null) {
                    return IOUtils.toByteArray(in);
                }
            }
            return new byte[0];
        }
    };

    public static String fetch(String url, Configuration env, String username, String password) throws IOException {
        HttpClient client = env.getHttpClient();
        String content = "";
        try {
            HttpGet get = new HttpGet(url);
            String credentials = username + ":" + password;
            final String encodedPassword = new String(Base64.encodeBase64(credentials.getBytes()));
            get.setHeader("Authorization", "Basic " + encodedPassword);

            HttpResponse response = client.execute(get);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                content = responseHandler.handleResponse(response);
            }
        }
        finally {
            client.getConnectionManager().shutdown();
        }
        return content;
    }

    public static String fetch(String url, Configuration env) throws IOException {
        return fetch(url, env, new BasicResponseHandler());
    }

    /** Calls the given <code>url</code> and returns the contents as a <code>byte[]</code>. */
    public static byte[] fetchBinary(final String url, final Configuration env) throws IOException {
        return fetch(url, env, BINARY_RESPONSE_HANDLER);
    }

    public static String fetch(String url, Map<String, String> params, Configuration env) throws IOException {
        HttpClient client = env.getHttpClient();
        String content = "";
        try {
            HttpPost post = new HttpPost(url);

            Iterator<Entry<String, String>> iterator = params.entrySet().iterator();

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(params.size());
            while (iterator.hasNext()) {
                Entry<String, String> entry = iterator.next();
                nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = client.execute(post);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                content = responseHandler.handleResponse(response);
            }
            else {
                throw new RuntimeException(response.getStatusLine().toString());
            }
        }
        finally {
            client.getConnectionManager().shutdown();
        }
        return content;
    }

    public static void post(final String url, final String body) {
        new Thread() {
            public void run() {
                DefaultHttpClient client = new DefaultHttpClient();
                try {
                    HttpPost post = new HttpPost(url);
                    post.setEntity(new StringEntity(body));
                    client.execute(post);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    client.getConnectionManager().shutdown();
                }
            }
        }.start();
    }

    private static <T> T fetch(final String url, final Configuration env, final ResponseHandler<T> responseHandler) throws IOException {
        HttpClient client = env.getHttpClient();

        T content;
        try {
            HttpGet get = new HttpGet(url);

            HttpResponse response = client.execute(get);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                content = responseHandler.handleResponse(response);
            }
            else {
                throw new RuntimeException(response.getStatusLine().toString());
            }
        }
        finally {
            client.getConnectionManager().shutdown();
        }
        return content;
    }
}
