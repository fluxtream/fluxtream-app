package org.fluxtream.connectors.bodymedia;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.fluxtream.core.ApiData;
import org.fluxtream.core.TimezoneMap;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.facets.extractors.AbstractFacetExtractor;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.MetadataService;
import org.fluxtream.core.utils.TimeUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.LocalDate;

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
    FlxLogger logger = FlxLogger.getLogger(BodymediaBurnFacetExtractor.class);

    DateTimeFormatter form = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZ");
    DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd");

    @Qualifier("metadataServiceImpl")
    @Autowired
    MetadataService metadataService;

    @Qualifier("connectorUpdateServiceImpl")
    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Override
    public List<AbstractFacet> extractFacets(final UpdateInfo updateInfo, final ApiData apiData,
                                             ObjectType objectType) throws Exception
    {

        logger.info("guestId=" + apiData.updateInfo.getGuestId() +
                    " connector=bodymedia action=extractFacets objectType=" + objectType.getName());

        ArrayList<AbstractFacet> facets;
        String name = objectType.getName();
        if(name.equals("burn"))
        {
            facets = extractBurnFacets(updateInfo,apiData);
        }
        else //If the facet to be extracted wasn't a burn facet
        {
            throw new JSONException("Burn extractor called with illegal ObjectType");
        }
        return facets;
    }

    /**
     * Extracts facets for each day from the data returned by the api.
     * @param apiData The data returned by the Burn api
     * @return A list of facets for each day provided by the apiData
     */
    private ArrayList<AbstractFacet> extractBurnFacets(final UpdateInfo updateInfo, ApiData apiData)
    {
        ArrayList<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        /* burnJson is a JSONArray that contains a seperate JSONArray and calorie counts for each day
         */
        JSONObject bodymediaResponse = JSONObject.fromObject(apiData.json);
        if(bodymediaResponse.has("days") && bodymediaResponse.has("lastSync"))
        {
            DateTime d = form.parseDateTime(bodymediaResponse.getJSONObject("lastSync").getString("dateTime"));

            // Get timezone map from UpdateInfo context
            TimezoneMap tzMap = (TimezoneMap)updateInfo.getContext("tzMap");

            // Insert lastSync into the updateInfo context so it's accessible to the updater
            updateInfo.setContext("lastSync", d);

            JSONArray daysArray = bodymediaResponse.getJSONArray("days");
            for(Object o : daysArray)
            {
                if(o instanceof JSONObject)
                {
                    JSONObject day = (JSONObject) o;
                    BodymediaBurnFacet burn = new BodymediaBurnFacet(apiData.updateInfo.apiKey.getId());
                    //The following call must be made to load data about he facets
                    super.extractCommonFacetData(burn, apiData);
                    burn.setTotalCalories(day.getInt("totalCalories"));
                    burn.date = day.getString("date");
                    burn.setEstimatedCalories(day.getInt("estimatedCalories"));
                    burn.setPredictedCalories(day.getInt("predictedCalories"));
                    burn.json = day.getString("minutes");
                    burn.lastSync = d.getMillis();

                    DateTime date = formatter.parseDateTime(day.getString("date"));
                    burn.date = TimeUtils.dateFormatter.print(date.getMillis());

                    if(tzMap!=null)
                    {
                        // Create a LocalDate object which just captures the date without any
                        // timezone assumptions
                        LocalDate ld = new LocalDate(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth());
                        // Use tzMap to convert date into a datetime with timezone information
                        DateTime realDateStart = tzMap.getStartOfDate(ld);
                        // Set the start and end times for the facet.  The start time is the leading midnight
                        // of burn.date according to BodyMedia's idea of what timezone you were in then.
                        // End should, I think, be start + the number of minutes in the minutes array *
                        // the number of milliseconds in a minute.
                        burn.start = realDateStart.getMillis();
                        int minutesLength = 1440;
                        burn.end = burn.start + DateTimeConstants.MILLIS_PER_MINUTE * minutesLength;
                    }
                    else {
                        // This is the old code from Prasanth that uses metadataService, which isn't right
                        TimeZone timeZone = metadataService.getTimeZone(apiData.updateInfo.getGuestId(), date.getMillis());
                        long fromMidnight = TimeUtils.fromMidnight(date.getMillis(), timeZone);
                        long toMidnight = TimeUtils.toMidnight(date.getMillis(), timeZone);
                        //Sets the start and end times for the facet so that it can be uniquely defined
                        burn.start = fromMidnight;
                        burn.end = toMidnight;
                    }


                    facets.add(burn);
                }
                else
                    throw new RuntimeException("days array is not a proper JSONObject");
            }
        }
        return facets;
    }
}
