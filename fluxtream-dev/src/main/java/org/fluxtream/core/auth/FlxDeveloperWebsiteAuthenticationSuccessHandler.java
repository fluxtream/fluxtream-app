package org.fluxtream.core.auth;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: candide
 * Date: 30/04/14
 * Time: 20:47
 */
public class FlxDeveloperWebsiteAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        Map<String,Object> status = new HashMap<String,Object>();
        status.put("authd", new Boolean(true));
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(status));
    }

}
