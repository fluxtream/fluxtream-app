package com.fluxtream.connectors.bodymedia;

import java.util.ArrayList;
import java.util.List;
import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
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

        ArrayList<AbstractFacet> facets = null;
        JSONObject bodymediaResponse = JSONObject.fromObject(apiData.json);
        if(objectType.getName().equals("burn"))
        {
            facets = extractBurnFacet(bodymediaResponse, apiData);
        }
        else //If the facet to be extracted wasn't a burn facet
        {
            logger.info("guestId=" + apiData.updateInfo.getGuestId() +
            				" connector=bodymedia action=extractFacets error=no burn object");
        }
        return facets;
    }

    /**
     *
     * @param bodymediaResponse
     * @param apiData
     * @return
     */
    private ArrayList<AbstractFacet> extractBurnFacet(final JSONObject bodymediaResponse, ApiData apiData) {
        ArrayList<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        /* burnJson is a JSONArray that contains a seperate JSONArray and calorie counts for each day
         */
        if(bodymediaResponse.has("days"))
        {
            try
            {
                JSONArray daysArray = bodymediaResponse.getJSONArray("days");
                for(Object o : daysArray)
                {
                    if(o instanceof JSONObject)
                    {
                        JSONObject day = (JSONObject) o;
                        BodymediaBurnFacet facet = new BodymediaBurnFacet();
                        //The following call must be made
                        super.extractCommonFacetData(facet, apiData);
                        facet.setTotalCalories(day.getInt("totalCalories"));
                        facet.setDate(day.getString("date"));
                        facet.setEstimatedCalories(day.getInt("estimatedCalories"));
                        facet.setPredictedCalories(day.getInt("predictedCalories"));
                        facet.setBurnJson(day.getString("minutes"));
                        facets.add(facet);
                    }
                }
            }
            catch (JSONException e)
            {
                logger.info("guestId=" + apiData.updateInfo.getGuestId() +
                       				" connector=bodymedia action=extractFacets error=JSON incorrectly formatted");
            }
        }
        return facets;
    }
}
