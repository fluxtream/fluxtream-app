package com.fluxtream.utils;

import java.io.IOException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class TestControllerTest {

    public static void main(final String[] args) throws Exception {
        String username = "yourusername";
        for(int i=0; i<10; i++)
            fetch("http://localhost:8082/api/test/setAttribute?att=çamarcheenfin", username, "yourpassword");
    }

    public static void fetch(String url, String username, String password) throws IOException {
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
            } else
                content = IOUtils.toString(response.getEntity().getContent());
        }
        finally {
            client.getConnectionManager().shutdown();
        }
        System.out.println(content);
    }

}
