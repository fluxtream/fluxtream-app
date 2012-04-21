package com.fluxtream.connectors.openpath;

import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.connectors.Connector;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;

@Controller()
@RequestMapping("/openPath")
public class OpenPathConnectorController {

	@Autowired
	GuestService guestService;

	@Autowired
	ConnectorUpdateService connectorUpdateService;

	@RequestMapping(value = "/enterCredentials")
	public ModelAndView signin(HttpServletRequest request) {
		ModelAndView mav = new ModelAndView(
				"connectors/openpath/enterCredentials");
		return mav;
	}

	@RequestMapping("/check")
	public ModelAndView check(@RequestParam("accessKey") String accessKey,
			@RequestParam("secretKey") String secretKey,
			HttpServletRequest request) throws MessagingException {
		List<String> required = new ArrayList<String>();
		accessKey = accessKey.trim();
		secretKey = secretKey.trim();
		request.setAttribute("accessKey", accessKey);
		request.setAttribute("secretKey", secretKey);
		if (accessKey.equals(""))
			required.add("accessKey");
		if (secretKey.equals(""))
			required.add("secretKey");
		if (required.size() != 0) {
			request.setAttribute("required", required);
			return new ModelAndView("connectors/openpath/enterCredentials");
		}
		long guestId = ControllerHelper.getGuestId();
		boolean worked = false;
		try {
			worked = (new OpenPathHelper(accessKey, secretKey)).testConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (worked) {
			guestService.setApiKeyAttribute(guestId,
					Connector.getConnector("openpath"), "accessKey", accessKey);
			guestService.setApiKeyAttribute(guestId,
					Connector.getConnector("openpath"), "secretKey", secretKey);
			ModelAndView mav = new ModelAndView("connectors/openpath/success");
			return mav;
		} else {
			request.setAttribute("errorMessage",
					"Sorry, you must have entered wrong credentials.\nPlease try again.");
			return new ModelAndView("connectors/openpath/enterCredentials");
		}
	}

}
