package com.fluxtream;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.WordUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.fluxtream.utils.DesEncrypter;
import com.google.api.client.http.LowLevelHttpRequest;

@Component
public class Configuration implements InitializingBean {

	private DesEncrypter encrypter;
	
	public AutoReloadProperties commonProperties;
	
	public AutoReloadProperties targetEnvironmentProps;
	
	public AutoReloadProperties connectors;
	
	public AutoReloadProperties oauth;
	
	private Map<String,String> countries;
	
	private Map<String,String> countryCodes;
	
	public void setCommonProperties(AutoReloadProperties properties) throws IOException {
		this.commonProperties = properties;
	}
	
	public void setTargetEnvProperties(AutoReloadProperties properties) throws IOException {
		this.targetEnvironmentProps = properties;
	}
	
	public void setConnectorsProperties(AutoReloadProperties properties) throws IOException {
		this.connectors = properties;
	}
	
	public void setOauthProperties(AutoReloadProperties properties) throws IOException {
		this.oauth = properties;
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
	
	public String get(String key) {
		String property = (String)commonProperties.get(key);
		if (property==null)
			property = targetEnvironmentProps.get(key);
		if (property==null)
			property = oauth.get(key);
		if (property==null)
			property = connectors.get(key);
		if (property!=null) return property.trim();
		return property;
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
	}

	public String getCountry(String geo_country_code) {
		return countries.get(geo_country_code.toUpperCase());
	}

	public String getCountryCode(String country) {
		return countryCodes.get(country);
	}
	
}
