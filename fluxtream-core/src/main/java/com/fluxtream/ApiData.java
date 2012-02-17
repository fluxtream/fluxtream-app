package com.fluxtream;

import java.util.TimeZone;

import net.sf.json.JSONObject;

import org.dom4j.Document;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.AbstractEntity;

public class ApiData extends AbstractEntity {

	public String xml;
	public String json;
	public long start, end;
	public UpdateInfo updateInfo;

	public JSONObject jsonObject;
	public Document xmlDocument;
	
	private static final DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");

	public ApiData(UpdateInfo updateInfo, long start, long end) {
		this.updateInfo = updateInfo;
		this.start = start;
		this.end = end;
	}
	
	public String getDate(TimeZone timeZone) {
		return format.withZone(DateTimeZone.forTimeZone(timeZone)).print(start);
	}
	
}
