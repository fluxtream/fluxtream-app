package org.fluxtream.core.api;

        import com.wordnik.swagger.config.ConfigFactory;
        import com.wordnik.swagger.config.ScannerFactory;
        import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
        import com.wordnik.swagger.jaxrs.config.WebXMLReader;
        import com.wordnik.swagger.jaxrs.listing.ApiListingCache;
        import com.wordnik.swagger.jersey.JerseyApiReader;
        import com.wordnik.swagger.model.ApiInfo;
        import com.wordnik.swagger.reader.ClassReaders;
        import org.fluxtream.core.Configuration;
        import org.springframework.web.context.WebApplicationContext;
        import org.springframework.web.context.support.WebApplicationContextUtils;
        import scala.collection.Iterator;
        import scala.collection.JavaConversions;
        import scala.collection.immutable.List;

        import javax.servlet.ServletConfig;
        import javax.servlet.ServletException;
        import javax.servlet.http.HttpServlet;
        import javax.servlet.http.HttpServletRequest;
        import javax.servlet.http.HttpServletResponse;
        import javax.ws.rs.core.Application;
        import java.io.IOException;
        import java.util.ArrayList;

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
        final DefaultJaxrsScanner jaxrsScanner = new DefaultJaxrsScanner(){

            @Override
            public List<Class<?>> classesFromContext(Application app, ServletConfig sc) {
                final List<Class<?>> classes = super.classesFromContext(app, sc);
                return filterClasses(classes);
            }

            private List<Class<?>> filterClasses(final List<Class<?>> classes) {
                final Iterator<Class<?>> eachClass = classes.iterator();
                final ArrayList<Class<?>> filteredClasses = new ArrayList<Class<?>>();
                while(eachClass.hasNext()) {
                    Class clazz = eachClass.next();
//                    if (clazz.getName().indexOf("fluxtream")!=-1)
//                        continue;
                    filteredClasses.add(clazz);
                }
                return JavaConversions.asScalaBuffer(filteredClasses).toList();
            }

            @Override
            public List<Class<?>> classes() {
                final List<Class<?>> classes = super.classes();
                return filterClasses(classes);
            }
        };
        ScannerFactory.setScanner(jaxrsScanner);
        ClassReaders.setReader(new JerseyApiReader());
        String docsBaseURL = env.get("docsHomeBaseUrl")!=null
                           ? env.get("docsHomeBaseUrl")
                           : env.get("homeBaseUrl");
        ApiInfo apiInfo = new ApiInfo(
                "Fluxtream Public REST API",
                "",
                String.format("%shtml/privacyPolicy.html", docsBaseURL),
                "info@fluxtream.org",
                "Apache 2.0",
                "http://www.apache.org/licences/LICENSE-2.0.html"
        );
        ConfigFactory.config().setBasePath(docsBaseURL+"api");
        ConfigFactory.config().setApiInfo(apiInfo);
        ConfigFactory.config().setApiVersion("v1");
        env.reload();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        reload();
        com.wordnik.swagger.jersey.listing.ApiListingCache.invalidateCache();
        ApiListingCache.invalidateCache();
        resp.getWriter().write("oauth config reloaded - cache invalidated twice");
    }
}
