package org.fluxtream.mvc.controllers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.fluxtream.Configuration;
import org.fluxtream.aspects.FlxLogger;
import org.fluxtream.services.ApiDataService;
import org.fluxtream.services.CoachingService;
import org.fluxtream.services.ConnectorUpdateService;
import org.fluxtream.services.GuestService;
import org.fluxtream.services.MetadataService;
import org.fluxtream.services.NotificationsService;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * User: candide
 * Date: 11/04/14
 * Time: 14:11
 */
@Controller
public class DevController {

    FlxLogger logger = FlxLogger.getLogger(DevController.class);

    @Autowired
    Configuration env;

    @Autowired
    GuestService guestService;

    @Autowired
    ApiDataService apiDataService;

    @Autowired
    MetadataService metadataService;

    @Autowired
    NotificationsService notificationsService;

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Autowired
    CoachingService coachingService;

    @Autowired
    BeanFactory beanFactory;

    @Autowired
    ErrorController errorController;

    @RequestMapping(value = "/dev")
    public ModelAndView devIndex(HttpServletRequest request,
                                 HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0);
        Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();
        if (auth != null && auth.isAuthenticated())
            return new ModelAndView("redirect:/dev/home");
        ModelAndView mav = new ModelAndView("developer/developerIndex");
        String release = env.get("release");
        mav.addObject("release", release);
        final String facebookAppId = env.get("facebook.appId");
        if (facebookAppId !=null&&!facebookAppId.equals("xxx")) {
            mav.addObject("facebookAppId", facebookAppId);
            mav.addObject("supportsFBLogin", true);
        } else
            mav.addObject("supportsFBLogin", false);
        return mav;
    }

    @RequestMapping(value = "/dev/home")
    public ModelAndView devHome(HttpServletRequest request,
                                 HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0);
        ModelAndView mav = new ModelAndView("developer/developerHome");
        String release = env.get("release");
        mav.addObject("release", release);
        return mav;
    }

}
