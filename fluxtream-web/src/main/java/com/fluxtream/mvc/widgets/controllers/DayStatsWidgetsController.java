package com.fluxtream.mvc.widgets.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.services.ApiDataService;

@Controller
public class DayStatsWidgetsController {

	private List<String> userWidgets = new ArrayList<String>();

	@PersistenceContext
	EntityManager entityManager;

	@Autowired
	ApiDataService apiDataService;

	public DayStatsWidgetsController() {
		userWidgets.add("caloriesBurnt");
		userWidgets.add("hoursSlept");
		userWidgets.add("hoursSlept");
		userWidgets.add("hoursSlept");
		userWidgets.add("hoursSlept");
		userWidgets.add("hoursSlept");
	}

	@RequestMapping("/widgets/stats/date/{date}")
	public ModelAndView getDayStatsWidget(@PathVariable("date") String date) {
		ModelAndView mav = new ModelAndView("widgets/stats/dayStats");
		mav.addObject("userWidgets", userWidgets);
		return mav;
	}

	@RequestMapping("/widgets/stats/date/{date}.json")
	public void getDayStatsWidgetJson(HttpServletResponse response,
			@PathVariable("date") String date) throws IOException {
		JSONObject o = new JSONObject();
		caloriesBurnt(o);
		hoursSlept(o);
		response.getWriter().write(o.toString());
	}

	JSONObject caloriesBurnt(JSONObject o) {
		return o.accumulate("caloriesBurnt", 777);
	}

	JSONObject hoursSlept(JSONObject o) {
		return o.accumulate("hoursSlept", 555);
	}

}
