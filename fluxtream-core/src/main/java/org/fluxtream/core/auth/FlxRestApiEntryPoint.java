package org.fluxtream.core.auth;

import org.fluxtream.core.Configuration;
import org.fluxtream.core.cors.SimpleCORSFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FlxRestApiEntryPoint implements AuthenticationEntryPoint {

    @Autowired
    Configuration env;

    @Autowired
    SimpleCORSFilter simpleCORSFilter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        simpleCORSFilter.setCORSHeaders(request, response);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"result\":\"KO\",\"message\":\"Access Denied. Please log in to your Fluxtream account (%s) to access this resource\"}",
                env.get("homeBaseUrl")));
    }

}
