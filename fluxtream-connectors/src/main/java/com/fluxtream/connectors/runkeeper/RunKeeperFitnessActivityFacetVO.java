package com.fluxtream.connectors.runkeeper;

import java.util.Date;
import java.util.TimeZone;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.models.DurationModel;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class RunKeeperFitnessActivityFacetVO extends AbstractTimedFacetVO<RunKeeperFitnessActivityFacet> {

    double averageHeartRate = 0d;
    public double total_distance;
    public double total_climb;
    public String activityType;

    @Override
    protected void fromFacet(final RunKeeperFitnessActivityFacet facet, final TimeInterval timeInterval, final GuestSettings settings) {
        double totalMeasures = 0d;
        int nMeasures = 0;
        for (HeartRateMeasure measure : facet.heart_rate) {
            nMeasures++;
            totalMeasures += measure.heartRate;
        }
        TimeZone timeZone = TimeZone.getTimeZone(facet.timeZone);
        this.startMinute = toMinuteOfDay(new Date(facet.start), timeZone);
        this.endMinute = toMinuteOfDay(new Date(facet.end), timeZone);
        averageHeartRate = round(totalMeasures/nMeasures);
        this.total_distance = facet.total_distance;
        this.duration = new DurationModel(facet.duration);
        this.total_climb = facet.total_climb;
        this.activityType = facet.type;
    }

    double round(double v) {
        return (double) Math.round(v * 100) / 100;
    }

}
