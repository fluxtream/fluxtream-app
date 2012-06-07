package com.fluxtream.connectors.bodymedia;

import java.util.ArrayList;
import java.util.List;
import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Extracts information from the apicall and creates a facet
 * TODO Finish extractor body
 */
@Component
public class BodymediaFacetExtractor extends AbstractFacetExtractor {

    //Logs various transactions
    Logger logger = Logger.getLogger(BodymediaFacetExtractor.class);

    @Override
    public List<AbstractFacet> extractFacets(ApiData apiData, ObjectType objectType) throws Exception {

        logger.info("guestId=" + apiData.updateInfo.getGuestId() +
        				" connector=bodymedia action=extractFacets objectType="
        						+ objectType.getName());

        ArrayList<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        JSONObject bodymediaResponse = JSONObject.fromObject(apiData.json);
        if(objectType.getName().equals("burn"))
        {
            facets.add(extractBurnFacet(bodymediaResponse));
        }
        else //If the facet to be extracted wasn't a burn facet
        {
            logger.info("guestId=" + apiData.updateInfo.getGuestId() +
            				" connector=bodymedia action=extractFacets error=no burn object");
        }
        return facets;
    }

    /**
     * Extracts a burn facet from the json response
     * @param bodymediaResponse The response sent by bodymedia's api
     * @return A burn facet
     */
    private BodymediaBurnFacet extractBurnFacet(final JSONObject bodymediaResponse) {
        BodymediaBurnFacet facet = new BodymediaBurnFacet();
        if(bodymediaResponse.has("totalCalories"))
        {
            facet.setTotalCalories(bodymediaResponse.getInt("totalCalories"));
        }
        if(bodymediaResponse.has("averageCalories"))
        {
            facet.setAverageCalories(bodymediaResponse.getInt("averageCalories"));
        }
        /* days is a JSONArray that contains a seperate JSONArray and calorie counts for each day
         */
        if(bodymediaResponse.has("days"))
        {
            Object minuteArray = bodymediaResponse.get("days");
            if(minuteArray instanceof JSONArray)
                facet.setDays((JSONArray) minuteArray);
        }
        return facet;
    }
}
