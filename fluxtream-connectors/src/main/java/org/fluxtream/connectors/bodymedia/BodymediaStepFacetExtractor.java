package org.fluxtream.connectors.bodymedia;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.fluxtream.core.ApiData;
import org.fluxtream.core.TimezoneMap;
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
import org.fluxtream.core.aspects.FlxLogger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Extracts information from the apicall and creates a facet
 */
@Component
public class BodymediaStepFacetExtractor extends AbstractFacetExtractor
{

    //Logs various transactions
    FlxLogger logger = FlxLogger.getLogger(BodymediaStepFacetExtractor.class);

    @Qualifier("connectorUpdateServiceImpl")
    @Autowired
    ConnectorUpdateService connectorUpdateService;

    DateTimeFormatter form = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZ");
    DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd");

    @Qualifier("metadataServiceImpl")
    @Autowired
    MetadataService metadataService;

    @Override
    public List<AbstractFacet> extractFacets(final UpdateInfo updateInfo, final ApiData apiData,
                                             final ObjectType objectType) throws Exception
    {

        logger.info("guestId=" + apiData.updateInfo.getGuestId() +
        				" connector=bodymedia action=extractFacets objectType="
        						+ objectType.getName());

        ArrayList<AbstractFacet> facets;
        String name = objectType.getName();
        if(name.equals("steps"))
        {
            facets = extractStepFacets(updateInfo, apiData);
        }
        else //If the facet to be extracted wasn't a step facet
        {
            throw new RuntimeException("Step extractor called with illegal ObjectType");
        }
        return facets;
    }

    private ArrayList<AbstractFacet> extractStepFacets(final UpdateInfo updateInfo, final ApiData apiData)
    {
        ArrayList<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        /* burnJson is a JSONArray that contains a seperate JSONArray and calorie counts for each day
         */
        JSONObject bodymediaResponse = JSONObject.fromObject(apiData.json);
        JSONArray daysArray = bodymediaResponse.getJSONArray("days");
        if(bodymediaResponse.has("lastSync"))
        {
            DateTime d = form.parseDateTime(bodymediaResponse.getJSONObject("lastSync").getString("dateTime"));

            // Get timezone map from UpdateInfo context
            TimezoneMap tzMap = (TimezoneMap)updateInfo.getContext("tzMap");

            // Insert lastSync into the updateInfo context so it's accessible to the updater
            updateInfo.setContext("lastSync", d);

            for(Object o : daysArray)
            {
                if(o instanceof JSONObject)
                {
                    JSONObject day = (JSONObject) o;
                    BodymediaStepsFacet steps = new BodymediaStepsFacet(apiData.updateInfo.apiKey.getId());
                    super.extractCommonFacetData(steps, apiData);
                    steps.totalSteps = day.getInt("totalSteps");
                    steps.date = day.getString("date");
                    steps.json = day.getString("hours");
                    steps.lastSync = d.getMillis();

                    DateTime date = formatter.parseDateTime(day.getString("date"));
                    steps.date = TimeUtils.dateFormatter.print(date.getMillis());
                    if(tzMap!=null)
                    {
                        // Create a LocalDate object which just captures the date without any
                        // timezone assumptions
                        LocalDate ld = new LocalDate(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth());
                        // Use tzMap to convert date into a datetime with timezone information
                        DateTime realDateStart = tzMap.getStartOfDate(ld);
                        // Set the start and end times for the facet.  The start time is the leading midnight
                        // of date according to BodyMedia's idea of what timezone you were in then.
                        // Need to figure out what end should be...
                        steps.start = realDateStart.getMillis();
                        int minutesLength = 1440;
                        steps.end = steps.start + DateTimeConstants.MILLIS_PER_MINUTE * minutesLength;
                    }
                    else {
                        TimeZone timeZone = metadataService.getTimeZone(apiData.updateInfo.getGuestId(), date.getMillis());
                        long fromMidnight = TimeUtils.fromMidnight(date.getMillis(), timeZone);
                        long toMidnight = TimeUtils.toMidnight(date.getMillis(), timeZone);
                        steps.start = fromMidnight;
                        steps.end = toMidnight;
                    }
                    facets.add(steps);
                }
                else
                    throw new JSONException("Days array is not a proper JSONObject");
            }
        }
        return facets;
    }
}