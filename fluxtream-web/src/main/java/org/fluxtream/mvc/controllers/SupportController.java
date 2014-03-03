package org.fluxtream.mvc.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpException;
import org.fluxtream.aspects.FlxLogger;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import org.fluxtream.Configuration;
import org.fluxtream.domain.Guest;
import org.fluxtream.domain.ResetPasswordToken;
import org.fluxtream.services.GuestService;
import com.postmark.PostmarkMailSender;

@Controller
@RequestMapping("/support")
public class SupportController {

	static FlxLogger logger = FlxLogger.getLogger(SupportController.class);

	@Qualifier("authenticationManager")
	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	Configuration env;

	@Autowired
	GuestService guestService;

	@Autowired
	VelocityEngine velocityEngine;

	@RequestMapping(value = "/lostPassword")
	public ModelAndView lostPassword(HttpServletRequest request) {
		ModelAndView mav = new ModelAndView("support/lostPassword");
		mav.addObject("release", env.get("release"));
		return mav;
	}

	@RequestMapping(value = "/sendResetRequest")
	public ModelAndView sendResetRequest(HttpServletRequest request) throws HttpException,IOException {
		// create a hash for this request
        String email = request.getParameter("recover[email]");
		Guest guest = guestService.getGuestByEmail(email);
		if (guest == null) {
			ModelAndView mav = new ModelAndView("support/lostPassword");
			mav.addObject("error",
					"Sorry, we could not find a user with your email address. Please try again.");
			mav.addObject("release", env.get("release"));
			return mav;
		}
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(email);

        // Retrieve postmark properties.  Keys postmarkApiKey and postmarkSendAddress should be set
        // in common.properties
        String postmarkSendAddress = env.get("postmarkSendAddress");
        String postmarkApiKey = env.get("postmarkApiKey");

        // Process postmarkSendAddress
        if(postmarkSendAddress==null) {
            postmarkSendAddress = "support@fluxtream.com";
            logger.warn("component=support_controller action=sendResetRequest" +
                                    " guestId=" + guest.getId() +
                                    " message=\"**** PLEASE SET postmarkSendAddress IN common.properties; defaulting to support@fluxtream.com\"");
        }
		message.setFrom(postmarkSendAddress);

        // Process postmarkApiKey
        if(postmarkApiKey==null) {
            logger.error("component=support_controller action=sendResetRequest" +
                                    " guestId=" + guest.getId() +
                                    " message=\"**** PLEASE SET postmarkApiKey IN common.properties.  Cannot send reset email without it.\"");
            ModelAndView mav = new ModelAndView("support/serverConfigError");
          	mav.addObject("release", env.get("release"));
            mav.addObject("userMessage", "We are not able to send email for resetting your password");
            mav.addObject("adminMessage", "Please set up the following keys in common.properties to enable email sending: postmarkApiKey and postmarkSendAddress");
          	return mav;
        }
        PostmarkMailSender sender = new PostmarkMailSender(
      				env.get("postmarkApiKey"));



		message.setSubject("Fluxtream Reset password request");
		Map<String, String> vars = new HashMap<String, String>();
		ResetPasswordToken pToken = guestService.createToken(guest.getId());
		vars.put("token", pToken.token);
		vars.put("homeBaseUrl", env.get("homeBaseUrl"));
		if (guest.firstname != null && !guest.firstname.equals(""))
			vars.put("username", guest.firstname);
		else
			vars.put("username", guest.username);
        String mailMessage ="Hi " + vars.get("username") + ",\n" +
                            "\n" +
                            "Someone requested that your Fluxtream.com password be reset.\n" +
                            "\n" +
                            "If this wasn't you, there's nothing to worry about - simply ignore this email and nothing will change.\n" +
                            "\n" +
                            "If you DID ask to reset the password on your Fluxtream account, just click here to make it happen:\n" +
                            "\n" +
                            vars.get("homeBaseUrl") + "support/resetPassword?token=" + vars.get("token") + "\n" +
                            "\n" +
                            "Thanks,\n" +
                            "\n" +
                            "The Fluxtream Team";

		message.setText(mailMessage);
		sender.send(message);
		ModelAndView mav = new ModelAndView("support/resetRequestSent");
		mav.addObject("release", env.get("release"));
		return mav;
	}

	@RequestMapping(value = "/resetPassword")
	public ModelAndView resetPassword(HttpServletRequest request,
			@RequestParam("token") String token) throws Exception {
		List<String> errors = new ArrayList<String>();

		ResetPasswordToken pToken = guestService.getToken(token);
		Guest guest = null;
		if (pToken == null)
			errors.add("invalidToken");
		else {
			guest = guestService.getGuestById(pToken.guestId);
		}
		ModelAndView mav = new ModelAndView("support/resetPassword");
		if (pToken != null && guest != null) {
			if (guest.firstname != null
					&& !guest.firstname.equals(""))
				request.setAttribute("username", guest.firstname);
			else
				request.setAttribute("username", guest.username);
			request.setAttribute("token", pToken.token);
		} else
			request.setAttribute("errors", errors);
		mav.addObject("release", env.get("release"));
		return mav;
	}

	@RequestMapping(value = "/doResetPassword")
	public ModelAndView doResetPassword(HttpServletRequest request,
			@RequestParam("token") String token,
			@RequestParam("password") String password,
			@RequestParam("password2") String password2) throws Exception {
		// create a hash for this request
		List<String> required = new ArrayList<String>();
		List<String> errors = new ArrayList<String>();

		if (password == "")
			required.add("password");
		if (password2 == "")
			required.add("password2");
		if (password.length() < 8)
			errors.add("passwordTooShort");
		if (!password.equals(password2))
			errors.add("passwordsDontMatch");

		ResetPasswordToken pToken = guestService.getToken(token);
		long now = System.currentTimeMillis();
		int resetPasswordTokensExpireDelay = Integer.valueOf(env
				.get("resetPasswordTokensExpireDelay"));

		Guest guest = null;
		if (pToken == null)
			errors.add("invalidToken");
		else if ((now - pToken.ts) > resetPasswordTokensExpireDelay) {
			errors.add("tokenExpired");
			guestService.deleteToken(token);
		} else {
			guest = guestService.getGuestById(pToken.guestId);
		}

		if (errors.size() > 0 || required.size() > 0) {
			ModelAndView mav = new ModelAndView("support/resetPassword");

			if (guest != null) {
				if (guest.firstname != null
						&& !guest.firstname.equals(""))
					request.setAttribute("username", guest.firstname);
				else
					request.setAttribute("username", guest.username);
			} else
				request.setAttribute("username", "Stranger");

			mav.addObject("errors", errors);
			mav.addObject("required", required);
			mav.addObject("token", pToken == null ? "null" : pToken.token);
			mav.addObject("release", env.get("release"));
			return mav;
		} else {
			guestService.setPassword(guest.getId(), password);
			guestService.deleteToken(pToken.token);
			UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
					guest.username, password);
			Authentication authentication = authenticationManager
					.authenticate(authToken);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			ModelAndView mav = new ModelAndView("support/passwordReset");
			mav.addObject("token", pToken.token);
			mav.addObject("release", env.get("release"));
			return mav;
		}
	}
}
