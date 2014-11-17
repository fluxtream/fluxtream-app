package org.fluxtream.connectors.fitbit;

import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.services.impl.FieldHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component("fitbitFoodLogSummary")
public class FitbitFoodLogSummaryFieldHandler implements FieldHandler {

    private static final String DATASET_KEY = "dataset";
    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public List<BodyTrackHelper.BodyTrackUploadResult> handleField ( final long guestId, AbstractFacet facet) {

        List<BodyTrackHelper.BodyTrackUploadResult> results = new ArrayList<BodyTrackHelper.BodyTrackUploadResult>();

        FitbitFoodLogSummaryFacet fitbitFoodLogSummaryFacet = (FitbitFoodLogSummaryFacet) facet;

        // The Fitbit activity data is daily data that covers an entire day.  The start/end time may be the
        // leading and trailing midnights for the date, or may both be at noon on the date, depending
        // on the version that did the import.  Either way they should now both be in UTC since
        // it is a local time facet.  Datastore points only have a single time associated with them.
        // Set this time to be the middle value between start and end, which should be noon UTC in
        // either case.  Also convert to double seconds since that is what the datastore uses.
        double dsTime = (double)(fitbitFoodLogSummaryFacet.start + fitbitFoodLogSummaryFacet.end)/2000.0;
        List<String> channelNames = new ArrayList<String>();
        List<List<Object>> data = new ArrayList<List<Object>>();
        List<Object> record = new ArrayList<Object>();

        // Add the timestamp to the start of the record
        record.add(dsTime);

        // Add each non-empty field to both the channelNames and data record so they correspond
        if (fitbitFoodLogSummaryFacet.calories > 0) {
            channelNames.add("caloriesIn");
            record.add(fitbitFoodLogSummaryFacet.calories);
        }
        if (fitbitFoodLogSummaryFacet.water > 0) {
            channelNames.add("water");
            record.add(fitbitFoodLogSummaryFacet.water);
        }
        if (fitbitFoodLogSummaryFacet.caloriesGoal > 0) {
            channelNames.add("caloriesInGoal");
            record.add(fitbitFoodLogSummaryFacet.caloriesGoal);
        }
        if (fitbitFoodLogSummaryFacet.caloriesOutGoal > 0) {
            channelNames.add("caloriesOutGoal");
            record.add(fitbitFoodLogSummaryFacet.caloriesOutGoal);
        }

        data.add(record);

        results.add(bodyTrackHelper.uploadToBodyTrack(guestId, "Fitbit", channelNames, data));

        // TODO: check the status code in the BodyTrackUploadResult
        return results;
    }

}