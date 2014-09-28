package org.fluxtream.mvc.controllers;

import org.fluxtream.core.Configuration;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.vos.AbstractFacetVO;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

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

    @Autowired
    MetadataService metadataService;

    @Autowired
    Configuration env;

    @RequestMapping("/{encryptedParameters}.html")
    public ModelAndView index(@PathVariable("encryptedParameters") String encryptedParameters) throws UnsupportedEncodingException {
        String params = env.decrypt(encryptedParameters);
        params = URLDecoder.decode(params, "UTF-8");
        final String[] parameters = params.split("/");
        if (parameters.length!=3)
            throw new RuntimeException("Unexpected number of parameters: " + parameters.length);
        int api = Integer.valueOf(parameters[0]);
        int objectType = Integer.valueOf(parameters[1]);
        long facetId = Long.valueOf(parameters[2]);
        final AbstractFacetVO<AbstractFacet> facet = apiDataService.getFacet(api, objectType, facetId);
        final Connector connector = Connector.fromValue(facet.api);
        String facetName = String.format("%s.%s", connector.getName(), ObjectType.getObjectType(connector, facet.objectType));
        ModelAndView mav = new ModelAndView("openGraph/" + facetName);
        mav.addObject("facet", facet);
        mav.addObject("metadataService", metadataService);
        return mav;
    }
}
