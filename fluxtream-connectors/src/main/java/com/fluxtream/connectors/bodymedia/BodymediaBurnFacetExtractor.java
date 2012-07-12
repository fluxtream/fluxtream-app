package com.fluxtream.connectors.bodymedia;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import com.fluxtream.ApiData;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.facets.extractors.AbstractFacetExtractor;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.utils.TimeUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Extracts information from the apicall and creates a facet
 */
@Component
public class BodymediaBurnFacetExtractor extends AbstractFacetExtractor
{

    //Logs various transactions
    Logger logger = Logger.getLogger(BodymediaBurnFacetExtractor.class);

    DateTimeFormatter form = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZ");
    DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd");

    @Qualifier("metadataServiceImpl")
    @Autowired
    MetadataService metadataService;

    @Qualifier("connectorUpdateServiceImpl")
    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Override
    public List<AbstractFacet> extractFacets(ApiData apiData, ObjectType objectType) throws Exception
    {

        logger.info("guestId=" + apiData.updateInfo.getGuestId() +
        				" connector=bodymedia action=extractFacets objectType="
        						+ objectType.getName());

        ArrayList<AbstractFacet> facets;
        String name = objectType.getName();
        if(name.equals("burn"))
        {
            facets = extractBurnFacets(apiData);
        }
        else //If the facet to be extracted wasn't a burn facet
        {
            throw new JSONException("Burn extractor called with illegal ObjectType");
        }
        return facets;
    }

    @Override
    public void setUpdateInfo(final UpdateInfo updateInfo) {
        super.setUpdateInfo(updateInfo);
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
            DateTime d = form.parseDateTime(bodymediaResponse.getJSONObject("lastSync").getString("dateTime"));
            JSONArray daysArray = bodymediaResponse.getJSONArray("days");
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
                    burn.setJson(day.getString("minutes"));
                    burn.setLastSync(d.getMillis());

                    DateTime date = formatter.parseDateTime(day.getString("date"));
                    burn.date = dateFormatter.print(date.getMillis());
                    TimeZone timeZone = metadataService.getTimeZone(apiData.updateInfo.getGuestId(), date.getMillis());
                    long fromMidnight = TimeUtils.fromMidnight(date.getMillis(), timeZone);
                    long toMidnight = TimeUtils.toMidnight(date.getMillis(), timeZone);
                    //Sets the start and end times for the facet so that it can be uniquely defined
                    burn.start = fromMidnight;
                    burn.end = toMidnight;

                    facets.add(burn);
                }
                else
                    throw new RuntimeException("days array is not a proper JSONObject");
            }
        }
        return facets;
    }
}
