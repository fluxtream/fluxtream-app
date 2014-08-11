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
public class BodymediaSleepFacetExtractor extends AbstractFacetExtractor
{

    //Logs various transactions
    FlxLogger logger = FlxLogger.getLogger(BodymediaSleepFacetExtractor.class);

    @Qualifier("connectorUpdateServiceImpl")
    @Autowired
    ConnectorUpdateService connectorUpdateService;

    DateTimeFormatter form = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZ");
    DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd");

    @Qualifier("metadataServiceImpl")
    @Autowired
    MetadataService metadataService;

    @Override
    public List<AbstractFacet> extractFacets(final UpdateInfo updateInfo,
                                             final ApiData apiData,
                                             final ObjectType objectType) throws Exception
    {

        logger.info("guestId=" + updateInfo.getGuestId() +
        				" connector=bodymedia action=extractFacets objectType="
        						+ objectType.getName());

        ArrayList<AbstractFacet> facets;
        String name = objectType.getName();
        if(name.equals("sleep"))
        {
            facets = extractSleepFacets(updateInfo, apiData);
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
     * @return a list containing a single BodymediaSleepFacet for the current day
     */
    private ArrayList<AbstractFacet> extractSleepFacets(final UpdateInfo updateInfo,
							final ApiData apiData)
    {
        ArrayList<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        /* burnJson is a JSONArray that contains a seperate JSONArray and calorie counts for each day
         */
        JSONObject bodymediaResponse = JSONObject.fromObject(apiData.json);
        if(bodymediaResponse.has("Failed"))
        {
            BodymediaSleepFacet sleep = new BodymediaSleepFacet(updateInfo.apiKey.getId());
            sleep.date = bodymediaResponse.getString("Date");
        }
        else
        {
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
                        BodymediaSleepFacet sleep = new BodymediaSleepFacet(updateInfo.apiKey.getId());
                        super.extractCommonFacetData(sleep, apiData);
                        sleep.efficiency = day.getDouble("efficiency");
                        sleep.totalLying = day.getInt("totalLying");
                        sleep.totalSleeping = day.getInt("totalSleep");
                        sleep.json = day.getString("sleepPeriods");
                        sleep.lastSync = d.getMillis();

                        //https://developer.bodymedia.com/docs/read/api_reference_v2/Sleep_Service
                        //  sleep data is from noon the previous day to noon the current day,
                        //  so subtract MILLIS_IN_DAY/2 from midnight

                        long MILLIS_IN_DAY = 86400000l;
                        DateTime date = formatter.parseDateTime(day.getString("date"));

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
                            sleep.date = TimeUtils.dateFormatter.print(realDateStart.getMillis());
                            sleep.start = realDateStart.getMillis() - DateTimeConstants.MILLIS_PER_DAY/2;
                            sleep.end = realDateStart.getMillis() + DateTimeConstants.MILLIS_PER_DAY/2;
                        }
                        else {
                            sleep.date = TimeUtils.dateFormatter.print(date.getMillis());
                            TimeZone timeZone = metadataService.getTimeZone(updateInfo.getGuestId(), date.getMillis());
                            long fromNoon = TimeUtils.fromMidnight(date.getMillis(), timeZone) - MILLIS_IN_DAY / 2;
                            long toNoon = TimeUtils.toMidnight(date.getMillis(), timeZone) - MILLIS_IN_DAY / 2;
                            sleep.start = fromNoon;
                            sleep.end = toNoon;
                        }
                        facets.add(sleep);
                    }
                    else
                        throw new JSONException("Days array is not a proper JSONObject");
                }
            }
        }
        return facets;
    }

}
