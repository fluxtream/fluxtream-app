package org.fluxtream.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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


    public static DefaultHttpClient httpClientTrustingAllSSLCerts() throws NoSuchAlgorithmException, KeyManagementException {
        DefaultHttpClient httpclient = new DefaultHttpClient();

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, getTrustingManager(), new java.security.SecureRandom());

        SSLSocketFactory socketFactory = new SSLSocketFactory(sc);
        Scheme sch = new Scheme("https", socketFactory, 443);
        httpclient.getConnectionManager().getSchemeRegistry().register(sch);
        return httpclient;
    }

    private static TrustManager[] getTrustingManager() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

        }};
        return trustAllCerts;
    }


    public static String fetch(String url, String username, String password) throws IOException {
        HttpClient client = new DefaultHttpClient();
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

    public static String fetch(String url) throws IOException, UnexpectedHttpResponseCodeException {
        return fetch(url, new BasicResponseHandler());
    }

    /** Calls the given <code>url</code> and returns the contents as a <code>byte[]</code>. */
    public static byte[] fetchBinary(final String url) throws UnexpectedHttpResponseCodeException, IOException {
        return fetch(url, BINARY_RESPONSE_HANDLER);
    }

    public static String fetch(String url, Map<String, String> params) throws UnexpectedHttpResponseCodeException, IOException {
        HttpClient client = new DefaultHttpClient();
        String content = "";
        try {
            HttpPost post = new HttpPost(url);

            Iterator<Entry<String, String>> iterator = params.entrySet().iterator();

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(params.size());
            while (iterator.hasNext()) {
                Entry<String, String> entry = iterator.next();
                nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs, "utf-8"));

            HttpResponse response = client.execute(post);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                content = responseHandler.handleResponse(response);
            }
            else {
                throw new UnexpectedHttpResponseCodeException(response.getStatusLine().getStatusCode(),
                                                              response.getStatusLine().getReasonPhrase());
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
                    if (body!=null)
                        post.setEntity(new StringEntity(body, "utf-8"));
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

    public static String fetch(final String url, final String body) throws UnexpectedHttpResponseCodeException, IOException {
        return fetch(url, body, null, -1);
    }

    public static String fetch(final String url, String body, String proxyHost, int proxyPort)
            throws UnexpectedHttpResponseCodeException, IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        if (proxyHost!=null)
            setProxy(client, proxyHost, proxyPort);
        String content = null;
        try {
            HttpPost post = new HttpPost(url);
            if (body!=null)
                post.setEntity(new StringEntity(body, "utf-8"));
            HttpResponse response = client.execute(post);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                content = responseHandler.handleResponse(response);
            }
            else {
                throw new UnexpectedHttpResponseCodeException(response.getStatusLine().getStatusCode(),
                                                              response.getStatusLine().getReasonPhrase());
            }
        }
        finally {
            client.getConnectionManager().shutdown();
        }
        return content;
    }

    public static void postAsync(final String url, final String body) {
        new Thread() {
            public void run() {
                DefaultHttpClient client = new DefaultHttpClient();
                try {
                    HttpPost post = new HttpPost(url);
                    if (body!=null)
                        post.setEntity(new StringEntity(body, "utf-8"));
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

    private static <T> T fetch(final String url, final ResponseHandler<T> responseHandler) throws UnexpectedHttpResponseCodeException, IOException {
        HttpClient client = new DefaultHttpClient();

        T content;
        try {
            HttpGet get = new HttpGet(url);

            HttpResponse response = client.execute(get);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                content = responseHandler.handleResponse(response);
            }
            else {
                throw new UnexpectedHttpResponseCodeException(response.getStatusLine().getStatusCode(),
                                                              response.getStatusLine().getReasonPhrase());
            }
        }
        finally {
            client.getConnectionManager().shutdown();
        }
        return content;
    }

    public static void setProxy(final DefaultHttpClient client, String proxyHost, int proxyPort) {
        HttpHost proxy = new HttpHost(proxyHost, proxyPort);
        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
    }

}
