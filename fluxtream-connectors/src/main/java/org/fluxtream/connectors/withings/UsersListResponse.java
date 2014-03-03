package org.fluxtream.connectors.withings;

import java.io.Serializable;

@SuppressWarnings("serial")
public class UsersListResponse implements Serializable {

	int status;
	
	UsersListResponseBody body;

	public UsersListResponse() {}
	
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public UsersListResponseBody getBody() {
		return body;
	}

	public void setBody(UsersListResponseBody body) {
		this.body = body;
	}
	
}
