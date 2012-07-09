package com.fluxtream.connectors.zeo;

import java.util.Date;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.models.DurationModel;

public class ZeoSleepStatsFacetVO extends AbstractTimedFacetVO<ZeoSleepStatsFacet> {
	
	public DurationModel minutesAsleep;
	public DurationModel minutesAwake;
	public DurationModel minutesToFallAsleep;
	public int zq;
	public int morningFeel;
	public Date riseTime;
	public Date bedTime;
	public String sleepGraph;

	@Override
	public void fromFacet(ZeoSleepStatsFacet facet, TimeInterval timeInterval, GuestSettings settings) {
		startMinute = toMinuteOfDay(facet.bedTime, timeInterval.timeZone);
		endMinute = toMinuteOfDay(facet.riseTime, timeInterval.timeZone);
		minutesAsleep = new DurationModel(facet.totalZ);
		minutesAwake = new DurationModel((int) ((double)facet.totalZ/100d*(double)facet.timeInWakePercentage));
		minutesToFallAsleep = new DurationModel(facet.timeToZ);
		riseTime = facet.riseTime;
		bedTime = facet.bedTime;
		zq = facet.zq;
		morningFeel = facet.morningFeel;
		sleepGraph = facet.sleepGraph;
	}

}
