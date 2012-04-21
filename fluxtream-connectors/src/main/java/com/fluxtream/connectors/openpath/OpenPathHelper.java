package com.fluxtream.connectors.openpath;

public class OpenPathHelper {

	private String accessKey, secretKey;
	
	public OpenPathHelper(String accessKey, String secretKey) {
		super();
		this.accessKey = accessKey;
		this.secretKey = secretKey;
	}

	public boolean testConnection() {
		return false;
	}

}
