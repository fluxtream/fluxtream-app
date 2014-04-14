package org.fluxtream.connectors.sms_backup;

import java.util.ArrayList;
import java.util.List;
import org.fluxtream.core.ApiData;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.facets.extractors.AbstractFacetExtractor;
import org.springframework.stereotype.Component;

@Component
public class SmsEntryFacetExtractor extends AbstractFacetExtractor {

    @Override
    public List<AbstractFacet> extractFacets(final UpdateInfo updateInfo, final ApiData apiData, final ObjectType objectType) throws Exception {
        return new ArrayList<AbstractFacet>();
    }

}
