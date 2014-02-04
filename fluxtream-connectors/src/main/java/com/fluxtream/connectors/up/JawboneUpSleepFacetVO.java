package com.fluxtream.connectors.up;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.domain.GuestSettings;

/**
 * User: candide
 * Date: 04/02/14
 * Time: 13:50
 */
public class JawboneUpSleepFacetVO extends AbstractFacetVO<JawboneUpSleepFacet> {

    public String title;
    public String date;
    public double place_lat;
    public double place_lon;
    public int place_acc;
    public String place_name;
    public String snapshot_image;
    public long smart_alarm_fire;
    public long awake_time;
    public long asleep_time;
    public int awakenings;
    public int rem;
    public int light;
    public int deep;
    public int awake;
    public int duration;
    public int quality;

    @Override
    protected void fromFacet(final JawboneUpSleepFacet facet, final TimeInterval timeInterval, final GuestSettings settings) throws OutsideTimeBoundariesException {
        this.title = facet.title;
        this.date = facet.date;
        this.place_acc = facet.place_acc;
        this.place_lat = facet.place_lat;
        this.place_lon = facet.place_lon;
        this.place_name = facet.place_name;
        this.snapshot_image = facet.snapshot_image;
        this.smart_alarm_fire = facet.smart_alarm_fire;
        this.awake_time = facet.awake_time;
        this.asleep_time = facet.asleep_time;
        this.awakenings = facet.awakenings;
        this.rem = facet.rem;
        this.light = facet.light;
        this.deep = facet.deep;
        this.awake = facet.awake;
        this.duration = facet.duration;
        this.quality = facet.quality;
    }

}
