package com.fluxtream.connectors.zeo;

import java.util.TimeZone;
import com.fluxtream.Configuration;
import com.fluxtream.utils.HttpUtils;
import com.fluxtream.utils.TimeUtils;
import net.sf.json.JSONObject;
import org.joda.time.DateTimeZone;

class ZeoHelper {

	public boolean testConnection(Configuration env, TimeZone tz,
			String username, String password) {
		String today = TimeUtils.dateFormatter.withZone(DateTimeZone.forTimeZone(tz)).print(System.currentTimeMillis());
		String apiKey = env.get("zeoApiKey");
		String url = "https://api.myzeo.com:8443/zeows/api/v1/json/sleeperService/getLatestSleepRecord?key="
				+ apiKey + "&date=" + today;
		try {
			String s = HttpUtils.fetch(url, username, password);
			JSONObject json = JSONObject.fromObject(s);
			JSONObject response = json.getJSONObject("response");
			String name = response.getString("name");
			return name.equals("getLatestSleepRecord");
		} catch (Exception e) {
			return false;
		}
	}

}
