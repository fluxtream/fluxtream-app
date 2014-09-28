package org.fluxtream.connectors.fitbit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.services.impl.FieldHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component("fitbitActivity")
public class FitbitActivityFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public List<BodyTrackHelper.BodyTrackUploadResult> handleField ( final long guestId, AbstractFacet facet) {
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
        if (fitbitActivityFacet.activeScore >= 0) {
            channelNames.add("activeScore");
            record.add(fitbitActivityFacet.activeScore);
        }
        if (fitbitActivityFacet.floors >= 0) {
            channelNames.add("floors");
            record.add(fitbitActivityFacet.floors);
        }
        if (fitbitActivityFacet.elevation >= 0) {
            channelNames.add("elevation");
            record.add(fitbitActivityFacet.elevation);
        }
		if (fitbitActivityFacet.caloriesOut >= 0) {
            channelNames.add("caloriesOut");
            record.add(fitbitActivityFacet.caloriesOut);
        }
		if (fitbitActivityFacet.fairlyActiveMinutes >= 0) {
            channelNames.add("fairlyActiveMinutes");
            record.add(fitbitActivityFacet.fairlyActiveMinutes);
        }
		if (fitbitActivityFacet.lightlyActiveMinutes >= 0) {
            channelNames.add("lightlyActiveMinutes");
            record.add(fitbitActivityFacet.lightlyActiveMinutes);
        }
		if (fitbitActivityFacet.sedentaryMinutes >= 0) {
            channelNames.add("sedentaryMinutes");
            record.add(fitbitActivityFacet.sedentaryMinutes);
        }
		if (fitbitActivityFacet.veryActiveMinutes >= 0) {
            channelNames.add("veryActiveMinutes");
            record.add(fitbitActivityFacet.veryActiveMinutes);
        }
		if (fitbitActivityFacet.steps >= 0) {
            channelNames.add("steps");
            record.add(fitbitActivityFacet.steps);
        }
        if (fitbitActivityFacet.trackerDistance >= 0) {
            channelNames.add("trackerDistance");
            record.add(fitbitActivityFacet.trackerDistance);
        }
        if (fitbitActivityFacet.loggedActivitiesDistance >= 0) {
            channelNames.add("loggedActivitiesDistance");
            record.add(fitbitActivityFacet.loggedActivitiesDistance);
        }
        if (fitbitActivityFacet.veryActiveDistance >= 0) {
            channelNames.add("veryActiveDistance");
            record.add(fitbitActivityFacet.veryActiveDistance);
        }
        if (fitbitActivityFacet.totalDistance >= 0) {
            channelNames.add("totalDistance");
            record.add(fitbitActivityFacet.totalDistance);
        }
        if (fitbitActivityFacet.moderatelyActiveDistance >= 0) {
            channelNames.add("moderatelyActiveDistance");
            record.add(fitbitActivityFacet.moderatelyActiveDistance);
        }
        if (fitbitActivityFacet.lightlyActiveDistance >= 0) {
            channelNames.add("lightlyActiveDistance");
            record.add(fitbitActivityFacet.lightlyActiveDistance);
        }
        if (fitbitActivityFacet.sedentaryActiveDistance >= 0) {
            channelNames.add("sedentaryActiveDistance");
            record.add(fitbitActivityFacet.sedentaryActiveDistance);
        }

        data.add(record);

        // TODO: check the status code in the BodyTrackUploadResult
        return Arrays.asList(bodyTrackHelper.uploadToBodyTrack(guestId, "Fitbit", channelNames, data));
    }

}