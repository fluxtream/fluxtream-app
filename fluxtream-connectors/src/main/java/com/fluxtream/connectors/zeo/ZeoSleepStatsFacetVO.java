package com.fluxtream.connectors.zeo;

import java.util.Date;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.models.DurationModel;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class ZeoSleepStatsFacetVO extends AbstractTimedFacetVO<ZeoSleepStatsFacet> {
	
	public DurationModel minutesAsleep;
	public DurationModel minutesAwake;
	public DurationModel minutesToFallAsleep;
	public int zq;
	public int morningFeel;
	public Date riseTime;
	public Date bedTime;
	public String sleepGraph;
    public String s, e;
    DateTimeFormatter zeoTimeFormat = DateTimeFormat.forPattern("yyyyMMddHHmm");

    @Override
	public void fromFacet(ZeoSleepStatsFacet facet, TimeInterval timeInterval, GuestSettings settings) {
		startMinute = toMinuteOfDay(new Date(facet.start), timeInterval.timeZone);
		endMinute = toMinuteOfDay(new Date(facet.end), timeInterval.timeZone);
		minutesAsleep = new DurationModel(facet.totalZ*60);
		minutesAwake = new DurationModel((int) ((double)facet.totalZ*60d/100d*(double)facet.timeInWakePercentage));
		minutesToFallAsleep = new DurationModel(facet.timeToZ*60);
		riseTime = new Date(facet.end);
		bedTime = new Date(facet.start);
		zq = facet.zq;
		morningFeel = facet.morningFeel;
		sleepGraph = facet.sleepGraph;
        s = zeoTimeFormat.withZone(DateTimeZone.forTimeZone(timeInterval.timeZone)).print(bedTime.getTime());
        e = zeoTimeFormat.withZone(DateTimeZone.forTimeZone(timeInterval.timeZone)).print(riseTime.getTime());
    }

}
