package org.fluxtream.core.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class RestCallException extends Exception {

    public String url, message;

    public RestCallException(String url, String message) {
        this.url = url;
        this.message = message;
    }

    public RestCallException(Exception cause, String url) {
        super(cause);
        try {
            this.url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {}
    }

    public String getMessage() {
        return this.message;
    }


}
