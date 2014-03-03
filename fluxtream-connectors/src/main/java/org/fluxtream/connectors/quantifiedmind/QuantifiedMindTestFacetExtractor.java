package org.fluxtream.connectors.quantifiedmind;

import java.util.ArrayList;
import java.util.List;
import org.fluxtream.ApiData;
import org.fluxtream.connectors.ObjectType;
import org.fluxtream.connectors.updaters.UpdateInfo;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.facets.extractors.AbstractFacetExtractor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component
public class QuantifiedMindTestFacetExtractor extends AbstractFacetExtractor {

    @Override
    public List<AbstractFacet> extractFacets(final UpdateInfo updateInfo, final ApiData apiData, final ObjectType objectType) throws Exception {
        List<AbstractFacet> facets = new ArrayList<AbstractFacet>();

        JSONArray array = JSONArray.fromObject(apiData.json);
        for(int i=0; i<array.size(); i++) {
            QuantifiedMindTestFacet facet = new QuantifiedMindTestFacet(apiData.updateInfo.apiKey.getId());

            JSONObject testData = array.getJSONObject(i);
            extractCommonFacetData(facet, apiData);

            facet.start = testData.getLong("test_timestamp")*1000;
            facet.end = facet.start;

            facet.test_name = testData.getString("test_name");
            facet.result_name = testData.getString("result_name");
            facet.result_value = testData.getDouble("result_value");
            facet.session_timestamp = testData.getLong("session_timestamp")*1000;

            facets.add(facet);
        }

        return facets;
    }
}
