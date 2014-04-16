package org.fluxtream.mvc.controllers;

import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.services.GuestService;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

/**
 * User: candide
 * Date: 11/04/14
 * Time: 14:11
 */
@Controller
public class DevController {

//    FlxLogger logger = FlxLogger.getLogger(DevController.class);

    @Autowired
    Configuration env;

    @Autowired
    BeanFactory beanFactory;

    @Autowired
    GuestService guestService;

    @RequestMapping(value = {"/", ""})
    public ModelAndView devIndex(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0);
        Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();
        if (auth != null && auth.isAuthenticated())
            return new ModelAndView("redirect:/home");
        ModelAndView mav = new ModelAndView("index");
        String release = env.get("release");
        mav.addObject("release", release);
        return mav;
    }

    @RequestMapping(value = "/partials/{partial}")
    public ModelAndView partial(@PathVariable("partial") String partial) {
        ModelAndView mav = new ModelAndView("/partials/" + partial);
        String release = env.get("release");
        mav.addObject("release", release);
        return mav;
    }

    @RequestMapping(value = "/checkIn")
    public ModelAndView checkIn(HttpServletRequest request,
                                HttpServletResponse response) throws IOException, NoSuchAlgorithmException, URISyntaxException {
        if (AuthHelper.getGuest()==null)
            return new ModelAndView("redirect:/");
        long guestId = AuthHelper.getGuestId();
        if (AuthHelper.getGuest().hasRole("ROLE_DEVELOPER"))
            return new ModelAndView("redirect:/home");
        checkIn(request, guestId);
        final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        SavedRequest savedRequest =
                requestCache.getRequest(request, response);
        if (savedRequest!=null) {
            final String redirectUrl = savedRequest.getRedirectUrl();
            requestCache.removeRequest(request, response);
            final URI uri = new URI(redirectUrl);
            return new ModelAndView("redirect:" + uri.getPath());
        }
        return new ModelAndView("redirect:/app");
    }

    private void checkIn(HttpServletRequest request, long guestId)
            throws IOException {
        String remoteAddr = request.getHeader("X-Forwarded-For");
        if (remoteAddr == null)
            remoteAddr = request.getRemoteAddr();
        guestService.checkIn(guestId, remoteAddr);
    }

    @RequestMapping(value = "/home")
    public ModelAndView devHome(HttpServletRequest request,
                                 HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0);
        ModelAndView mav = new ModelAndView("home");
        String release = env.get("release");
        mav.addObject("release", release);
        return mav;
    }

}
