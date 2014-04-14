package org.fluxtream.connectors.up;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractFacetVO;
import org.fluxtream.core.domain.GuestSettings;

/**
 * User: candide
 * Date: 04/02/14
 * Time: 13:43
 */
public class JawboneUpMovesFacetVO extends AbstractFacetVO<JawboneUpMovesFacet> {

    public String title;
    public String date;
    public String snapshot_image;
    public int distance;
    public double km;
    public int steps;
    public int active_time;
    public int inactive_time;
    public int longest_active;
    public int longest_idle;
    public double calories;
    public double bmr_day;
    public double bmr;
    public double bg_calories;
    public double wo_calories;
    public int wo_time;
    public int wo_active_time;
    public int wo_count;
    public int wo_longest;

    @Override
    protected void fromFacet(final JawboneUpMovesFacet facet, final TimeInterval timeInterval, final GuestSettings settings) throws OutsideTimeBoundariesException {
        this.title = facet.title;
        this.date = facet.date;
        this.snapshot_image = JawboneUpVOHelper.getImageURL(facet.snapshot_image, facet, settings.config);
        this.distance = facet.distance;
        this.km = facet.km;
        this.steps = facet.steps;
        this.active_time = facet.active_time;
        this.inactive_time = facet.inactive_time;
        this.longest_active = facet.longest_active;
        this.longest_idle = facet.longest_idle;
        this.calories = round(facet.calories, 2);
        this.bmr_day = facet.bmr_day;
        this.bmr = facet.bmr;
        this.bg_calories = facet.bg_calories;
        this.wo_calories = facet.wo_calories;
        this.wo_time = facet.wo_time;
        this.wo_active_time = facet.wo_active_time;
        this.wo_count = facet.wo_count;
        this.wo_longest = facet.wo_longest;
    }

}
