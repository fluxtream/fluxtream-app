package org.fluxtream.core.auth;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: candide
 * Date: 30/04/14
 * Time: 20:52
 */
public class FlxDeveloperWebsiteAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        Map<String,Object> status = new HashMap<String,Object>();
        status.put("authd", new Boolean(false));
        status.put("message", exception.getMessage());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(status));
    }
}
