package com.fluxtream.mvc.admin.controllers;

import static com.fluxtream.utils.Utils.stackTrace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.Configuration;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.FullTextSearchService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.google.gson.Gson;

@Controller
@RequestMapping(value = "/admin")
public class AdminAjaxController {

	@Autowired
	ThreadPoolTaskExecutor executor;

	@Autowired
	Configuration env;

	@Autowired
	ConnectorUpdateService connectorUpdateService;

	@Autowired
	MetadataService metadataService;

	@Autowired
	GuestService guestService;

	@Autowired
	ApiDataService apiDataService;

	@PersistenceContext
	EntityManager em;

	@Autowired
	FullTextSearchService searchService;

	private final Gson gson = new Gson();

	@RequestMapping(value = "/cleanupRunningUpdateTasks")
	public void cleanupRunningUpdateTasks(HttpServletResponse response) {
		connectorUpdateService.cleanupRunningUpdateTasks();
	}

	@SuppressWarnings({ "rawtypes" })
	@RequestMapping(value = "/searchFacets")
	public ModelAndView searchFacets(@RequestParam long guestId,
			@RequestParam String terms, HttpServletResponse response)
			throws Throwable {
		try {
			List<AbstractFacet> facets = searchService.searchFacetsIndex(
					guestId, terms);

			List<AbstractFacetVO> jsonFacets = new ArrayList<AbstractFacetVO>();
			TimeZone currentTimeZone = metadataService
					.getCurrentTimeZone(guestId);
			TimeInterval timeInterval = new TimeInterval(new Date().getTime(),
					System.currentTimeMillis(), TimeUnit.DAY, currentTimeZone);
			for (AbstractFacet facet : facets) {
				Class<? extends AbstractFacetVO<AbstractFacet>> jsonFacetClass = AbstractFacetVO
						.getFacetVOClass(facet);
				AbstractFacetVO<AbstractFacet> jsonFacet = jsonFacetClass
						.newInstance();
				jsonFacet.extractValues(facet, timeInterval, null);
				jsonFacets.add(jsonFacet);
			}

			ModelAndView mav = new ModelAndView("views/facets");
			mav.addObject("facets", jsonFacets);

			return mav;
		} catch (Throwable t) {
			String stackTrace = stackTrace(t);
			response.getWriter().write(stackTrace);
			throw t;
		}
	}

	@RequestMapping(value = "/initLuceneIndex")
	public void initLuceneIndex(HttpServletResponse response)
			throws IOException {
		String json = "";
		try {
			searchService.reinitializeIndex();
			json = gson.toJson(new StatusModel(true,
					"Lucene index successfully initialized"));
		} catch (Throwable t) {
			String stackTrace = stackTrace(t);
			json = gson.toJson(new StatusModel(false,
					"Could not initialize Lucene index", stackTrace));
		}
		response.getWriter().print(json);
	}

}
