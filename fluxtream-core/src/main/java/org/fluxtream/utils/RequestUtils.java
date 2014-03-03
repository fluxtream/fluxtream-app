package org.fluxtream.utils;

import javax.servlet.http.HttpServletRequest;

public class RequestUtils {
    /**
     * NOTE(candu): This also works for /etc/hosts overrides,
     * and it correctly distinguishes requests over 10.x.x.x,
     * 192.168.x.x, etc. from localhost requests.
     *
     * @param request  the incoming HTTP request
     * @return         whether this request originated locally
     */
    public static boolean isLocal(HttpServletRequest request) {
        return request.getLocalName().equals("localhost");
    }

    /**
     * NOTE(candu): Currently this just wraps isLocal(), but it
     * could be used to identify other test/development environments.
     *
     * @param request  the incoming HTTP request
     * @return         whether this request is from a dev environment
     */
    public static boolean isDev(HttpServletRequest request) {
        return isLocal(request);
    }
}
