package org.fluxtream.core.mvc.view;

import org.fluxtream.core.domain.oauth2.Application;
import org.fluxtream.core.domain.oauth2.AuthorizationToken;
import org.fluxtream.core.services.OAuth2MgmtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.servlet.View;

import java.util.Locale;

/**
 * This view resolver extends the base spring UrlBasedViewResolver by adding a override to
 * redirects to the 'callback url' that results from a successfull add connector dance for when
 * the dance has been initiated by an oauth call: in that case, the calling website will typically
 * want the user's browser to be redirected to one of its own URLs - that URL is configured in the
 * (oauth-managed) Application's &lt;code&gt;addConnectorCallbackURL&lt;/code&gt; property.
 */
public class UrlBasedViewResolver extends org.springframework.web.servlet.view.UrlBasedViewResolver {

    private static final String ADD_CONNECTOR_SUCCESS_CALLBACK_REDIRECT = "/app/from/";

    @Autowired
    OAuth2MgmtService oAuth2MgmtService;

    @Override
    protected View createView(String viewName, Locale locale) throws Exception {
        if (viewName.startsWith(REDIRECT_URL_PREFIX)) {
            String location = viewName.substring(REDIRECT_URL_PREFIX.length());
            if (location.startsWith(ADD_CONNECTOR_SUCCESS_CALLBACK_REDIRECT)) {
                final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication instanceof PreAuthenticatedAuthenticationToken) {
                    PreAuthenticatedAuthenticationToken authToken = (PreAuthenticatedAuthenticationToken) authentication;
                    final Object credentials = authToken.getCredentials();
                    if (credentials instanceof AuthorizationToken) {
                        AuthorizationToken token = (AuthorizationToken) credentials;
                        final Application applicationForToken = oAuth2MgmtService.getApplicationForToken(token);
                        if (applicationForToken!=null) {
                            String addConnectorCallbackURL = applicationForToken.addConnectorCallbackURL;
                            if (addConnectorCallbackURL != null) {
                                String connectorName = location.substring(location.lastIndexOf("/") + 1);
                                addConnectorCallbackURL += addConnectorCallbackURL.indexOf("?") == -1
                                        ? "?connectorName=" + connectorName
                                        : "=connectorName=" + connectorName;
                                return super.createView("redirect:" + addConnectorCallbackURL, locale);
                            }
                        }
                    }
                }
            }
        }
        return super.createView(viewName, locale);
    }
}
