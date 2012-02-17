package com.fluxtream.mvc.models;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HelpController {

	@RequestMapping("/help/clock")
	public String getClockHelp(HttpServletResponse response) {
		return "help/clock";
	}
	
}
