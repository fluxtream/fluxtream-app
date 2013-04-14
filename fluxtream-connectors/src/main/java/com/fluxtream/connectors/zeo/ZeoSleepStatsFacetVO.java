package com.fluxtream.connectors.zeo;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractLocalTimeTimedFacetVO;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.models.DurationModel;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class ZeoSleepStatsFacetVO extends AbstractLocalTimeTimedFacetVO<ZeoSleepStatsFacet> {
	
	public DurationModel minutesAsleep;
	public DurationModel minutesAwake;
	public DurationModel minutesToFallAsleep;
	public int zq;
	public int morningFeel;
	public LocalDateTime riseTime;
	public LocalDateTime bedTime;
	public String sleepGraph;
    public String s, e;
    DateTimeFormatter zeoTimeFormat = DateTimeFormat.forPattern("yyyyMMddHHmm");

    @Override
	public void fromFacet(ZeoSleepStatsFacet facet, TimeInterval timeInterval, GuestSettings settings) {
		minutesAsleep = new DurationModel(facet.totalZ*60);
		minutesAwake = new DurationModel((int) ((double)facet.totalZ*60d/100d*(double)facet.timeInWakePercentage));
		minutesToFallAsleep = new DurationModel(facet.timeToZ*60);
		riseTime = new LocalDateTime(facet.end);
		bedTime = new LocalDateTime(facet.start);
		zq = facet.zq;
		morningFeel = facet.morningFeel;
		sleepGraph = facet.sleepGraph;
        startMinute = bedTime.getHourOfDay()*60+bedTime.getMinuteOfHour();
        endMinute = riseTime.getHourOfDay()*60+riseTime.getMinuteOfHour();
        s = zeoTimeFormat.withZone(DateTimeZone.forTimeZone(timeInterval.timeZone)).print(facet.start);
        e = zeoTimeFormat.withZone(DateTimeZone.forTimeZone(timeInterval.timeZone)).print(facet.end);
    }

}
