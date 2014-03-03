package org.fluxtream.utils;

/**
 * User: candide
 * Date: 02/09/13
 * Time: 11:05
 */
public class UnexpectedHttpResponseCodeException extends Exception {

    public Integer getHttpResponseCode() {
        return httpResponseCode;
    }

    public String getHttpResponseMessage() {
        return httpResponseMessage;
    }

    private final Integer httpResponseCode;
    private final String httpResponseMessage;

    public UnexpectedHttpResponseCodeException(Integer httpResponseCode, String httpResponseMessage) {
        super("Unexpected HTTP Response Code: " + httpResponseCode);
        this.httpResponseCode = httpResponseCode;
        this.httpResponseMessage = httpResponseMessage;
    }
}
