package com.fluxtream.mvc.controllers;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.ConnectorInfo;
import com.fluxtream.mvc.models.ConnectorModel;
import com.fluxtream.mvc.views.ViewsHelper;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SystemService;

@Controller
@Deprecated
public class ConnectorsController {

	@Autowired
	GuestService guestService;

	@Autowired
	SystemService systemService;

	@Autowired
	Configuration env;

	@RequestMapping("/connectors/init")
	public ModelAndView getRecommendedInitialConnectors(
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView mav = new ModelAndView("connectors/init");
		long guestId = ControllerHelper.getGuestId();
		List<ApiKey> userConnectors = guestService.getApiKeys(guestId);
		mav.addObject("hasLatitude",
				hasConnector(userConnectors, "google_latitude"));
		mav.addObject("userConnectors", userConnectors);
		mav.addObject("release", env.get("release"));
		return mav;
	}
	
	@RequestMapping("/connectors/welcome2")
	public ModelAndView getWelcome2() {
		return new ModelAndView("/connectors/welcome2");
	}
	
	@RequestMapping("/connectors/welcome3")
	public ModelAndView getWelcome3() {
		return new ModelAndView("/connectors/welcome3");
	}

	@RequestMapping("/connectors/main")
	public ModelAndView getSettings(HttpServletRequest request,
			HttpServletResponse response) {
		ModelAndView mav = new ModelAndView("connectors/addOrRemove");
		long guestId = ControllerHelper.getGuestId();
		List<ApiKey> userApis = guestService.getApiKeys(guestId);

//		if (userApis.size() == 0 && request.getParameter("all") == null) {
//			mav.addObject("release", env.get("release"));
//			mav.setViewName("connectors/welcome");
//			return mav;
//		}
		
		mav.addObject(
				"userConnectorRows",
				new ViewsHelper<ConnectorModel>().rows(
						ViewsHelper.toConnectorModels(userApis), 3));

		List<ConnectorInfo> pagedApis = getPagedAvailableApis(guestId,
				userApis, request, mav);

		mav.addObject("availableConnectorRows",
				new ViewsHelper<ConnectorInfo>().rows(pagedApis, 3));
		mav.addObject("userApis", userApis);
		mav.addObject("guestId", guestId);
		mav.addObject("env", env);
		return mav;
	}

	private Boolean hasConnector(List<ApiKey> userConnectors,
			String connectorName) {
		for (ApiKey apiKey : userConnectors) {
			if (apiKey.getConnector().getName().toLowerCase()
					.equals(connectorName.toLowerCase()))
				return true;
		}
		return false;
	}

	private List<ConnectorInfo> getPagedAvailableApis(long guestId,
			List<ApiKey> userApis, HttpServletRequest request, ModelAndView mav) {
		List<ConnectorInfo> availableApis = subtractApis(userApis, guestId);
		int pageSize = 6;
		int page = getPage(request);
		int pages = getPages(availableApis, pageSize);
		int itemsAtPage = getItemsAtPage(availableApis, page, pageSize);
		int start = (page * pageSize);
		int end = start + itemsAtPage - 1;
		mav.addObject("currentPage", page);
		mav.addObject("pages", pages);
		mav.addObject("showing", "Showing connectors " + (start + 1) + " to "
				+ (end + 1) + " of " + availableApis.size());
		mav.addObject("env", env);
		List<ConnectorInfo> pagedApis = availableApis.subList(start, end + 1);
		return pagedApis;
	}

	int getItemsAtPage(List<ConnectorInfo> apis, int page, int pageSize) {
		int n = apis.size() / pageSize;
		int m = apis.size() % pageSize;
		if (page <= (n - 1))
			return pageSize;
		return m;
	}

	int getPage(HttpServletRequest request) {
		if (request.getParameter("page") != null)
			return Integer.valueOf(request.getParameter("page"));
		return 0;
	}

	int getPages(List<ConnectorInfo> apis, int pageSize) {
		int n = apis.size() / pageSize;
		int m = apis.size() % pageSize;
		return (m > 0) ? n + 1 : n;
	}

	@RequestMapping("/connectors/connectorDescription")
	public ModelAndView connectorDescription(@RequestParam("api") String api,
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView mav = new ModelAndView("connectors/connectorDescription");
		mav.addObject("api", api.toLowerCase());
		return mav;
	}

	@RequestMapping("/connectors/connectorSettings")
	public ModelAndView connectorSettings(@RequestParam("api") String api,
			HttpServletRequest request, HttpServletResponse response) {
		ModelAndView mav = new ModelAndView("connectors/connectorSettings");
		mav.addObject("api", api.toLowerCase());
		return mav;
	}

	@RequestMapping("/connectors/userConnectors")
	public ModelAndView userConnectors(HttpServletRequest request,
			HttpServletResponse response) {
		ModelAndView mav = new ModelAndView("connectors/userConnectors");
		long guestId = ControllerHelper.getGuestId();
		List<ApiKey> userApis = guestService.getApiKeys(guestId);
		mav.addObject(
				"userConnectorRows",
				new ViewsHelper<ConnectorModel>().rows(
						ViewsHelper.toConnectorModels(userApis), 3));
		return mav;
	}

	@RequestMapping("/connectors/availableConnectors")
	public ModelAndView availableConnectors(HttpServletRequest request,
			HttpServletResponse response) {
		ModelAndView mav = new ModelAndView("connectors/availableConnectors");
		long guestId = ControllerHelper.getGuestId();
		List<ApiKey> userApis = guestService.getApiKeys(guestId);

		List<ConnectorInfo> pagedApis = getPagedAvailableApis(guestId,
				userApis, request, mav);

		mav.addObject("availableConnectorRows",
				new ViewsHelper<ConnectorInfo>().rows(pagedApis, 3));
		return mav;
	}

	List<ConnectorInfo> subtractApis(List<ApiKey> userKeys, long guestId) {
		List<ConnectorInfo> remainingConnectors = systemService.getConnectors();
		List<ConnectorInfo> allConnectors = systemService.getConnectors();
		for (ConnectorInfo connector : allConnectors) {
			connector.connectUrl += "?guestId=" + guestId;
			for (ApiKey key : userKeys) {
				if (key.getConnector() == connector.getApi())
					remainingConnectors.remove(connector);
			}
			if (!connector.enabled)
				remainingConnectors.remove(connector);
		}
		return remainingConnectors;
	}

	@RequestMapping("/connectors/removeConnector")
	public void removeConnector(@RequestParam("api") String api,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		long guestId = ControllerHelper.getGuestId();
		JSONObject result = new JSONObject();
		try {
			Connector apiToRemove = Connector.fromString(api);
			guestService.removeApiKey(guestId, apiToRemove);
			result.put("result", "ok");
			response.getWriter().write(result.toString());
		} catch (Throwable t) {
			result.put("result", "ko");
			response.getWriter().write(result.toString());
		}
	}

}
