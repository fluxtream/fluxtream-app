package com.fluxtream.mvc.controllers;

import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.services.ApiDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * User: candide
 * Date: 21/09/13
 * Time: 15:46
 */
@Controller
@RequestMapping("/openGraph")
public class OpenGraphController {

    @Autowired
    ApiDataService apiDataService;

    @RequestMapping("/{api}/{objectType}/{facetId}.html")
    public ModelAndView index(@PathVariable("api") int api,
                              @PathVariable("objectType") int objectType,
                              @PathVariable("facetId") int facetId) {
        final AbstractFacetVO<AbstractFacet> facet = apiDataService.getFacet(api, objectType, facetId);
        ModelAndView mav = new ModelAndView("openGraph/facet");
        mav.addObject("facet", facet);
        return mav;
    }
}
