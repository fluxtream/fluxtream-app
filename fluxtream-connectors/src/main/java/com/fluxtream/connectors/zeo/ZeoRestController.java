package com.fluxtream.connectors.zeo;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.GuestService;
import com.fluxtream.utils.HttpUtils;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/zeo")
public class ZeoRestController {

	Logger logger = Logger.getLogger(ZeoRestController.class);

	@Autowired
	GuestService guestService;

	@Autowired
	Configuration env;

    @Qualifier("apiDataServiceImpl")
    @Autowired
	ApiDataService apiDataService;

	@RequestMapping(value = "/subscribe")
	public ModelAndView userSubscribe(HttpServletRequest request) throws IOException {
        ModelAndView mav = new ModelAndView("connectors/withings/enterCredentials");
        return mav;
	}
	
	@RequestMapping(value = "/submitCredentials")
	public ModelAndView userSubscribed(@PathVariable final Long guestId, HttpServletRequest request,
                                       HttpServletResponse response) throws Exception {

        ModelAndView mav = new ModelAndView("connectors/withings/success");
        mav.addObject("guestId", guestId);
        return mav;
	}

}
