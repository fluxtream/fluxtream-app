package com.fluxtream.mvc.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.connectors.vos.StartMinuteComparator;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.models.HomeModel;
import com.fluxtream.mvc.views.ViewsHelper;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.services.NotificationsService;
import com.fluxtream.services.SettingsService;
import com.fluxtream.updaters.strategies.UpdateStrategyFactory;

@Controller
public class ListController {

	private static final String X_FLX_EXECUTION_TIME = "X-FLX-Execution_Time";

	@Autowired
	GuestService guestService;

	@Autowired
	SettingsService settingsService;

	@Autowired
	ApiDataService apiDataService;

	@Autowired
	Configuration env;

	@Autowired
	UpdateStrategyFactory updateStrategyFactory;

	@Autowired
	MetadataService metadataService;

	@Autowired
	FacetsHelper facetsHelper;

	@Autowired
	NotificationsService notificationsService;
	
	@RequestMapping("/tabs/list/{page}")
	public ModelAndView loadListDigest(HttpServletRequest request,
			HttpServletResponse response, @PathVariable("page") int page)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {

		String filter = request.getParameter("filter");
		if (filter == null)
			filter = "";
		
		long then = System.currentTimeMillis();
		HomeModel homeModel = facetsHelper.getHomeModel(request);
		ModelAndView mav = new ModelAndView();
		long guestId = ControllerHelper.getGuestId();
		List<ApiKey> userKeys = guestService.getApiKeys(guestId);

		
		List<ApiKey> apiKeySelection = getApiKeySelection(guestId, filter);

		facetsHelper.removeGoogleLatitude(guestId, userKeys);

		List<AbstractInstantFacetVO<AbstractFacet>> facetVos = new ArrayList<AbstractInstantFacetVO<AbstractFacet>>();
		Set<String> haveDataConnectors = new HashSet<String>();
		GuestSettings settings = settingsService.getSettings(guestId);

		for (ApiKey apiKey : userKeys) {
			List<AbstractFacet> facets = facetsHelper.getFacets(request,
					apiKey.getConnector(), false);
			for (AbstractFacet facet : facets) {
				Class<? extends AbstractFacetVO<AbstractFacet>> jsonFacetClass = AbstractFacetVO
						.getFacetVOClass(facet);
				if (!AbstractInstantFacetVO.class
						.isAssignableFrom(jsonFacetClass))
					continue;
				haveDataConnectors.add(apiKey.getConnector().getName());
				if (!apiKeySelection.contains(apiKey))
					continue;
				AbstractInstantFacetVO<AbstractFacet> facetVo = (AbstractInstantFacetVO<AbstractFacet>) jsonFacetClass
						.newInstance();
				facetVo.extractValues(facet, homeModel.getTimeInterval(),
						settings);
				facetVos.add(facetVo);
			}
		}

		startTimeSortFacets(facetVos);
		facetVos = setPageLimits(page, mav, facetVos);

		mav.addObject("haveDataConnectors", haveDataConnectors);
		mav.addObject("selectedConnectors", connectorNames(apiKeySelection));
		mav.addObject("userConnectors", ViewsHelper.toConnectorModels(userKeys));
		mav.addObject("facets", facetVos);

		mav.addObject("settings", settings);
		mav.addObject("helper", facetsHelper);
		mav.addObject("pageSize",
				Integer.valueOf(env.get("LIST_PAGE_SIZE")));
		mav.addObject("manyPages", Integer.valueOf(env.get("MANY_PAGES")));
		mav.addObject("format", DateTimeFormat.forPattern("HH:mm"));

		mav.setViewName("calendar/list");
		response.setHeader(X_FLX_EXECUTION_TIME,
				String.valueOf(System.currentTimeMillis() - then));
		return mav;
	}

	@SuppressWarnings("unchecked")
	private void startTimeSortFacets(
			List<AbstractInstantFacetVO<AbstractFacet>> facetVos) {
		AbstractInstantFacetVO<AbstractFacet>[] facetArray = facetVos
				.toArray(new AbstractInstantFacetVO[0]);
		Arrays.sort(facetArray, new StartMinuteComparator());

		facetVos.clear();
		for (@SuppressWarnings("rawtypes")
		AbstractInstantFacetVO facet : facetArray) {
			facetVos.add(facet);
		}
	}

	private List<AbstractInstantFacetVO<AbstractFacet>> setPageLimits(int page,
			ModelAndView mav,
			List<AbstractInstantFacetVO<AbstractFacet>> facetVos) {
		int from = page * Integer.valueOf(env.get("LIST_PAGE_SIZE"));
		int to = (page + 1) * Integer.valueOf(env.get("LIST_PAGE_SIZE"));
		to = to < facetVos.size() ? to : facetVos.size();
		mav.addObject("total", facetVos.size());
		facetVos = facetVos.subList(from, to);
		mav.addObject("from", from);
		mav.addObject("to", to);
		mav.addObject("page", page);
		return facetVos;
	}

	private List<ApiKey> getApiKeySelection(long guestId, String filter) {
		List<ApiKey> userKeys = guestService.getApiKeys(guestId);
		String[] uncheckedConnectors = filter.split(",");
		List<String> filteredOutConnectors = new ArrayList<String>(
				Arrays.asList(uncheckedConnectors));

		List<ApiKey> apiKeySelection = getCheckedApiKeys(userKeys,
				filteredOutConnectors);
		return apiKeySelection;
	}

	/**
	 * 
	 * @param apiKeys
	 * @param uncheckedConnectors
	 * @return the list of api keys corresponding to the names in
	 *         checkedConnectors
	 */
	private List<ApiKey> getCheckedApiKeys(List<ApiKey> apiKeys,
			List<String> uncheckedConnectors) {
		List<ApiKey> result = new ArrayList<ApiKey>();
		there: for (ApiKey apiKey : apiKeys) {
			for (int i = 0; i < uncheckedConnectors.size(); i++) {
				String connectorName = uncheckedConnectors.get(i);
				if (apiKey.getConnector().getName().equals(connectorName)) {
					continue there;
				}
			}
			result.add(apiKey);
		}
		return result;
	}

	private List<String> connectorNames(List<ApiKey> apis) {
		List<String> connectorNames = new ArrayList<String>();
		for (ApiKey apiKey : apis)
			connectorNames.add(apiKey.getConnector().getName());
		return connectorNames;
	}
	
}
