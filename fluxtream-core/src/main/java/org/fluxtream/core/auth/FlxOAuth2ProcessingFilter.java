package org.fluxtream.core.auth;

import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.oauth2.AuthorizationToken;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.OAuth2MgmtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

/**
 * User: candide
 * Date: 18/04/14
 * Time: 18:18
 */
public class FlxOAuth2ProcessingFilter implements Filter {


    @Autowired
    OAuth2MgmtService oAuth2MgmtService;

    @Autowired
    GuestService guestService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {

    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
            ServletException {

        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        try {
            String tokenValue = parseToken(request);
            if (tokenValue!=null) {
//                request.setAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE, tokenValue);
                AuthorizationToken authToken = oAuth2MgmtService.getTokenFromAccessToken(tokenValue);
                if (authToken!=null&&authToken.getExpirationIn()>0) {
                    final Guest guest = guestService.getGuestById(authToken.guestId);
                    final FlxUserDetails userDetails = new FlxUserDetails(guest);
                    PreAuthenticatedAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken(userDetails, tokenValue,
                            getAuthorities(guest));
                    authentication.setDetails(userDetails);
                    authentication.setAuthenticated(true);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else
                    throw new Exception("No AuthorizationToken found or token expired");

            }
        }
        catch (Exception failed) {
            SecurityContextHolder.clearContext();
            response.sendError(403, "oAuth2: Sorry, we couldn't authenticate your request: " + failed.getMessage());
            return;
        }

        chain.doFilter(request, response);
    }

    private Collection<? extends GrantedAuthority> getAuthorities(final Guest one) {
        final List<String> userRoles = one.getUserRoles();
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        for (String userRole : userRoles)
            authorities.add(new SimpleGrantedAuthority(userRole));
        return authorities;
    }

    protected String parseToken(HttpServletRequest request) {
        // first check the header...
        String token = parseHeaderToken(request);

        // bearer type allows a request parameter as well
        if (token == null) {
            token = request.getParameter("access_token");
        }

        return token;
    }
    /**
     * Parse the OAuth header parameters. The parameters will be oauth-decoded.
     *
     * @param request The request.
     * @return The parsed parameters, or null if no OAuth authorization header was supplied.
     */
    protected String parseHeaderToken(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Enumeration<String> headers = request.getHeaders("Authorization");
        while (headers.hasMoreElements()) { // typically there is only one (most servers enforce that)
            String value = headers.nextElement();
            if ((value.toLowerCase().startsWith("Bearer".toLowerCase()))) {
                String authHeaderValue = value.substring("Bearer".length()).trim();
                int commaIndex = authHeaderValue.indexOf(',');
                if (commaIndex > 0) {
                    authHeaderValue = authHeaderValue.substring(0, commaIndex);
                }
                return authHeaderValue;
            }
            else {
                // todo: support additional authorization schemes for different token types, e.g. "MAC" specified by
                // http://tools.ietf.org/html/draft-hammer-oauth-v2-mac-token
            }
        }

        return null;
    }
}
