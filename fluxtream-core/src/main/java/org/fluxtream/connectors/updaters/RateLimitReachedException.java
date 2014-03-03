package org.fluxtream.connectors.updaters;

@SuppressWarnings("serial")
public class RateLimitReachedException extends Exception {

	public RateLimitReachedException() {
		super();
	}

	public RateLimitReachedException(String message, Throwable cause) {
		super(message, cause);
	}

	public RateLimitReachedException(String message) {
		super(message);
	}

	public RateLimitReachedException(Throwable cause) {
		super(cause);
	}
	
	
}
