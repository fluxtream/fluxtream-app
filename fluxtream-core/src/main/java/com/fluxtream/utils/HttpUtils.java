package com.fluxtream.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpException;
import org.apache.http.HttpResponse;
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

import com.fluxtream.Configuration;

public class HttpUtils {

	public static final String fetch(String url, Configuration env, String username, String password) throws HttpException, IOException {
		HttpClient client = env.getHttpClient();
		String content = "";
		try {
	        HttpGet get = new HttpGet(url);
			String credentials = username+":"+password;
			final String encodedPassword = new String(Base64.encodeBase64(credentials.getBytes()));
	        get.setHeader("Authorization", "Basic " + encodedPassword);
	        
	        HttpResponse response = client.execute(get);
	        
	        if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				content = responseHandler.handleResponse(response);
			}
		} finally {
			client.getConnectionManager().shutdown();
		}
		return content;
	}

	public static final String fetch(String url, Configuration env) throws HttpException, IOException {
		HttpClient client = env.getHttpClient();
		String content = "";
		try {
	        HttpGet get = new HttpGet(url);
	
	        HttpResponse response = client.execute(get);
	        
	        if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				content = responseHandler.handleResponse(response);
			} else {
				throw new RuntimeException(response.getStatusLine().toString());
			}
		} finally {
			client.getConnectionManager().shutdown();
		}
		return content;
	}
	
	public static final String fetch(String url, Map<String,String> params, Configuration env) throws HttpException, IOException {
		HttpClient client = env.getHttpClient();
		String content = "";
		try {
	        HttpPost post = new HttpPost(url);
	        
			Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
	
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(params.size());
			while (iterator.hasNext()) {
				Entry<String,String> entry = iterator.next();
		        nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
			}
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			
	        HttpResponse response = client.execute(post);
	
			if (response.getStatusLine().getStatusCode() == 200) {
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				content = responseHandler.handleResponse(response);
			} else {
				throw new RuntimeException(response.getStatusLine().toString());
			}
		} finally {
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
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					client.getConnectionManager().shutdown();
				}
			}
		}.start();
	}

}
