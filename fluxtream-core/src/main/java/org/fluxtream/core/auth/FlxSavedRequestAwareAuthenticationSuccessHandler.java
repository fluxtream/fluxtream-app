package org.fluxtream.core.auth;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.fluxtream.core.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: candide
 * Date: 03/10/13
 * Time: 11:17
 */
public class FlxSavedRequestAwareAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    protected final Log logger = LogFactory.getLog(this.getClass());

    @Autowired
    Configuration env;

    private RequestCache requestCache = new HttpSessionRequestCache();

    private ObjectMapper objectMapper = new ObjectMapper();
    private String mobileDefaultTargetUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        final String requestRedirect = request.getParameter("r");
        SavedRequest savedRequest = requestCache.getRequest(request, response);

        if (request.getHeader("X-DEV-WEBSITE")!=null) {
            Map<String, Object> status = new HashMap<String, Object>();
            status.put("authd", new Boolean(true));
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(status));
            return;
        } else if (request.getHeader("User-Agent").indexOf("Mobile") != -1) {
            logger.debug("Redirecting to mobile default target URL: " + mobileDefaultTargetUrl);
            getRedirectStrategy().sendRedirect(request, response, mobileDefaultTargetUrl);
        } else if (requestRedirect !=null) {
            // if there's an explicit redirect parameter it takes precedence over any saved request
            logger.debug("Redirecting to request redirection parameter: " + requestRedirect);
            getRedirectStrategy().sendRedirect(request, response, requestRedirect);
        }

        if (savedRequest != null) {
            // if saved request was an ajax call or targeting a simple asset, ignore it
            if (savedRequest.getRedirectUrl().indexOf("/api/")!=-1||
                savedRequest.getRedirectUrl().indexOf(env.get("release"))!=-1||
                savedRequest.getRedirectUrl().indexOf("static/")!=-1) {
                requestCache.removeRequest(request, response);
                getRedirectStrategy().sendRedirect(request, response, getDefaultTargetUrl());
                return;
            }
        }
        String targetUrlParameter = getTargetUrlParameter();
        if (isAlwaysUseDefaultTargetUrl() || (targetUrlParameter != null && StringUtils.hasText(request.getParameter(targetUrlParameter)))) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        clearAuthenticationAttributes(request);

        // Use the DefaultSavedRequest URL
        String targetUrl = savedRequest.getRedirectUrl();
        logger.debug("Redirecting to DefaultSavedRequest Url: " + targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);

    }

    public void setRequestCache(RequestCache requestCache) {
        this.requestCache = requestCache;
    }

    public void setMobileDefaultTargetUrl(String mobileDefaultTargetUrl) {
        this.mobileDefaultTargetUrl = mobileDefaultTargetUrl;
    }

}
