package com.fluxtream.mvc.widgets.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class DayStatsWidgetsController {

	@RequestMapping("/widgets/stats/date/{date}")
	public ModelAndView getDayStatsWidget(@PathVariable("date") String date) {
		ModelAndView mav = new ModelAndView("widgets/stats/dayStats");
		return mav;
	}
	
}
