package com.fluxtream.mvc.widgets.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class WeekStatsWidgetsController {

	@RequestMapping("/widgets/stats/week/{year}/{week}")
	public ModelAndView getDayStatsWidget(@PathVariable("year") String year,
			@PathVariable("week") String week) {
		ModelAndView mav = new ModelAndView("widgets/stats/weekStats");
		return mav;
	}
	
}
