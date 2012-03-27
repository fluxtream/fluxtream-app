package com.fluxtream.mvc.admin.controllers;

import static com.fluxtream.utils.Utils.stackTrace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.connectors.updaters.UpdateInfo.UpdateType;
import com.fluxtream.connectors.updaters.UpdateResult;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.ScheduledUpdate;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;

@Controller
@RequestMapping(value = "/admin")
public class AdminController {

	@Autowired
	Configuration env;

	@Autowired
	ConnectorUpdateService connectorUpdateService;

	@Autowired
	ApiDataService apiDataService;

	@Autowired
	AdminHelper helper;

	@Autowired
	GuestService guestService;

	@RequestMapping(value = { "/general", "/index" })
	public ModelAndView general() {
		ModelAndView mav = new ModelAndView("admin/index");
		mav.addObject("release", env.get("release"));
		mav.addObject("childView", "general.jsp");
		mav.addObject("activeTab", "stats");
		mav.addObject("helper", helper);
		Guest guest = ControllerHelper.getGuest();
		mav.addObject("fullname", guest.getGuestName());
		return mav;
	}

	@RequestMapping(value = "/general/lucene")
	public ModelAndView generalLuceneIndex() {
		ModelAndView mav = new ModelAndView("admin/index");
		mav.addObject("release", env.get("release"));
		mav.addObject("childView", "general.jsp");
		mav.addObject("activeTab", "lucene");
		mav.addObject("helper", helper);
		return mav;
	}

	@RequestMapping(value = "/general/stats")
	public ModelAndView generalStats() {
		ModelAndView mav = new ModelAndView("admin/index");
		mav.addObject("release", env.get("release"));
		mav.addObject("childView", "general.jsp");
		mav.addObject("activeTab", "stats");
		mav.addObject("helper", helper);
		return mav;
	}

	@RequestMapping(value = "/general/tasks")
	public ModelAndView generalTasks() {
		ModelAndView mav = new ModelAndView("admin/index");
		mav.addObject("release", env.get("release"));
		mav.addObject("childView", "general.jsp");
		mav.addObject("activeTab", "tasks");
		mav.addObject("helper", helper);
		return mav;
	}

	@RequestMapping(value = "/general/connectors")
	public String connectorsRoot() {
		return "redirect:/admin/general/connectors/stats";
	}

	@RequestMapping(value = "/general/connectors/stats")
	public ModelAndView generalConnectors() {
		ModelAndView mav = new ModelAndView("admin/index");
		mav.addObject("release", env.get("release"));
		mav.addObject("childView", "general.jsp");
		mav.addObject("activeTab", "connectors");
		mav.addObject("helper", helper);
		mav.addObject("activePill", "stats");
		return mav;
	}

	@RequestMapping(value = "/general/connectors/settings/{connectorName}")
	public ModelAndView generalConnectorsLayout(HttpServletRequest request,
			@PathVariable String connectorName) {
		ModelAndView mav = new ModelAndView("admin/index");
		mav.addObject("release", env.get("release"));
		mav.addObject("childView", "general.jsp");
		mav.addObject("activeTab", "connectors");
		mav.addObject("helper", helper);
		mav.addObject("activePill", "stats");
		request.setAttribute("settings", connectorName);
		return mav;
	}

	@RequestMapping(value = "/general/connectors/layout")
	public ModelAndView generalConnectorsLayout() {
		ModelAndView mav = new ModelAndView("admin/index");
		mav.addObject("release", env.get("release"));
		mav.addObject("childView", "general.jsp");
		mav.addObject("activeTab", "connectors");
		mav.addObject("helper", helper);
		mav.addObject("activePill", "layout");
		return mav;
	}

	@RequestMapping(value = "/general/roles")
	public ModelAndView generalRoles() {
		ModelAndView mav = new ModelAndView("admin/index");
		mav.addObject("release", env.get("release"));
		mav.addObject("childView", "general.jsp");
		mav.addObject("activeTab", "roles");
		mav.addObject("helper", helper);
		return mav;
	}

	@RequestMapping(value = "/guests")
	public ModelAndView guests() {
		ModelAndView mav = new ModelAndView("admin/index");
		mav.addObject("release", env.get("release"));
		mav.addObject("childView", "guests.jsp");
		mav.addObject("helper", helper);
		return mav;
	}

