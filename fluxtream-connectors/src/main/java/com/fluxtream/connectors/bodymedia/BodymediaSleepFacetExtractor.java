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
public class BodymediaSleepFacetExtractor extends AbstractFacetExtractor
{

    //Logs various transactions
    Logger logger = Logger.getLogger(BodymediaSleepFacetExtractor.class);

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    private final static int SLEEP_OBJECT_VALUE = 4;

    @Override
    public List<AbstractFacet> extractFacets(ApiData apiData, ObjectType objectType) throws Exception
    {

        logger.info("guestId=" + apiData.updateInfo.getGuestId() +
        				" connector=bodymedia action=extractFacets objectType="
        						+ objectType.getName());

        ArrayList<AbstractFacet> facets = null;
        String name = objectType.getName();
        if(name.equals("sleep"))
        {
            facets = extractSleepFacets(apiData);
        }
        else //If the facet to be extracted wasn't a step facet
        {
            throw new RuntimeException("Sleep extractor called with illegal ObjectType");
        }
        return facets;
    }

    /**
     * Extracts Data from the Sleep api.
     * @param apiData The data returned by bodymedia
     * @return
     */
    private ArrayList<AbstractFacet> extractSleepFacets(final ApiData apiData)
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
                    BodymediaSleepFacet sleep = new BodymediaSleepFacet();
                    super.extractCommonFacetData(sleep, apiData);
                    sleep.setDate(day.getString("date"));
                    sleep.setEfficiency(day.getDouble("efficiency"));
                    sleep.setTotalLying(day.getInt("totalLying"));
                    sleep.setTotalSleeping(day.getInt("totalSleep"));
                    sleep.setSleepJson(day.getString("sleepPeriods"));

                    DateTime date = formatter.parseDateTime(day.getString("date"));
                    sleep.start = date.getMillis()/1000;
                    date = date.plusDays(1);
                    sleep.end = date.getMillis()/1000;

                    facets.add(sleep);
                }
            }

            connectorUpdateService.addApiUpdate(updateInfo.getGuestId(), connector(),
        						SLEEP_OBJECT_VALUE, then, System.currentTimeMillis() - then,
                                "http://api.bodymedia.com/v2/json/sleep/day/period/", true, d.getMillis());
        } catch (JSONException e)
        {
            logger.info("guestId=" + apiData.updateInfo.getGuestId() +
                        " connector=bodymedia action=extractFacets error=JSON incorrectly formatted");
        }

        return facets;
    }

}
