package com.fluxtream;

import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import oauth.signpost.OAuthProvider;
import oauth.signpost.OAuthProviderListener;
import oauth.signpost.http.HttpRequest;
import oauth.signpost.http.HttpResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.WordUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.fluxtream.utils.DesEncrypter;
import com.fluxtream.utils.ProxyHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpTransport;
import com.google.gdata.client.Service.GDataRequestFactory;

@Component
public class Configuration implements InitializingBean {

	private DesEncrypter encrypter;
	
	public Map<String,String> props = new ConcurrentHashMap<String,String>();
	
	public Map<String,String> connectors = new ConcurrentHashMap<String,String>();
	
	public Map<String,String> oauth = new ConcurrentHashMap<String,String>();
	
	private Map<String,String> countries;
	
	private Map<String,String> countryCodes;
	
	public void setCommonProperties(Properties properties) throws IOException {
		putAll(properties, props);
	}
	
	private void putAll(Properties source, Map<String,String> destination) {
		for(Object key:source.keySet()) {
			destination.put((String)key, (String)source.get(key));
		}
	}
	
	public void setTargetEnvProperties(Properties properties) throws IOException {
		putAll(properties, props);
	}
	
	public void setConnectorsProperties(Properties properties) throws IOException {
		putAll(properties, connectors);
		putAll(properties, props);
	}
	
	public void setOauthProperties(Properties properties) throws IOException {
		putAll(properties, oauth);
		putAll(properties, props);
	}
	
	public void setCountries(Properties properties) throws IOException {
		countries = new ConcurrentHashMap<String,String>();
		countryCodes = new ConcurrentHashMap<String,String>();
		for(Object key:properties.keySet()) {
			String code = (String) key;
			String countryName = properties.getProperty(code);
			String capitalizedCountryName = WordUtils.capitalize(countryName.toLowerCase());
			String upperCaseCountryCode = code.toUpperCase();
			countries.put(upperCaseCountryCode, capitalizedCountryName);
			countryCodes.put(capitalizedCountryName, upperCaseCountryCode);
		}
	}
	
	public String encrypt(String s) {
		return encrypter.encrypt(s);
	}
	
	public String decrypt(String s) {
		return encrypter.decrypt(s);
	}
	
	private void setupHttpProxy() {
		if (hasProxy()) {
			System.setProperty("http.proxyHost", get("proxyHost"));
			System.setProperty("http.proxyPort", get("proxyPort"));
			System.setProperty("http.proxySet", "true");
			System.setProperty("https.proxyHost", get("proxyHost"));
			System.setProperty("https.proxyPort", get("proxyPort"));
			System.setProperty("https.proxySet", "true");
			Authenticator authenticator = new ProxyAuth (get("proxyUser"), get("proxyPassword"));
			Authenticator.setDefault(authenticator);
			final LowLevelHttpTransport lowLevelHttpTransport = HttpTransport.useLowLevelHttpTransport();
			HttpTransport.setLowLevelHttpTransport(new ProxyHttpTransport(lowLevelHttpTransport, this));
		}
	}
	
	private class ProxyAuth extends Authenticator {
	    private PasswordAuthentication auth;

	    private ProxyAuth(String user, String password) {
	        auth = new PasswordAuthentication(user, password == null ? new char[]{} : password.toCharArray());
	    }

	    protected PasswordAuthentication getPasswordAuthentication() {
	        return auth;
	    }
	}

	private boolean hasProxy() {
		boolean hasProxy = (String)props.get("proxyHost")!=null;
		return hasProxy;
	}
	
	public void setProxyAuthHeaders(GDataRequestFactory requestFactory) {
		if (get("proxyUser")==null) return;
		String encodedPassword = getBase64EncodedPassword();
		requestFactory.setPrivateHeader("Proxy-Authorization", "Basic " + encodedPassword);
		requestFactory.setHeader("Proxy-Authorization", "Basic " + encodedPassword);
	}
	
	public void setProxyAuthHeaders(HttpURLConnection request) {
		if (get("proxyUser")==null) return;
		String encodedPassword = getBase64EncodedPassword();
		request.setRequestProperty( "Proxy-Authorization", "Basic " + encodedPassword );
	}
	
	private String getBase64EncodedPassword() {
		String credentials = get("proxyUser")+":"+get("proxyPassword");
		String encodedPassword = new String(Base64.encodeBase64(credentials.getBytes()));
		return encodedPassword;
	}
	
	public void setProxyAuthHeaders(OAuthProvider provider) {
		if (get("proxyUser")==null) return;
		String credentials = get("proxyUser")+":"+get("proxyPassword");
		final String encodedPassword = new String(Base64.encodeBase64(credentials.getBytes()));
		provider.setListener(new OAuthProviderListener() {

			@Override
			public boolean onResponseReceived(HttpRequest request,
					HttpResponse response) throws Exception {
				return false;
			}

			@Override
			public void prepareRequest(HttpRequest request) throws Exception {
			}

			@Override
			public void prepareSubmission(HttpRequest request) throws Exception {
				request.setHeader("Proxy-Authorization", "Basic " + encodedPassword);
			}
			
		});
	}
		
	public String get(String key) {
		String property = (String)props.get(key);
		if (property!=null)
			return property.trim();
		return null;
	}

	public long getInt(String key) {
		return Integer.valueOf(get(key));
	}

	public HttpClient getHttpClient() {
		DefaultHttpClient client = new DefaultHttpClient();
		return client;
	}

	public void setProxyAuthHeaders(LowLevelHttpRequest request) {
		if (get("proxyUser")==null) return;
		String credentials = get("proxyUser")+":"+get("proxyPassword");
		String encodedPassword = new String(Base64.encodeBase64(credentials.getBytes()));
		request.addHeader("Proxy-Authorization", "Basic " + encodedPassword);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.encrypter = new DesEncrypter(get("crypto"));
		setupHttpProxy();
	}

	public String getCountry(String geo_country_code) {
		return countries.get(geo_country_code.toUpperCase());
	}

	public String getCountryCode(String country) {
		return countryCodes.get(country);
	}
	
}
