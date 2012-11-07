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

    public String date;
	public String startTimeStorage;
	public String endTimeStorage;
	
	public static DateTimeFormatter timeStorageFormat = DateTimeFormat.forPattern(
			"yyyy-MM-dd'T'HH:mm:ss.SSS");

	public void updateTimeInfo(TimeZone timeZone) throws ParseException {
		Date startDate = new Date(timeStorageFormat.withZone(DateTimeZone.forTimeZone(timeZone)).parseMillis(startTimeStorage));
		Date endDate = new Date(timeStorageFormat.withZone(DateTimeZone.forTimeZone(timeZone)).parseMillis(endTimeStorage));
		
		this.start = startDate.getTime();
		this.end = endDate.getTime();
	}

    public static void main (String[] args) {
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Brussels");
        String startTimeStorage = "2012-8-29T23:34:4.000";
        String endTimeStorage = "2012-8-30T9:23:52.000";
        Date startDate = new Date(timeStorageFormat.withZone(DateTimeZone.forTimeZone(timeZone)).parseMillis(startTimeStorage));
        Date endDate = new Date(timeStorageFormat.withZone(DateTimeZone.forTimeZone(timeZone)).parseMillis(endTimeStorage));
        System.out.println(startDate);
        System.out.println(endDate);
    }

}


