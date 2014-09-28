package org.fluxtream.connectors.zeo;

import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractLocalTimeTimedFacetVO;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.DurationModel;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class ZeoSleepStatsFacetVO extends AbstractLocalTimeTimedFacetVO<ZeoSleepStatsFacet> {
	
	public DurationModel minutesAsleep;
	public DurationModel minutesAwake;
	public DurationModel minutesToFallAsleep;
	public int zq;
	public int morningFeel;
	public String sleepGraph;
    public String s, e;
    transient DateTimeFormatter zeoTimeFormat = DateTimeFormat.forPattern("yyyyMMddHHmm");

    @Override
	public void fromFacet(ZeoSleepStatsFacet facet, TimeInterval timeInterval, GuestSettings settings) {
        date = facet.date;
		minutesAsleep = new DurationModel(facet.totalZ*60);
		minutesAwake = new DurationModel((int) ((double)facet.totalZ*60d/100d*(double)facet.timeInWakePercentage));
		minutesToFallAsleep = new DurationModel(facet.timeToZ*60);
		zq = facet.zq;
		morningFeel = facet.morningFeel;
		sleepGraph = facet.sleepGraph;
        s = zeoTimeFormat.withZone(DateTimeZone.forTimeZone(timeInterval.getMainTimeZone())).print(facet.start);
        e = zeoTimeFormat.withZone(DateTimeZone.forTimeZone(timeInterval.getMainTimeZone())).print(facet.end);
    }

}
