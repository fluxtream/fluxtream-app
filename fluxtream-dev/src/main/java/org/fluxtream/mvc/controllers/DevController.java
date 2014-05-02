package org.fluxtream.mvc.controllers;

import net.sf.json.JSONObject;
import org.codehaus.jackson.map.ObjectMapper;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.impl.ExistingEmailException;
import org.fluxtream.core.services.impl.UsernameAlreadyTakenException;
import org.fluxtream.core.utils.HttpUtils;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.markdown4j.Markdown4jProcessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
        response.sendRedirect(env.get("homeBaseUrl") + "swagger-ui/index.html?accessToken=" + accessToken);
    }

    @RequestMapping(value = "/partials/{partial}")
    public ModelAndView partial(@PathVariable("partial") String partial) {
        ModelAndView mav = new ModelAndView("/public/partials/" + partial);
        String release = env.get("release");
        mav.addObject("release", release);
        return mav;
    }

    @RequestMapping(value = "/partials/user-manual")
    public ModelAndView userManual() throws IOException {
        ModelAndView mav = new ModelAndView("/public/partials/user-manual");
        String release = env.get("release");
        final String userManualLocation = env.get("userManual.location");
        Markdown4jProcessor processor = new Markdown4jProcessor();
        String manual = processor.process(new File(userManualLocation));
        mav.addObject("manual", manual);
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

    @RequestMapping("/createAccountForm")
    public ModelAndView createAccountForm(
            @RequestParam(value="isDeveloperAccount",required=false, defaultValue = "false") boolean isDeveloperAccount
    ) {
        ModelAndView mav = new ModelAndView("public/partials/sign-up");
        mav.addObject("isDeveloperAccount", isDeveloperAccount);
        return mav;
    }

    @RequestMapping(value="/createAccount", method= RequestMethod.POST)
    public void createAccount(
            @RequestParam(value="email", required=false, defaultValue="") String email,
            @RequestParam(value="username", required=false, defaultValue="") String username,
            @RequestParam(value="firstname", required=false, defaultValue="") String firstname,
            @RequestParam(value="lastname", required=false, defaultValue="") String lastname,
            @RequestParam(value="password", required=false, defaultValue="") String password,
            @RequestParam(value="password2", required=false, defaultValue="") String password2,
//		@RequestParam("recaptchaChallenge") String challenge,
//		@RequestParam("recaptchaResponse") String uresponse,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception, UsernameAlreadyTakenException, ExistingEmailException {
        email = email.trim();
        password = password.trim();
        password2 = password2.trim();
        username = username.trim();
        firstname = firstname.trim();
        lastname = lastname.trim();

        Map<String,String> fields = new HashMap<String,String>();
        for (String fieldName : new String[]{"username", "email", "firstname", "lastname", "password", "password2"})
            fields.put(fieldName, "valid");

        if (email.equals("")) fields.put("email", "E-mail is required");
        if (firstname.equals("")) fields.put("firstname", "First name is required");
        if (username.equals("")) {
            fields.put("username", "Username is required");
        } else if (guestService.getGuest(username)!=null) {
            fields.put("username", "This username is already taken");
        }
        if (password.equals("")) fields.put("password", "Password is required");
        if (password2.equals("")) fields.put("password2", "Please repeat the password");

        if (!fields.containsKey("password")&&password.length()<8)
            fields.put("password", "Password is too short (min 8 characters)");
        if (!password.equals(password2))
            fields.put("password2", "Passwords don't match");
        final Guest existingGuestWithTheSameEmail = guestService.getGuestByEmail(email);
        if (existingGuestWithTheSameEmail !=null && existingGuestWithTheSameEmail.getUserRoles().contains("ROLE_DEVELOPER"))
            fields.put("email", "We have already signed you up. Sign in?");

//		String remoteAddr = request.getRemoteAddr();
//        ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
//        reCaptcha.setPrivateKey("6LeXl8QSAAAAADjPASFlMINNRVwtlpcvGugcr2RI");
//
//        ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(remoteAddr, challenge, uresponse);
//
//		if (!reCaptchaResponse.isValid())
//		errors.add("wrongCaptcha");

        ObjectMapper objectMapper = new ObjectMapper();
        response.setContentType("application/json");
        if (allValid(fields)) {
            logger.info("action=register success=true username="+username + " email=" + email);
            guestService.createGuest(username, firstname,
                    lastname, password, email, Guest.RegistrationMethod.REGISTRATION_METHOD_FORM, true);
            response.getWriter().write("{\"status\":\"valid\"}");
        } else {
            Map<String, Object> jsonResponse = new HashMap<String,Object>();
            jsonResponse.put("fields", fields);
            jsonResponse.put("status", "invalid");
            response.getWriter().write(objectMapper.writeValueAsString(jsonResponse));
        }
    }

    private boolean allValid(Map<String, String> fields) {
        for (String s : fields.values()) {
            if (!s.equals("valid"))
                return false;
        }
        return true;
    }

}
