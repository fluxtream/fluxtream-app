package org.fluxtream.mvc.controllers;

import net.sf.json.JSONObject;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.utils.HttpUtils;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

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

    @RequestMapping(value = {"/", "", "welcome"})
    public ModelAndView devIndex(HttpServletResponse response) {
        noCache(response);
        Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();
        if (auth != null && auth.isAuthenticated())
            return new ModelAndView("redirect:/home");
        final ModelAndView mav = new ModelAndView("index", "release", env.get("release"));
        mav.addObject("visibility", "public");
        return mav;
    }

    @RequestMapping(value = "/home")
    public ModelAndView partnersHome(HttpServletResponse response) {
        noCache(response);
        final ModelAndView mav = new ModelAndView("index", "release", env.get("release"));
        mav.addObject("visibility", "partners");
        return mav;
    }

    private void noCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0);
    }

    @RequestMapping(value = "/swapToken")
    public void obtainAccessToken(@RequestParam("code") String code,
                                  HttpServletResponse response) throws IOException, UnexpectedHttpResponseCodeException {
        Map<String,String> parameters = new HashMap<String,String>();
        parameters.put("grant_type", "authorization_code");
        parameters.put("code", code);
        parameters.put("client_id", env.get("fluxtreamDev.client.id"));
        parameters.put("client_secret", env.get("fluxtreamDev.client.secret"));
        parameters.put("redirect_uri", "somedummyfield");
        final String json = HttpUtils.fetch(env.get("homeBaseUrl") + "auth/oauth2/token", parameters);
        final JSONObject token = JSONObject.fromObject(json);
        final String accessToken = token.getString("access_token");
        response.sendRedirect(env.get("homeBaseUrl")+"swagger-ui/index.html?accessToken=" + accessToken);
    }

    @RequestMapping(value = "/partials/{partial}")
    public ModelAndView partial(@PathVariable("partial") String partial) {
        ModelAndView mav = new ModelAndView("/public/partials/" + partial);
        String release = env.get("release");
        mav.addObject("release", release);
        return mav;
    }

    @RequestMapping(value = "/partners/partials/{partial}")
    public ModelAndView partnersPartial(@PathVariable("partial") String partial) {
        ModelAndView mav = new ModelAndView("/partners/partials/" + partial);
        String release = env.get("release");
        mav.addObject("release", release);
        return mav;
    }

    @RequestMapping(value = "/checkIn")
    public ModelAndView checkIn(HttpServletRequest request,
                                HttpServletResponse response) throws IOException, NoSuchAlgorithmException, URISyntaxException {
        if (AuthHelper.getGuest()==null)
            return new ModelAndView("redirect:/");
        return new ModelAndView("redirect:/home");
    }

}
