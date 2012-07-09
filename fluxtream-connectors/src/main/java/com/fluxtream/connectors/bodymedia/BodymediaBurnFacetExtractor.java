package com.fluxtream.connectors.bodymedia;

import java.util.ArrayList;
import java.util.List;
import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import com.fluxtream.services.ConnectorUpdateService;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Extracts information from the apicall and creates a facet
 */
@Component
public class BodymediaBurnFacetExtractor extends AbstractFacetExtractor
{

    //Logs various transactions
    Logger logger = Logger.getLogger(BodymediaBurnFacetExtractor.class);

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    private final static int BURN_OBJECT_VALUE = 1;

    @Override
    public List<AbstractFacet> extractFacets(ApiData apiData, ObjectType objectType) throws Exception
    {

        logger.info("guestId=" + apiData.updateInfo.getGuestId() +
        				" connector=bodymedia action=extractFacets objectType="
        						+ objectType.getName());

        ArrayList<AbstractFacet> facets = null;
        String name = objectType.getName();
        if(name.equals("burn"))
        {
            facets = extractBurnFacets(apiData);
        }
        else //If the facet to be extracted wasn't a burn facet
        {
            throw new RuntimeException("Burn extractor called with illegal ObjectType");
        }
        return facets;
    }

    /**
     * Extracts facets for each day from the data returned by the api.
     * @param apiData The data returned by the Burn api
     * @return A list of facets for each day provided by the apiData
     */
    private ArrayList<AbstractFacet> extractBurnFacets(ApiData apiData)
    {
        ArrayList<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        /* burnJson is a JSONArray that contains a seperate JSONArray and calorie counts for each day
         */
        JSONObject bodymediaResponse = JSONObject.fromObject(apiData.json);
        if(bodymediaResponse.has("days"))
        {
            try
            {
                DateTimeFormatter form = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZ");
                DateTime d = form.parseDateTime(bodymediaResponse.getJSONObject("lastSync").getString("dateTime"));
                JSONArray daysArray = bodymediaResponse.getJSONArray("days");
                long then = System.currentTimeMillis();
                for(Object o : daysArray)
                {
                    if(o instanceof JSONObject)
                    {
                        JSONObject day = (JSONObject) o;
                        BodymediaBurnFacet burn = new BodymediaBurnFacet();
                        //The following call must be made to load data about he facets
                        super.extractCommonFacetData(burn, apiData);
                        burn.setTotalCalories(day.getInt("totalCalories"));
                        burn.setDate(day.getString("date"));
                        burn.setEstimatedCalories(day.getInt("estimatedCalories"));
                        burn.setPredictedCalories(day.getInt("predictedCalories"));
                        burn.setBurnJson(day.getString("minutes"));

                        //Sets the start and end times for the facet so that it can be uniquely defined
                        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd");
                        DateTime date = formatter.parseDateTime(day.getString("date"));
                        burn.start = date.getMillis()/1000;
                        date = date.plusDays(1);
                        burn.end = date.getMillis()/1000;

                        facets.add(burn);
                    }
                }

                connectorUpdateService.addApiUpdate(updateInfo.getGuestId(), connector(),
            						BURN_OBJECT_VALUE, then, System.currentTimeMillis() - then,
                                    "http://api.bodymedia.com/v2/json/burn/day/minute/intensity/", true, d.getMillis());
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
