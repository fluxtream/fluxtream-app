package com.fluxtream.mvc.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.models.HomeModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SettingsService;

@Controller
public class TooltipsController {


	@Autowired
	FacetsHelper facetsHelper;

	@Autowired
	GuestService guestService;

	@Autowired
	SettingsService settingsService;

	@Autowired
	ApiDataService apiDataService;

	@Autowired
	Configuration env;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@RequestMapping("/calendar/tooltips")
	public ModelAndView getTooltips(HttpServletRequest request)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		HomeModel homeModel = facetsHelper.getHomeModel(request);
		ModelAndView mav = new ModelAndView("calendar/tooltips");
		long guestId = ControllerHelper.getGuestId();
		GuestSettings settings = settingsService.getSettings(guestId);
		List<ApiKey> userKeys = guestService.getApiKeys(guestId);
		facetsHelper.removeGoogleLatitude(guestId, userKeys);
		List<AbstractInstantFacetVO<AbstractFacet>> facetVos = new ArrayList<AbstractInstantFacetVO<AbstractFacet>>();
		Set<String> haveDataConnectors = new HashSet<String>();
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
				AbstractInstantFacetVO<AbstractFacet> facetVo = (AbstractInstantFacetVO<AbstractFacet>) jsonFacetClass
						.newInstance();
				facetVo.extractValues(facet, homeModel.getTimeInterval(), settings);
				facetVos.add(facetVo);
			}
		}

		AbstractInstantFacetVO[] facetArray = facetVos
				.toArray(new AbstractInstantFacetVO[0]);
		Arrays.sort(facetArray, new Comparator<AbstractInstantFacetVO>() {
			@Override
			public int compare(AbstractInstantFacetVO f1,
					AbstractInstantFacetVO f2) {
				return f1.start > f2.start ? 1 : -1;
			}
		});

		facetVos.clear();
		for (AbstractInstantFacetVO facet : facetArray) {
			facetVos.add(facet);
		}
		mav.addObject("format", DateTimeFormat.forPattern("HH:mm"));
		mav.addObject("facets", facetVos);
		mav.addObject("settings", settings);
		mav.addObject("helper", facetsHelper);
		return mav;
	}
}
