package org.fluxtream.core.api;

import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jaxrs.config.WebXMLReader;
import com.wordnik.swagger.jersey.JerseyApiReader;
import com.wordnik.swagger.model.*;
import com.wordnik.swagger.reader.ClassReaders;
import org.fluxtream.core.Configuration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.util.ArrayList;
import java.util.List;

/**
 * User: candide
 * Date: 20/04/14
 * Time: 23:00
 */
public class SwaggerBootstrapServlet extends HttpServlet {

    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        WebApplicationContext webContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        Configuration env = webContext.getBean(org.fluxtream.core.Configuration.class);

        System.out.println("ApplicationContext started, setting up REST API info");
        ConfigFactory.setConfig(new WebXMLReader(config));
        ScannerFactory.setScanner(new DefaultJaxrsScanner());
        ClassReaders.setReader(new JerseyApiReader());
        ApiInfo apiInfo = new ApiInfo(
                "Fluxtream Public REST API",
                "",
                String.format("%shtml/privacyPolicy.html", env.get("homeBaseUrl")),
                "info@fluxtream.org",
                "Apache 2.0",
                "http://www.apache.org/licences/LICENSE-2.0.html"
        );
        List<GrantType> grantTypes = new ArrayList<GrantType>();
        LoginEndpoint loginEndpoint = new LoginEndpoint(String.format("%sauth/oauth2/dialog", env.get("homeBaseUrl")));
        ImplicitGrant implicitGrant = new ImplicitGrant(loginEndpoint, "accessToken");
        grantTypes.add(implicitGrant);
        AuthorizationType oauth = new OAuthBuilder().grantTypes(grantTypes).build();
        ConfigFactory.config().addAuthorization(oauth);
        ConfigFactory.config().setApiInfo(apiInfo);
    }
}
