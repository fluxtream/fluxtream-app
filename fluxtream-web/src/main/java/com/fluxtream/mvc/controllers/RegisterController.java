package com.fluxtream.mvc.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.services.GuestService;

@Controller
public class RegisterController {
	
	Logger logger = Logger.getLogger(RegisterController.class);

	@Qualifier("authenticationManager")
	AuthenticationManager authenticationManager;
	
	@Autowired
	GuestService guestService;
	

	@RequestMapping("/createAccountForm")
	public String createAccountForm() {
		return "createAccount";
	}
	
	@RequestMapping("/createAccount")
	public ModelAndView createAccount(
		@RequestParam("email") String email,
		@RequestParam("username") String username,
		@RequestParam("firstname") String firstname,
		@RequestParam("lastname") String lastname,
		@RequestParam("password") String password,
		@RequestParam("password2") String password2,
//		@RequestParam("recaptchaChallenge") String challenge,
//		@RequestParam("recaptchaResponse") String uresponse,
		HttpServletRequest request,
		HttpServletResponse response) throws Exception
	{
		email = email.trim();
		password = password.trim();
		password2 = password2.trim();
		username = username.trim();
		firstname = firstname.trim();
		lastname = lastname.trim();
		
		List<String> required = new ArrayList<String>();
		List<String> errors = new ArrayList<String>();
		if (email=="") required.add("email");
		if (username=="") {
			required.add("username");
		} else if (guestService.getGuest(username)!=null) {
			errors.add("usernameTaken");
		}
		if (password=="") required.add("password");
		if (password2=="") required.add("password2");
		if (password.length()<8)
			errors.add("passwordTooShort");
		if (!password.equals(password2))
			errors.add("passwordsDontMatch");
		if (guestService.getGuestByEmail(email)!=null)
			errors.add("userExists");
					
//		String remoteAddr = request.getRemoteAddr();
//        ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
//        reCaptcha.setPrivateKey("6LeXl8QSAAAAADjPASFlMINNRVwtlpcvGugcr2RI");
//
//        ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(remoteAddr, challenge, uresponse);
//
//		if (!reCaptchaResponse.isValid())
//		errors.add("wrongCaptcha");
		
		if (errors.size()==0&&required.size()==0) {
			logger.info("action=register success=true username="+username + " email=" + email);
			guestService.createGuest (username, firstname, lastname, password, email);
			request.setAttribute("username", username);
			request.setAttribute("password", password);
			return new ModelAndView("accountCreationComplete");
		} else {
			logger.info("action=register errors=true");
			ModelAndView mav = new ModelAndView("createAccount");
			mav.addObject("email", email);
			mav.addObject("username", username);
			mav.addObject("firstname", firstname);
			mav.addObject("lastname", lastname);
			mav.addObject("errors", errors);
			mav.addObject("required", required);
			return mav;
		}
	}
	
}
