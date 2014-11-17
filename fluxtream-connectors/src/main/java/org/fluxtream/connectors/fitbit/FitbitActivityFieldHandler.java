package org.fluxtream.connectors.fitbit;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.services.impl.FieldHandler;
import org.joda.time.LocalTime;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component("fitbitActivity")
public class FitbitActivityFieldHandler implements FieldHandler {

    private static final String DATASET_KEY = "dataset";
    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public List<BodyTrackHelper.BodyTrackUploadResult> handleField ( final long guestId, AbstractFacet facet) {

        List<BodyTrackHelper.BodyTrackUploadResult> results = new ArrayList<BodyTrackHelper.BodyTrackUploadResult>();

        FitbitTrackerActivityFacet fitbitActivityFacet = (FitbitTrackerActivityFacet) facet;

        // The Fitbit activity data is daily data that covers an entire day.  The start/end time may be the
        // leading and trailing midnights for the date, or may both be at noon on the date, depending
        // on the version that did the import.  Either way they should now both be in UTC since
        // it is a local time facet.  Datastore points only have a single time associated with them.
        // Set this time to be the middle value between start and end, which should be noon UTC in
        // either case.  Also convert to double seconds since that is what the datastore uses.
        double dsTime = (double)(fitbitActivityFacet.start + fitbitActivityFacet.end)/2000.0;
        List<String> channelNames = new ArrayList<String>();
        List<List<Object>> data = new ArrayList<List<Object>>();
        List<Object> record = new ArrayList<Object>();

        // Add the timestamp to the start of the record
        record.add(dsTime);

        // Add each non-empty field to both the channelNames and data record so they correspond
        if (fitbitActivityFacet.activeScore > 0) {
            channelNames.add("activeScore");
            record.add(fitbitActivityFacet.activeScore);
        }
        if (fitbitActivityFacet.floors > 0) {
            channelNames.add("floors");
            record.add(fitbitActivityFacet.floors);
        }
        if (fitbitActivityFacet.elevation > 0) {
            channelNames.add("elevation");
            record.add(fitbitActivityFacet.elevation);
        }
		if (fitbitActivityFacet.caloriesOut > 0) {
            channelNames.add("caloriesOut");
            record.add(fitbitActivityFacet.caloriesOut);
        }
		if (fitbitActivityFacet.fairlyActiveMinutes > 0) {
            channelNames.add("fairlyActiveMinutes");
            record.add(fitbitActivityFacet.fairlyActiveMinutes);
        }
		if (fitbitActivityFacet.lightlyActiveMinutes > 0) {
            channelNames.add("lightlyActiveMinutes");
            record.add(fitbitActivityFacet.lightlyActiveMinutes);
        }
		if (fitbitActivityFacet.sedentaryMinutes > 0) {
            channelNames.add("sedentaryMinutes");
            record.add(fitbitActivityFacet.sedentaryMinutes);
        }
		if (fitbitActivityFacet.veryActiveMinutes > 0) {
            channelNames.add("veryActiveMinutes");
            record.add(fitbitActivityFacet.veryActiveMinutes);
        }
		if (fitbitActivityFacet.steps > 0) {
            channelNames.add("steps");
            record.add(fitbitActivityFacet.steps);
        }
        if (fitbitActivityFacet.trackerDistance > 0) {
            channelNames.add("trackerDistance");
            record.add(fitbitActivityFacet.trackerDistance);
        }
        if (fitbitActivityFacet.loggedActivitiesDistance > 0) {
            channelNames.add("loggedActivitiesDistance");
            record.add(fitbitActivityFacet.loggedActivitiesDistance);
        }
        if (fitbitActivityFacet.veryActiveDistance > 0) {
            channelNames.add("veryActiveDistance");
            record.add(fitbitActivityFacet.veryActiveDistance);
        }
        if (fitbitActivityFacet.totalDistance > 0) {
            channelNames.add("totalDistance");
            record.add(fitbitActivityFacet.totalDistance);
        }
        if (fitbitActivityFacet.moderatelyActiveDistance > 0) {
            channelNames.add("moderatelyActiveDistance");
            record.add(fitbitActivityFacet.moderatelyActiveDistance);
        }
        if (fitbitActivityFacet.lightlyActiveDistance > 0) {
            channelNames.add("lightlyActiveDistance");
            record.add(fitbitActivityFacet.lightlyActiveDistance);
        }
        if (fitbitActivityFacet.sedentaryActiveDistance > 0) {
            channelNames.add("sedentaryActiveDistance");
            record.add(fitbitActivityFacet.sedentaryActiveDistance);
        }

        data.add(record);

        results.add(bodyTrackHelper.uploadToBodyTrack(guestId, "Fitbit", channelNames, data));

        List<String> metrics = Arrays.asList("steps", "calories", "distance", "floors", "elevation");

        for (String metric : metrics) {
            try {
                Field jsonField = FitbitTrackerActivityFacet.class.getField(metric + "Json");
                if (jsonField.get(fitbitActivityFacet)!=null) {
                    final BodyTrackHelper.BodyTrackUploadResult bodyTrackUploadResult = addIntradayData(guestId, fitbitActivityFacet, jsonField, metric);
                    if (bodyTrackUploadResult!=null)
                        results.add(bodyTrackUploadResult);
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                continue;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                continue;
            }
        }

        // TODO: check the status code in the BodyTrackUploadResult
        return results;
    }

    private BodyTrackHelper.BodyTrackUploadResult addIntradayData(final long guestId,
                                                                  FitbitTrackerActivityFacet fitbitActivityFacet,
                                                                  final Field field,
                                                                  final String metric) throws IllegalAccessException {
        List<List<Object>> data = new ArrayList<List<Object>>();

        long midnight = ISODateTimeFormat.date().withZoneUTC().parseDateTime(fitbitActivityFacet.date).toDateMidnight().getMillis();
        JSONObject intradayJson = JSONObject.fromObject(field.get(fitbitActivityFacet));

        final String intradayMetricKey = "activities-log-" + metric + "-intraday";

        if (intradayJson.has(intradayMetricKey)) {
            JSONObject intradayDataJson = intradayJson.getJSONObject(intradayMetricKey);
            if (intradayDataJson.has(DATASET_KEY)) {
                JSONArray intradayDataArray = intradayDataJson.getJSONArray(DATASET_KEY);
                for (int i=0; i<intradayDataArray.size(); i++) {
                    JSONObject intradayDataRecord = intradayDataArray.getJSONObject(i);
                    String time = intradayDataRecord.getString("time") + "Z";
                    final LocalTime localTime = ISODateTimeFormat.timeNoMillis().parseLocalTime(time);
                    final long timeGmt = (midnight + localTime.getMillisOfDay())/1000;
                    int value = intradayDataRecord.getInt("value");
                    List<Object> record = new ArrayList<Object>();
                    record.add(timeGmt);
                    record.add(value);
                    if (metric.equals("calories")) {
                        int level = intradayDataRecord.getInt("level");
                        record.add(level);
                        int mets = intradayDataRecord.getInt("mets");
                        record.add(mets);
                    }
                    data.add(record);
                }
            }
            final List<String> channelNames = !metric.equals("calories")
                                            ? Arrays.asList(metric + "Intraday")
                                            : Arrays.asList(metric + "Intraday", "levelsIntraday", "metsIntraday");

            return bodyTrackHelper.uploadToBodyTrack(guestId, "Fitbit", channelNames, data);
        }
        return null;
    }

}