package org.fluxtream.core.api;

import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jaxrs.config.WebXMLReader;
import com.wordnik.swagger.jersey.JerseyApiReader;
import com.wordnik.swagger.model.ApiInfo;
import com.wordnik.swagger.reader.ClassReaders;
import org.fluxtream.core.Configuration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * User: candide
 * Date: 20/04/14
 * Time: 23:00
 */
public class SwaggerBootstrapServlet extends HttpServlet {

    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        ConfigFactory.setConfig(new WebXMLReader(config));
        reload();
    }

    private void reload() {
        System.out.println("ApplicationContext started, setting up REST API info");
        WebApplicationContext webContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());

        Configuration env = webContext.getBean(Configuration.class);
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
        ConfigFactory.config().setBasePath(env.get("homeBaseUrl")+"api/v1");
        ConfigFactory.config().setApiInfo(apiInfo);
        ConfigFactory.config().setApiVersion("v1");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        reload();
        resp.getWriter().write("oauth config reloaded");
    }
}