	@RequestMapping(value = { "/guests/{guestId}", "/guests/{guestId}/general" })
	public ModelAndView guestGeneral(@PathVariable Long guestId) {
		ModelAndView mav = new ModelAndView("admin/index");
		mav.addObject("release", env.get("release"));
		mav.addObject("childView", "guests.jsp");
		mav.addObject("guestId", guestId);
		mav.addObject("helper", helper);
		mav.addObject("activeTab", "general");
		return mav;
	}

	@RequestMapping(value = "/guests/{guestId}/erase")
	public String eraseGuest(@PathVariable Long guestId) throws Exception {
		Guest guest = guestService.getGuestById(guestId);
		guestService.eraseGuestInfo(guest.username);
		return "redirect:/admin/guests";
	}
			
	@RequestMapping(value = "/guests/{guestId}/connectors")
	public ModelAndView guestConnectors(@PathVariable Long guestId,
			HttpServletRequest request) {
		ModelAndView mav = new ModelAndView("admin/index");
		mav.addObject("release", env.get("release"));
		setPreviousOperationMessage(request);
		mav.addObject("childView", "guests.jsp");
		mav.addObject("guestId", guestId);
		mav.addObject("helper", helper);
		mav.addObject("activeTab", "connectors");
		return mav;
	}

	private void setPreviousOperationMessage(HttpServletRequest request) {
		if (request.getSession().getAttribute("errorMessage") != null) {
			request.setAttribute("errorMessage", request.getSession()
					.getAttribute("errorMessage"));
			request.setAttribute("stackTrace", request.getSession()
					.getAttribute("stackTrace"));
			request.getSession().removeAttribute("errorMessage");
			request.getSession().removeAttribute("stackTrace");
		}
		if (request.getSession().getAttribute("successMessage") != null) {
			request.setAttribute("successMessage", request.getSession()
					.getAttribute("successMessage"));
			request.getSession().removeAttribute("successMessage");
		}
	}

	@RequestMapping(value = "/guests/{guestId}/connectors/{connectorName}")
	public ModelAndView generalConnector(@PathVariable Long guestId,
			@PathVariable String connectorName) {
		ModelAndView mav = new ModelAndView("admin/index");
		mav.addObject("release", env.get("release"));
		mav.addObject("childView", "guests.jsp");
		mav.addObject("guestId", guestId);
		mav.addObject("helper", helper);
		mav.addObject("connectorName", connectorName);
		return mav;
	}

	@RequestMapping(value = "/guests/{guestId}/sessions")
	public ModelAndView guestSessions(@PathVariable Long guestId) {
		ModelAndView mav = new ModelAndView("admin/index");
		mav.addObject("release", env.get("release"));
		mav.addObject("childView", "guests.jsp");
		mav.addObject("guest", guestId);
		mav.addObject("helper", helper);
		mav.addObject("activeTab", "sessions");
		return mav;
	}

	@RequestMapping(value = "/reinitializeConnectors")
	public String reinitializeConnectorData(@RequestParam String connectorName, HttpServletResponse response,
			HttpServletRequest request) throws IOException {
		Connector connector = Connector.getConnector(connectorName);
		Set<Long> connectorGuests = connectorUpdateService.getConnectorGuests(connector);
		long delay = 0;
		for (Long guestId : connectorGuests) {
			resetGuestConnectorData(connector, guestId, delay);
			delay += 60000;
		}
		setSuccessMessage(request, connector.getName() + " data will be re-imported for "
				+ connectorGuests.size() + " guests (one guest every minute)");
		return "redirect:/admin/general/connectors/stats";
	}
	
	private void resetGuestConnectorData(Connector connector, long guestId, long delay) {
		apiDataService.eraseApiData(guestId, connector);
		connectorUpdateService.deleteScheduledUpdates(guestId, connector);
		int[] objectTypeValues = connector.objectTypeValues();
		for (int objectTypes : objectTypeValues) {
			connectorUpdateService.scheduleUpdate(guestId,
					connector.getName(), objectTypes,
					UpdateType.INITIAL_HISTORY_UPDATE,
					System.currentTimeMillis()+delay);
		}
	}

