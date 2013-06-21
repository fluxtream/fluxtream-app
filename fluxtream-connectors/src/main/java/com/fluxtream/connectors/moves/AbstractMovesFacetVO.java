package com.fluxtream.connectors.moves;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.models.DurationModel;

/**
 * User: candide
 * Date: 20/06/13
 * Time: 17:37
 */
public abstract class AbstractMovesFacetVO<T extends MovesFacet> extends AbstractTimedFacetVO<T> {

    List<MovesActivityVO> activities = new ArrayList<MovesActivityVO>();

    protected void fromFacetBase(final MovesFacet facet, final TimeInterval timeInterval, final GuestSettings settings) throws OutsideTimeBoundariesException {
        TimeZone timeZone = timeInterval.getTimeZone(facet.start);
        this.startMinute = toMinuteOfDay(new Date(facet.start), timeZone);
        this.endMinute = toMinuteOfDay(new Date(facet.end), timeZone);
        this.duration = new DurationModel((int)(facet.end-facet.start)/1000);
        for (MovesActivity activity : facet.getActivities())
            activities.add(new MovesActivityVO(activity, timeZone));
    }

}
