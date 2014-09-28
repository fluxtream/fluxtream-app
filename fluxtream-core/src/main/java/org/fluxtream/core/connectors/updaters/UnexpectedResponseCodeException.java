package org.fluxtream.core.connectors.updaters;

/**
 * <p>
 * <code>UnexpectedResponseCodeException</code> does something...
 * </p>
 *
 * @author Anne Wright (anne.r.wright@gmail.com)
 */
public class UnexpectedResponseCodeException extends Exception {
    public int responseCode;
    public String URL;

	public UnexpectedResponseCodeException() {
		super();
	}

	public UnexpectedResponseCodeException(int httpResponseCode, String httpResponseMessageString, String url, Throwable cause) {
        super(httpResponseMessageString, cause);
        responseCode = httpResponseCode;
        URL=url;
	}

	public UnexpectedResponseCodeException(int httpResponseCode, String httpResponseMessageString, String url) {
		super(httpResponseMessageString);
        responseCode = httpResponseCode;
        URL=url;
	}

	public UnexpectedResponseCodeException(Throwable cause) {
		super(cause);
	}

}
