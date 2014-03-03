package org.fluxtream.connectors.withings;

import java.io.Serializable;

@SuppressWarnings("serial")
public class OnceWrapper implements Serializable {
	public OnceWrapper(){}
	private String once;

	public void setOnce(String once) {
		this.once = once;
	}

	public String getOnce() {
		return once;
	}
}
