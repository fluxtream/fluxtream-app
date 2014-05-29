package org.fluxtream.core.cors;

import org.fluxtream.core.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

@Component
public class SimpleCORSFilter implements Filter {

    @Autowired
    Configuration env;

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        setCORSHeaders(request, response);
        if (!request.getMethod().equalsIgnoreCase("OPTIONS"))
            chain.doFilter(req, res);
    }

    public void setCORSHeaders(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", getAllowedOrigin(request.getHeader("Origin")));
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Allow-Headers", "Origin, Content-Type");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Expose-Headers", "Accept-Ranges, Content-Encoding, Content-Length, Content-Range");
        response.setHeader("Access-Control-Max-Age", "3600");
    }

    public void init(FilterConfig filterConfig) {
        this.env = WebApplicationContextUtils.
                getRequiredWebApplicationContext(filterConfig.getServletContext()).
                getBean(Configuration.class);
    }

    public void destroy() {}

    public String getAllowedOrigin(String origin) {
        final ArrayList<String> allowedOrigins = (ArrayList<String>) env.getProperty("allowedOrigins");
        if (allowedOrigins.contains(origin))
            return origin;
        return allowedOrigins.iterator().next();
    }
}