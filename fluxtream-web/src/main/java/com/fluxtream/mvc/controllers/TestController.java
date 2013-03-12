package com.fluxtream.mvc.controllers;

import javax.servlet.http.HttpServletRequest;
import com.fluxtream.Configuration;
import com.fluxtream.services.GuestService;
import com.fluxtream.utils.RequestUtils;
import com.fluxtream.aspects.FlxLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class TestController {
    FlxLogger logger = FlxLogger.getLogger(TestController.class);

    @Autowired
    Configuration env;

    @Autowired
    GuestService guestService;

    @RequestMapping(value = "/test/unit")
    public ModelAndView unit(HttpServletRequest request) {
        // check that we're running locally
        if (!RequestUtils.isDev(request)) {
            return new ModelAndView("redirect:/welcome");
        }
        ModelAndView mav = new ModelAndView("test/unit");
        String release = env.get("release");
        mav.addObject("release", release);
        return mav;
    }
}
