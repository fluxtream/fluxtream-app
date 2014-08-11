package org.fluxtream.core.auth;

import org.fluxtream.core.Configuration;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.JPADaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FlxAuthFilter extends UsernamePasswordAuthenticationFilter {

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
                final UsernamePasswordAuthenticationToken authRequest =
                        new UsernamePasswordAuthenticationToken(details, one.password, getAuthorities(one));
                authRequest.setDetails(details);
                jpaDaoService.execute("UPDATE Guest SET autoLoginToken=null WHERE autoLoginToken='" + autoLoginToken + "'");
                return authRequest;
            } else
                throw new RuntimeException("No such autologin token: " + autoLoginToken);
        }
        Authentication authentication = null;
        try { authentication = super.attemptAuthentication(request, response);}
        catch (AuthenticationException failed) {
            authentication = attemptAuthenticationWithEmailAddress(request);
        }
        return authentication;
    }

    public Authentication attemptAuthenticationWithEmailAddress(HttpServletRequest request) throws AuthenticationException {

        String email = obtainUsername(request);
        String password = obtainPassword(request);

        final Guest guest = guestService.getGuestByEmail(email);
        String username = null;
        if (guest!=null) {
            username = guest.username;
        }

        if (username == null) {
            username = "";
        }

        if (password == null) {
            password = "";
        }

        username = username.trim();

        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);

        // Allow subclasses to set the "details" property
        setDetails(request, authRequest);

        return this.getAuthenticationManager().authenticate(authRequest);
    }

    private Collection<? extends GrantedAuthority> getAuthorities(final Guest one) {
        final List<String> userRoles = one.getUserRoles();
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        for (String userRole : userRoles)
            authorities.add(new SimpleGrantedAuthority(userRole));
        return authorities;
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
