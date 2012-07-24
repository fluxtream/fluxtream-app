package com.fluxtream.connectors.zeo;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.GuestService;
import com.fluxtream.utils.HttpUtils;

@Controller
@RequestMapping(value = "/zeo")
public class ZeoRestController {

	Logger logger = Logger.getLogger(ZeoRestController.class);

	@Autowired
	GuestService guestService;

	@Autowired
	Configuration env;

    @Qualifier("apiDataServiceImpl")
    @Autowired
	ApiDataService apiDataService;

	@RequestMapping(value = "/subscribe")
	public void userSubscribe(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		long guestId = ControllerHelper.getGuestId();
		String callbackUrl = env.get("homeBaseUrl") + "zeo/" + guestId + "/notify";
		String redirUrl = env.get("homeBaseUrl") + "zeo/" + guestId + "/userSubscribed";
		String zeoApiKey = env.get("zeoApiKey");

		// first unsubscribe (just to be sure)
		String unsubscribeUrl = "http://api.myzeo.com:8080/zeows/"
				+ "api/v1/json/sleeperService/unsubscribeFromNotify?key="
				+ zeoApiKey + "&userid=" + guestId;

		String unsubscribeResult = HttpUtils.fetch(unsubscribeUrl, env,
				env.get("zeoDeveloperUsername"),
				env.get("zeoDeveloperPassword"));
		logger.info("guestId=" + guestId +
				" action=unsubscribeNotifications connector=zeo result="
						+ unsubscribeResult);
		System.out.println("just unsubscribed: " + unsubscribeResult);

		String url = "http://api.myzeo.com:8080/zeows/"
				+ "api/v1/json/sleeperService/subscribeToNotify?key="
				+ zeoApiKey + "&userid=" + guestId + "&callback=" + callbackUrl
				+ "&redir=" + redirUrl;

		response.sendRedirect(url);
	}
	
	@RequestMapping(value = "/{guestId}/userSubscribed")
	public String userSubscribed(@PathVariable final Long guestId,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		System.out.println("zeo user is subscribed");

		guestService.setApiKeyAttribute(guestId, Connector.getConnector("zeo"),
				"subscribed-since", String.valueOf(System.currentTimeMillis()));

		return "redirect:/app/from/zeo";
	}

}
