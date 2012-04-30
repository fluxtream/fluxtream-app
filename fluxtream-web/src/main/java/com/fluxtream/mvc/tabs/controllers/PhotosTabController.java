package com.fluxtream.mvc.tabs.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.controllers.FacetsHelper;
import com.fluxtream.mvc.models.HomeModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.GuestService;

@Controller
public class PhotosTabController {

	@Autowired
	GuestService guestService;

	@Autowired
	ApiDataService apiDataService;

	@Autowired
	Configuration env;

	@Autowired
	FacetsHelper facetsHelper;

	@RequestMapping("/tabs/photos")
	public ModelAndView getPhotos(HttpServletRequest request) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		HomeModel homeModel = ControllerHelper.getHomeModel(request);
		ModelAndView mav = new ModelAndView("tabs/photos");
		List<AbstractFacetVO<AbstractFacet>> facetVos = getImageFacetVOs(
				request, homeModel);

		mav.addObject("facets", facetVos);
		return mav;
	}

	@RequestMapping("/tabs/photos/carousel")
	public ModelAndView getPhotosCarousel(HttpServletRequest request) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		HomeModel homeModel = ControllerHelper.getHomeModel(request);
		ModelAndView mav = new ModelAndView("tabs/photosCarousel");
		List<AbstractFacetVO<AbstractFacet>> facetVos = getImageFacetVOs(
				request, homeModel);

		mav.addObject("facets", facetVos);
		return mav;
	}

	private List<AbstractFacetVO<AbstractFacet>> getImageFacetVOs(
			HttpServletRequest request, HomeModel homeModel)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		long guestId = ControllerHelper.getGuestId();

		List<ApiKey> userKeys = guestService.getApiKeys(guestId);
		List<ApiKey> imageConnectorKeys = new ArrayList<ApiKey>();
		for (ApiKey apiKey : userKeys) {
			if (apiKey.getConnector().hasImageObjectType())
				imageConnectorKeys.add(apiKey);
		}

		List<AbstractFacetVO<AbstractFacet>> facetVos = new ArrayList<AbstractFacetVO<AbstractFacet>>();
		for (ApiKey apiKey : imageConnectorKeys) {
			List<AbstractFacet> facets = facetsHelper.getFacets(request,
					apiKey.getConnector(), false);
			for (AbstractFacet facet : facets) {
				Class<? extends AbstractFacetVO<AbstractFacet>> jsonFacetClass = AbstractFacetVO
						.getFacetVOClass(facet);
				AbstractInstantFacetVO<AbstractFacet> facetVo = (AbstractInstantFacetVO<AbstractFacet>) jsonFacetClass
						.newInstance();
				facetVo.extractValues(facet, homeModel.getTimeInterval(), null);
				facetVos.add(facetVo);
			}
		}
		return facetVos;
	}

}
