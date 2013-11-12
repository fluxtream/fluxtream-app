package com.fluxtream.connectors.sms_backup;

import java.util.ArrayList;
import java.util.List;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller()
@RequestMapping("/smsBackup")
public class SmsBackupConnectorController {

    private static final String SMS_BACKUP_USERNAME = "smsBackup.username";
    private static final String SMS_BACKUP_PASSWORD = "smsBackup.password";
    @Autowired
	GuestService guestService;

	@Autowired
	ConnectorUpdateService connectorUpdateService;

	@RequestMapping(value = "/enterCredentials")
	public ModelAndView signin(HttpServletRequest request) {
		ModelAndView mav = new ModelAndView(
				"connectors/smsBackup/enterCredentials");
		return mav;
	}

	@RequestMapping("/check")
	public ModelAndView check(@RequestParam("username") String email,
			@RequestParam("password") String password,
			HttpServletRequest request) throws MessagingException {
		List<String> required = new ArrayList<String>();
		email = email.trim();
		password = password.trim();
		request.setAttribute("username", email);
		if (email.equals(""))
			required.add("username");
		if (password.equals(""))
			required.add("password");
		if (required.size() != 0) {
			request.setAttribute("required", required);
			return new ModelAndView("connectors/smsBackup/enterCredentials");
		}
		ModelAndView mav = new ModelAndView("connectors/smsBackup/setFolderNames");
		boolean worked = false;
		try {
			worked = (new SmsBackupHelper(email, password)).testConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (worked) {
            request.getSession().setAttribute(SMS_BACKUP_USERNAME, email);
            request.getSession().setAttribute(SMS_BACKUP_PASSWORD, password);

			return mav;
		} else {
			request.setAttribute("errorMessage",
					"Sorry, you must have entered wrong credentials.\nPlease try again.");
			return new ModelAndView("connectors/smsBackup/enterCredentials");
		}
	}

	@RequestMapping("/setFolderNames")
	public ModelAndView setFolderNames(
            @RequestParam("smsFolderName") String smsFolderName,
			@RequestParam("callLogFolderName") String callLogFolderName,
			HttpServletRequest request) throws MessagingException {

		List<String> required = new ArrayList<String>();
		smsFolderName = smsFolderName.trim();
		callLogFolderName = callLogFolderName.trim();
		request.setAttribute("smsFolderName", smsFolderName);
		request.setAttribute("callLogFolderName", callLogFolderName);
		if (smsFolderName.equals(""))
			required.add("smsFolderName");
		if (callLogFolderName.equals(""))
			required.add("callLogFolderName");
		if (required.size() != 0) {
			request.setAttribute("required", required);
			return new ModelAndView("connectors/smsBackup/setFolderName");
		}
		
		ModelAndView mav = new ModelAndView("connectors/smsBackup/success");
		long guestId = AuthHelper.getGuestId();

        final Connector connector = Connector.getConnector("sms_backup");
        final ApiKey apiKey = guestService.createApiKey(guestId, connector);

        guestService.setApiKeyAttribute(apiKey, "username", (String)request.getSession().getAttribute(SMS_BACKUP_USERNAME));
        guestService.setApiKeyAttribute(apiKey, "password", (String)request.getSession().getAttribute(SMS_BACKUP_PASSWORD));
        request.getSession().removeAttribute(SMS_BACKUP_USERNAME);
        request.getSession().removeAttribute(SMS_BACKUP_PASSWORD);
		guestService.setApiKeyAttribute(apiKey, "smsFolderName",
				smsFolderName);
		guestService.setApiKeyAttribute(apiKey, "callLogFolderName",
				callLogFolderName);
		connectorUpdateService.updateConnector(apiKey,false);
		return mav;
	}
}
