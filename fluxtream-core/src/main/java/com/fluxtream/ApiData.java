package com.fluxtream;

import java.util.TimeZone;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.AbstractEntity;
import com.fluxtream.utils.TimeUtils;
import net.sf.json.JSONObject;
import org.dom4j.Document;
import org.joda.time.DateTimeZone;

public class ApiData extends AbstractEntity {

	public String xml;
	public String json;
	public long start, end;
	public UpdateInfo updateInfo;

	public JSONObject jsonObject;
	public Document xmlDocument;
	
	public ApiData(UpdateInfo updateInfo, long start, long end) {
		this.updateInfo = updateInfo;
		this.start = start;
		this.end = end;
	}
	
	public String getDate(TimeZone timeZone) {
		return TimeUtils.dateFormatter.withZone(DateTimeZone.forTimeZone(timeZone)).print(start);
	}
	
}
