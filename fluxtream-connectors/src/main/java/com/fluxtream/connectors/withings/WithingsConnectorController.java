package com.fluxtream.connectors.withings;

import static com.fluxtream.utils.HttpUtils.fetch;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.updaters.UpdateInfo.UpdateType;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.google.gson.Gson;

@Controller()
@RequestMapping("/withings")
public class WithingsConnectorController {
	
	private static final String WBSAPI_GETUSERSLIST = "http://wbsapi.withings.net/account?action=getuserslist&email=";
	private static final String WBSAPI_ONCE_ACTION_GET = "http://wbsapi.withings.net/once?action=get";

	@Autowired
	Configuration env;

	@Autowired
	GuestService guestService;
	
	@Autowired
	ConnectorUpdateService connectorUpdateService;
	
	@RequestMapping(value = "/chooseWithingsUser")
	public ModelAndView chooseWithingsUser( HttpServletRequest request,
			HttpServletResponse response) throws UnsupportedEncodingException {
		String publickey = request.getParameter("chosenUser");
		List<UsersListResponseUser> withingsUsers = (List<UsersListResponseUser>) request.getSession()
				.getAttribute("scaleUsers");
		long userid = getUserIdWithPublicKey(
				withingsUsers, publickey);
		long guestId = ControllerHelper.getGuestId();
		Guest guest = guestService.getGuestById(Long.valueOf(guestId));
		if (guest == null) {
			ModelAndView mav = new ModelAndView("general-error");
			mav.addObject("errorMessage",
					"There is no user with specified id: " + guestId);
			return mav;
		} else {
			connectorUpdateService.scheduleUpdate(guestId, "withings", 3,
					UpdateType.INITIAL_HISTORY_UPDATE,
					System.currentTimeMillis());
			
			guestService.setApiKeyAttribute(guestId, Connector.getConnector("WITHINGS"),
					"publickey", publickey);
			guestService.setApiKeyAttribute(guestId, Connector.getConnector("WITHINGS"),
					"userid", String.valueOf(userid));
		}
		ModelAndView mav = new ModelAndView("connectors/withings/success");
		mav.addObject("guestId", guestId);
		return mav;
	}

	@RequestMapping(value = "/notify")
	public String notifyMeasurement(HttpServletRequest request, HttpServletResponse response) {
		return null;
	}

	@RequestMapping(value = "/enterCredentials")
	public ModelAndView signin(
			HttpServletRequest request) {
		ModelAndView mav = new ModelAndView("connectors/withings/enterCredentials");
		return mav;
	}

	private long getUserIdWithPublicKey(List<UsersListResponseUser> attribute,
			String publickey) {
		for (UsersListResponseUser user : attribute) {
			if (user.getPublickey().equals(publickey))
				return user.getId();
		}
		return -1;
	}

	@RequestMapping(value="/setupWithings")
	public String setupWithings(HttpServletRequest request, HttpServletResponse response)
		throws NoSuchAlgorithmException, HttpException, IOException
	{
		String email = request.getParameter("username");
		String password = request.getParameter("password");
		email = email.trim();
		password = password.trim();
		request.setAttribute("username", email);
		List<String> required = new ArrayList<String>();
		if (email.equals(""))
			required.add("username");
		if (password.equals(""))
			required.add("password");
		if (required.size()!=0) {
			request.setAttribute("required", required);
			return "connectors/withings/enterCredentials";
		}
		try {
			List<UsersListResponseUser> scaleUsers = getScaleUsers(email, password);
			request.getSession(true).setAttribute("scaleUsers", scaleUsers);
			request.setAttribute("scaleUsers", scaleUsers);
		}
		catch (RuntimeException re) {
			int code = Integer.valueOf(re.getMessage());
			switch (code) {
			case 264:
				request.setAttribute("errorMessage", "The email address provided is either unknown or invalid");
				break;
			case 2555:
				request.setAttribute("errorMessage", "An unknown error occurred");
				break;
			case 100:
				request.setAttribute("errorMessage", "The hash is missing, invalid, or does not match the provided email");
				break;
			}
			request.setAttribute("username", email);
			return "connectors/withings/enterCredentials";
		}
		return "connectors/withings/chooseUser";
	}

	public List<UsersListResponseUser> getScaleUsers(String email,
			String password) throws NoSuchAlgorithmException, HttpException,
			IOException {
		String passwordHash = hash(password);
		String onceJson = fetch(WBSAPI_ONCE_ACTION_GET, env);
		WithingsOnceResponse once = new Gson().fromJson(onceJson,
				WithingsOnceResponse.class);
		String noonce = once.getBody().getOnce();
		String code = email + ":" + passwordHash + ":" + noonce;
		String hash = hash(code);
		String json = fetch(WBSAPI_GETUSERSLIST + email + "&hash=" + hash, env);
		System.out.println(json);
		UsersListResponse response = new Gson().fromJson(json,
				UsersListResponse.class);
		if (response.status!=0) throw new RuntimeException(String.valueOf(response.status));
		return response.getBody().getUsers();
	}

	public String hash(String toHash) throws NoSuchAlgorithmException {
		byte[] uniqueKey = toHash.getBytes();
		byte[] hash = null;
		hash = MessageDigest.getInstance("MD5").digest(uniqueKey);
		StringBuilder hashString = new StringBuilder();
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(hash[i]);
			if (hex.length() == 1) {
				hashString.append('0');
				hashString.append(hex.charAt(hex.length() - 1));
			} else
				hashString.append(hex.substring(hex.length() - 2));
		}
		return hashString.toString();
	}

}