	@RequestMapping(value = "/scheduleUpdate")
	public String scheduleUpdate(@RequestParam long guestId,
			@RequestParam String connectorName, HttpServletResponse response,
			HttpServletRequest request) throws IOException {
		Connector connector = Connector.getConnector(connectorName);
		try {
			int[] objectTypeValues = connector.objectTypeValues();
			for (int objectTypes : objectTypeValues) {
				ScheduledUpdate updt = connectorUpdateService
						.getNextScheduledUpdate(guestId, connector, objectTypes);
				if (updt != null)
					connectorUpdateService.reScheduleUpdate(updt,
							System.currentTimeMillis(), false);
				else
					connectorUpdateService.scheduleUpdate(guestId,
							connectorName, objectTypes,
							UpdateType.INITIAL_HISTORY_UPDATE,
							System.currentTimeMillis());
			}
			setSuccessMessage(request, "A history update for connector "
					+ connectorName + " has been scheduled");
		} catch (Throwable t) {
			String stackTrace = stackTrace(t);
			setErrorMessage(request, "Connector history update for "
					+ connectorName + " could not be scheduled", stackTrace);
		}
		return "redirect:/admin/guests/" + guestId + "/connectors";
	}

	@RequestMapping(value = "/updateConnectorData")
	public String updateConnectorData(@RequestParam long guestId,
			@RequestParam String connectorName, HttpServletResponse response,
			HttpServletRequest request) throws IOException {
		try {
			Connector connector = Connector.getConnector(connectorName);
			ApiKey apiKey = guestService.getApiKey(guestId, connector);
			AbstractUpdater updater = connectorUpdateService
					.getUpdater(connector);
			int[] objectTypeValues = connector.objectTypeValues();
			List<UpdateResult> updateResults = new ArrayList<UpdateResult>();
			if (objectTypeValues != null && objectTypeValues.length > 0) {
				for (int i = 0; i < objectTypeValues.length; i++) {
					UpdateInfo updateInfo = UpdateInfo.refreshFeedUpdateInfo(
							apiKey, objectTypeValues[i]);
					UpdateResult updateResult = updater.updateData(updateInfo);
					updateResults.add(updateResult);
				}
			} else {
				UpdateInfo updateInfo = UpdateInfo.refreshFeedUpdateInfo(
						apiKey, -1);
				UpdateResult updateResult = updater.updateData(updateInfo);
				updateResults.add(updateResult);
			}
			for (UpdateResult updateResult : updateResults) {
				if (updateResult.type != UpdateResult.ResultType.UPDATE_SUCCEEDED)
					setErrorMessage(request,
							"There was a problem updating some object type: "
									+ updateResult.type,
							updateResult.stackTrace);
			}
			setSuccessMessage(request, "Connector data " + connectorName
					+ " was updated successfully");
		} catch (Throwable t) {
			String stackTrace = stackTrace(t);
			setErrorMessage(request, "Could not update data for "
					+ connectorName, stackTrace);
		}
		return "redirect:/admin/guests/" + guestId + "/connectors";
	}

	@RequestMapping(value = "/resetConnector")
	public String resetConnector(@RequestParam long guestId,
			@RequestParam String connectorName, HttpServletResponse response,
			HttpServletRequest request) throws IOException {
		try {
			Connector connector = Connector.getConnector(connectorName);
			apiDataService.eraseApiData(guestId, connector);
			connectorUpdateService.deleteScheduledUpdates(guestId, connector);
			setSuccessMessage(request, "Connector " + connectorName
					+ " has been reset");
		} catch (Throwable t) {
			String stackTrace = stackTrace(t);
			setErrorMessage(request, "Connector " + connectorName
					+ " could not be reset", stackTrace);
		}
		return "redirect:/admin/guests/" + guestId + "/connectors";
	}

	private void setErrorMessage(HttpServletRequest request, String message,
			String stackTrace) {
		request.getSession().setAttribute("errorMessage", message);
		request.getSession().setAttribute("stackTrace", stackTrace);
	}

	private void setSuccessMessage(HttpServletRequest request, String message) {
		request.getSession().setAttribute("successMessage", message);
	}

}
