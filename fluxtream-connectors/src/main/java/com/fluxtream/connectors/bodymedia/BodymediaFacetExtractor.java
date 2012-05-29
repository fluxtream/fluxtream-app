package com.fluxtream.connectors.bodymedia;

import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts information from the apicall and creates a facet
 * TODO Finish extractor body
 */
@Component
public class BodymediaFacetExtractor extends AbstractFacetExtractor {
    @Override
    public List<AbstractFacet> extractFacets(ApiData apiData, ObjectType objectType) throws Exception {
        return new ArrayList<AbstractFacet>();  //To change body of implemented methods use File | Settings | File Templates.
    }
}
