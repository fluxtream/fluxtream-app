package org.fluxtream.connectors.withings;

import java.io.Serializable;


@SuppressWarnings("serial")
public class WithingsOnceResponse implements Serializable {
	public WithingsOnceResponse(){}
	private int status;
	private OnceWrapper body;
	public void setStatus(int status) {
		this.status = status;
	}
	public int getStatus() {
		return status;
	}
	public void setBody(OnceWrapper body) {
		this.body = body;
	}
	public OnceWrapper getBody() {
		return body;
	}
}