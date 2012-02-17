package com.fluxtream.connectors.zeo;

import java.util.Date;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.domain.GuestSettings;

public class ZeoSleepStatsFacetVO extends AbstractTimedFacetVO<ZeoSleepStatsFacet> {
	
	public int minutesAsleep;
	public int minutesAwake;
	public int minutesToFallAsleep;
	public int zq;
	public int morningFeel;
	public Date riseTime;
	public Date bedTime;
	public String sleepGraph;

	@Override
	public void fromFacet(ZeoSleepStatsFacet facet, TimeInterval timeInterval, GuestSettings settings) {
		startMinute = toMinuteOfDay(facet.bedTime, timeInterval.timeZone);
		endMinute = toMinuteOfDay(facet.riseTime, timeInterval.timeZone);
		minutesAsleep = facet.totalZ;
		minutesAwake = (int) ((double)minutesAsleep/100d*(double)facet.timeInWakePercentage);
		minutesToFallAsleep = facet.timeToZ;
		riseTime = facet.riseTime;
		bedTime = facet.bedTime;
		zq = facet.zq;
		morningFeel = facet.morningFeel;
		sleepGraph = facet.sleepGraph;
	}

}
