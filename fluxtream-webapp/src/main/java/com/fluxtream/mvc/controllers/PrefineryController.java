package com.fluxtream.mvc.controllers;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.Configuration;

@Controller
public class PrefineryController {

	Logger logger = Logger.getLogger(PrefineryController.class);
	
	@Autowired
	Configuration env;
	
	@RequestMapping(value="/get-invitation")
	public ModelAndView getInvitation(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		logger.info("action=get_invitation");
		
		ModelAndView mav = new ModelAndView("prefinery/invitation");
		mav.addObject("release", env.get("release"));
		return mav;
	}

}
