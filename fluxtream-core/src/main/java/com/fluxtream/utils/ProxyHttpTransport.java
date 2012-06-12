package com.fluxtream.utils;

import java.io.IOException;

import com.fluxtream.Configuration;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpTransport;

public class ProxyHttpTransport extends LowLevelHttpTransport {

	private LowLevelHttpTransport transport;
	private Configuration env;
	
	public ProxyHttpTransport (LowLevelHttpTransport transport, Configuration env) {
		this.transport = transport;
		this.env = env;
	}
	
	@Override
	public LowLevelHttpRequest buildDeleteRequest(String urlString)
			throws IOException {
		LowLevelHttpRequest request = transport.buildDeleteRequest(urlString);
		this.env.setProxyAuthHeaders(request);
		return request;
	}

	@Override
	public LowLevelHttpRequest buildGetRequest(String urlString)
			throws IOException {
		LowLevelHttpRequest request = transport.buildGetRequest(urlString);
		this.env.setProxyAuthHeaders(request);
		return request;
	}

	@Override
	public LowLevelHttpRequest buildPostRequest(String urlString)
			throws IOException {
		LowLevelHttpRequest request = transport.buildPostRequest(urlString);
		this.env.setProxyAuthHeaders(request);
		return request;
	}

	@Override
	public LowLevelHttpRequest buildPutRequest(String urlString)
			throws IOException {
		LowLevelHttpRequest request = transport.buildPutRequest(urlString);
		this.env.setProxyAuthHeaders(request);
		return request;
	}
	
}
