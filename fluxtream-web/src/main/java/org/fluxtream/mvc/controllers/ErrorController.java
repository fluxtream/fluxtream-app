package org.fluxtream.mvc.controllers;

import org.fluxtream.core.Configuration;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class ErrorController {

	FlxLogger logger = FlxLogger.getLogger(ErrorController.class);

	@Autowired
	Configuration env;

	public final static String SERVLET_EXCEPTION_ATTR = "javax.servlet.error.exception";

	public final static String JSP_EXCEPTION_ATTR = "javax.servlet.jsp.jspException";

    @RequestMapping(value = "/accessDenied")
    public String accessDenied(HttpServletRequest request,
                                     HttpServletResponse response) throws IOException {
        if (request.getParameter("json")!=null) {
            String baseUrl = env.get("homeBaseUrl");
            response.getWriter().write(String.format("{\"result\":\"KO\",\"message\":\"Access Denied. Please log in to your Fluxtream account (%s) to access this resource\"}", baseUrl));
            return null;
        }
        else return "accessDenied";
    }

    public ModelAndView handleError(int code,
                                    String errorMessage,
                                    String stackTrace) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("code", code);
        mav.addObject("errorMessage", errorMessage);
        mav.addObject("stackTrace", stackTrace);
        mav.addObject("release", env.get("release"));
        return mav;
    }


    @RequestMapping(value = "/error/{code}.html")
	public ModelAndView handleError(@PathVariable("code") int code,
			HttpServletRequest request) {
		ModelAndView mav = new ModelAndView("error");
		Throwable e = (Throwable) request.getAttribute(SERVLET_EXCEPTION_ATTR);
		if (e==null)
			e = (Throwable) request.getAttribute(JSP_EXCEPTION_ATTR);
		String stackTrace = null;
		if (e!=null) {
			stackTrace = Utils.stackTrace(e);
			mav.addObject("stackTrace", stackTrace);
		}
		switch(code) {
		case 404:
			String uri = (String) request.getAttribute("javax.servlet.forward.request_uri");
			handle404(mav, uri);
			break;
		case 500:
			handle500(mav, e);
			break;
		default:
			handle500(mav, e);
		}
		mav.addObject("release", env.get("release"));
		mav.addObject("code", code);
		return mav;
	}
	
	void handle404(ModelAndView mav, String uri) {
		logger.warn("httpError=404 uri=" + uri);
		mav.addObject("errorMessage", "Oups, we couldn't find this page");
	}

	void handle500(final ModelAndView mav, Throwable t) {
		String message = "no available message";
		if (t!=null) {
			message=t.getMessage();
			if (message != null && message.length()>384) message=message.substring(0, 384);
		}
		logger.error("httpError=500 message=" + message);
		mav.addObject("errorMessage", "Server error");
	}
	
}
