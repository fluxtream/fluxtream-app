package com.fluxtream.auth;

import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import com.fluxtream.Configuration;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.domain.Guest;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.JPADaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class FlxAuthFilter extends UsernamePasswordAuthenticationFilter {

	FlxLogger logger = FlxLogger
			.getLogger(UsernamePasswordAuthenticationFilter.class);

	@Autowired
	GuestService guestService;
	
	@Autowired
	Configuration env;

    @Autowired
    JPADaoService jpaDaoService;

	@Override
	public Authentication attemptAuthentication(
			javax.servlet.http.HttpServletRequest request,
			javax.servlet.http.HttpServletResponse response)
			throws AuthenticationException {
        final String autoLoginToken = request.getParameter("autoLoginToken");
        if (autoLoginToken !=null) {
            final Guest one = jpaDaoService.findOne("guest.byAutoLoginToken", Guest.class, autoLoginToken);

            if (one!=null) {
                if ((System.currentTimeMillis()-one.autoLoginTokenTimestamp)>60000) {
                    throw new RuntimeException("Token is too old!");
                }
                final FlxUserDetails details = new FlxUserDetails(one);
                final UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(details, one.password, Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
                authRequest.setDetails(details);
                jpaDaoService.execute("UPDATE Guest SET autoLoginToken=null WHERE autoLoginToken='" + autoLoginToken + "'");
                return authRequest;
            } else
                throw new RuntimeException("No such autologin token: " + autoLoginToken);
        }
		Authentication authentication = super.attemptAuthentication(request, response);
		return authentication;
	}

	@Override
	protected String obtainPassword(HttpServletRequest request) {
		return request.getParameter("f_password");
	}

	@Override
	protected String obtainUsername(HttpServletRequest request) {
		return request.getParameter("f_username");
	}

}
