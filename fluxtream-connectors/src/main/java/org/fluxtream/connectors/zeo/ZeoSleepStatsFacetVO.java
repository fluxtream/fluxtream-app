package org.fluxtream.connectors.zeo;

import java.util.Calendar;
import java.util.TimeZone;
import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.vos.AbstractLocalTimeTimedFacetVO;
import org.fluxtream.domain.GuestSettings;
import org.fluxtream.mvc.models.DurationModel;
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
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(facet.start);
        startMinute = c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE);
        c.setTimeInMillis(facet.end);
        endMinute = c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE);
		zq = facet.zq;
		morningFeel = facet.morningFeel;
		sleepGraph = facet.sleepGraph;
        s = zeoTimeFormat.withZone(DateTimeZone.forTimeZone(timeInterval.getMainTimeZone())).print(facet.start);
        e = zeoTimeFormat.withZone(DateTimeZone.forTimeZone(timeInterval.getMainTimeZone())).print(facet.end);
    }

}
