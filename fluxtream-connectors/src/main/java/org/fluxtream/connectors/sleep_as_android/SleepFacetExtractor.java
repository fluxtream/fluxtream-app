package org.fluxtream.connectors.sleep_as_android;

import org.fluxtream.core.ApiData;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.facets.extractors.AbstractFacetExtractor;

import java.util.ArrayList;
import java.util.List;

public class SleepFacetExtractor extends AbstractFacetExtractor {
    @Override
    public List<AbstractFacet> extractFacets(UpdateInfo updateInfo, ApiData apiData, ObjectType objectType) throws Exception {
        return new ArrayList<AbstractFacet>();
    }
}
