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

public class FlxAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void onAuthenticationFailure(HttpServletRequest request,
			HttpServletResponse response, AuthenticationException exception)
			throws IOException, ServletException {
        if (request.getHeader("X-DEV-WEBSITE")!=null) {
            Map<String, Object> status = new HashMap<String, Object>();
            status.put("authd", new Boolean(false));
            status.put("message", exception.getMessage());
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(status));
        } else if (request.getHeader("User-Agent").indexOf("Mobile") != -1) {
            setDefaultFailureUrl("/mobile/signIn?username=" + request.getParameter("f_username"));
        } else {
            setUseForward(false);
            setDefaultFailureUrl("/welcome?username=" + request.getParameter("f_username"));
        }
		super.onAuthenticationFailure(request, response, exception);
	}

	
	
}
