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
public class BodymediaStepFacetExtractor extends AbstractFacetExtractor
{

    //Logs various transactions
    Logger logger = Logger.getLogger(BodymediaStepFacetExtractor.class);

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    private final static int STEP_OBJECT_VALUE = 2;

    @Override
    public List<AbstractFacet> extractFacets(ApiData apiData, ObjectType objectType) throws Exception
    {

        logger.info("guestId=" + apiData.updateInfo.getGuestId() +
        				" connector=bodymedia action=extractFacets objectType="
        						+ objectType.getName());

        ArrayList<AbstractFacet> facets = null;
        String name = objectType.getName();
        if(name.equals("steps"))
        {
            facets = extractStepFacets(apiData);
        }
        else //If the facet to be extracted wasn't a step facet
        {
            throw new RuntimeException("Step extractor called with illegal ObjectType");
        }
        return facets;
    }

    private ArrayList<AbstractFacet> extractStepFacets(final ApiData apiData)
    {
        ArrayList<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        /* burnJson is a JSONArray that contains a seperate JSONArray and calorie counts for each day
         */
        JSONObject bodymediaResponse = JSONObject.fromObject(apiData.json);
        try{
            JSONArray daysArray = bodymediaResponse.getJSONArray("days");
            long then = System.currentTimeMillis();
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd");
            DateTimeFormatter form = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZ");
            DateTime d = form.parseDateTime(bodymediaResponse.getJSONObject("lastSync").getString("dateTime"));
            for(Object o : daysArray)
            {
                if(o instanceof JSONObject)
                {
                    JSONObject day = (JSONObject) o;
                    BodymediaStepsFacet step = new BodymediaStepsFacet();
                    super.extractCommonFacetData(step, apiData);
                    step.setTotalSteps(day.getInt("totalSteps"));
                    step.setDate(day.getString("date"));
                    step.setStepJson(day.getString("hours"));

                    DateTime date = formatter.parseDateTime(day.getString("date"));
                    step.start = date.getMillis()/1000;
                    date = date.plusDays(1);
                    step.end = date.getMillis()/1000;

                    facets.add(step);
                }
            }

            connectorUpdateService.addApiUpdate(updateInfo.getGuestId(), connector(),
        						STEP_OBJECT_VALUE, then, System.currentTimeMillis() - then,
                                "http://api.bodymedia.com/v2/json/step/day/hour/", true, d.getMillis());
        } catch (JSONException e)
        {
            logger.info("guestId=" + apiData.updateInfo.getGuestId() +
                        " connector=bodymedia action=extractFacets error=JSON incorrectly formatted");
        }

        return facets;
    }
}
