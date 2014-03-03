package org.fluxtream.connectors.sms_backup;

import java.util.ArrayList;
import java.util.List;
import org.fluxtream.ApiData;
import org.fluxtream.connectors.ObjectType;
import org.fluxtream.connectors.updaters.UpdateInfo;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.facets.extractors.AbstractFacetExtractor;
import org.springframework.stereotype.Component;

@Component
public class SmsEntryFacetExtractor extends AbstractFacetExtractor {

    @Override
    public List<AbstractFacet> extractFacets(final UpdateInfo updateInfo, final ApiData apiData, final ObjectType objectType) throws Exception {
        return new ArrayList<AbstractFacet>();
    }

}
