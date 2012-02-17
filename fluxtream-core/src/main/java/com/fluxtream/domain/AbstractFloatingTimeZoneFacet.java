package com.fluxtream.domain;

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import javax.persistence.MappedSuperclass;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

@MappedSuperclass
public abstract class AbstractFloatingTimeZoneFacet extends AbstractFacet {

	public String startTimeStorage;
	public String endTimeStorage;
	
	private static DateTimeFormatter format = DateTimeFormat.forPattern(
			"yyyy-MM-dd'T'HH:mm:ss.SSS");
	
	public void updateTimeInfo(TimeZone timeZone) throws ParseException {
		Date startDate = new Date(format.withZone(DateTimeZone.forTimeZone(timeZone)).parseMillis(startTimeStorage));
		Date endDate = new Date(format.withZone(DateTimeZone.forTimeZone(timeZone)).parseMillis(endTimeStorage));
		
		this.start = startDate.getTime();
		this.end = endDate.getTime();
	}
	
}
