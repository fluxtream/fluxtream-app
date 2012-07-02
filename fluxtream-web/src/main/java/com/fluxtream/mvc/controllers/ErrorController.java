package com.fluxtream.mvc.controllers;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.Configuration;
import com.fluxtream.utils.Utils;

@Controller
public class ErrorController {

	Logger logger = Logger.getLogger(ErrorController.class);

	@Autowired
	Configuration env;

	public final static String SERVLET_EXCEPTION_ATTR = "javax.servlet.error.exception";

	public final static String JSP_EXCEPTION_ATTR = "javax.servlet.jsp.jspException";

	@RequestMapping(value = "/errors/{code}.html")
	public ModelAndView handle404(@PathVariable("code") int code,
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
