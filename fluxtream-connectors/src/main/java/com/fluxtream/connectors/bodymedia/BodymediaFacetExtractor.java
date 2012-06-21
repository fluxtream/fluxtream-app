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
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Extracts information from the apicall and creates a facet
 */
@Component
public class BodymediaFacetExtractor extends AbstractFacetExtractor
{

    //Logs various transactions
    Logger logger = Logger.getLogger(BodymediaFacetExtractor.class);

    @Override
    public List<AbstractFacet> extractFacets(ApiData apiData, ObjectType objectType) throws Exception
    {

        logger.info("guestId=" + apiData.updateInfo.getGuestId() +
        				" connector=bodymedia action=extractFacets objectType="
        						+ objectType.getName());

        ArrayList<AbstractFacet> facets = null;
        if(objectType.getName().equals("burn"))
        {
            facets = extractBurnFacet(apiData);
        }
        else //If the facet to be extracted wasn't a burn facet
        {
            logger.info("guestId=" + apiData.updateInfo.getGuestId() +
            				" connector=bodymedia action=extractFacets error=no burn object");
        }
        return facets;
    }

    /**
     * Extracts facets for each day from the data returned by the api.
     * @param apiData The data returned by the Burn api
     * @return A list of facets for each day provided by the apiData
     */
    private ArrayList<AbstractFacet> extractBurnFacet(ApiData apiData)
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
                for(Object o : daysArray)
                {
                    if(o instanceof JSONObject)
                    {
                        JSONObject day = (JSONObject) o;
                        BodymediaBurnFacet facet = new BodymediaBurnFacet();
                        //The following call must be made to load data about he facets
                        super.extractCommonFacetData(facet, apiData);
                        facet.setTotalCalories(day.getInt("totalCalories"));
                        facet.setDate(day.getString("date"));
                        facet.setEstimatedCalories(day.getInt("estimatedCalories"));
                        facet.setPredictedCalories(day.getInt("predictedCalories"));
                        facet.setBurnJson(day.getString("minutes"));
                        facet.setLastSync(d.getMillis()/1000);

                        //Sets the start and end times for the facet so that it can be uniquely defined
                        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd");
                        DateTime date = formatter.parseDateTime(day.getString("date"));
                        facet.start = date.getMillis()/1000;
                        date = date.plusDays(1);
                        facet.end = date.getMillis()/1000;

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
