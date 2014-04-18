package org.fluxtream.core.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.Serializable;

/**
 * A holder of selected HTTP details related to an OAuth2 authentication request.
 *
 * Shamelessly copied from Spring's oAuth2 plugin (Dave Syer)
 *
 */
public class FlxOAuth2AuthenticationDetails implements Serializable {

    private static final long serialVersionUID = -4809832298438307309L;

    public static final String ACCESS_TOKEN_VALUE = FlxOAuth2AuthenticationDetails.class.getSimpleName() + ".ACCESS_TOKEN_VALUE";

    private final String remoteAddress;

    private final String sessionId;

    private final String tokenValue;

    private final String display;

    /**
     * Records the access token value and remote address and will also set the session Id if a session already exists
     * (it won't create one).
     *
     * @param request that the authentication request was received from
     */
    public FlxOAuth2AuthenticationDetails(HttpServletRequest request) {
        this.tokenValue = (String) request.getAttribute(ACCESS_TOKEN_VALUE);
        this.remoteAddress = request.getRemoteAddr();

        HttpSession session = request.getSession(false);
        this.sessionId = (session != null) ? session.getId() : null;
        StringBuilder builder = new StringBuilder();
        if (remoteAddress!=null) {
            builder.append("remoteAddress=").append(remoteAddress);
        }
        if (builder.length()>1) {
            builder.append(", ");
        }
        if (sessionId!=null) {
            builder.append("sessionId=<SESSION>");
        }
        if (builder.length()>1) {
            builder.append(", ");
        }
        if (tokenValue!=null) {
            builder.append("tokenValue=<TOKEN>");
        }
        this.display = builder.toString();
    }

    /**
     * The access token value used to authenticate the request (normally in an authorization header).
     *
     * @return the tokenValue used to authenticate the request
     */
    public String getTokenValue() {
        return tokenValue;
    }

    /**
     * Indicates the TCP/IP address the authentication request was received from.
     *
     * @return the address
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Indicates the <code>HttpSession</code> id the authentication request was received from.
     *
     * @return the session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        return display;
    }

}
