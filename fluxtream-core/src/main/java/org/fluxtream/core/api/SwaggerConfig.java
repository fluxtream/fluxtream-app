package org.fluxtream.core.api;

import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.model.*;
import org.fluxtream.core.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * User: candide
 * Date: 20/04/14
 * Time: 23:00
 */
@Component
public class SwaggerConfig implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    Configuration env;

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        if (event.getApplicationContext().getDisplayName().equals("Root WebApplicationContext")) {
            System.out.println("ApplicationContext started, setting up REST API info");
            ApiInfo apiInfo = new ApiInfo(
                "Fluxtream Public REST API",
                "",
                "https://fluxtream.org/html/privacyPolicy.html",
                "info@fluxtream.org",
                "Apache 2.0",
                "http://www.apache.org/licences/LICENSE-2.0.html"
            );
            List<GrantType> grantTypes = new ArrayList<GrantType>();
            LoginEndpoint loginEndpoint = new LoginEndpoint(env.get("homeBaseUrl") + "auth/oauth2/dialog");
            ImplicitGrant implicitGrant = new ImplicitGrant(loginEndpoint, "accessToken");
            grantTypes.add(implicitGrant);
            AuthorizationType oauth = new OAuthBuilder().grantTypes(grantTypes).build();
            ConfigFactory.config().addAuthorization(oauth);
            ConfigFactory.config().setApiInfo(apiInfo);
        }
    }
}
