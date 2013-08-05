package com.fluxtream.connectors.sms_backup;

import java.util.ArrayList;
import java.util.List;
import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import org.springframework.stereotype.Component;

@Component
public class SmsEntryFacetExtractor extends AbstractFacetExtractor {

    @Override
    public List<AbstractFacet> extractFacets(final UpdateInfo updateInfo, final ApiData apiData, final ObjectType objectType) throws Exception {
        return new ArrayList<AbstractFacet>();
    }

}
