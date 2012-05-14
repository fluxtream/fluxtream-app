package com.fluxtream.thirdparty.helpers;

import static com.fluxtream.utils.HttpUtils.fetch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fluxtream.Configuration;
import com.fluxtream.domain.metadata.WeatherInfo;

@Component
public class WWOHelper {

	@Autowired
	private Configuration env;

	public List<WeatherInfo> getWeatherInfo(double latitude, double longitude, String fdate) throws HttpException, IOException {
		String wwoUrl = "http://www.worldweatheronline.com/feed/premium-weather-v2.ashx?" +
"key=" + env.get("wwo.key") + "&feedkey=" + env.get("wwo.feedkey") + "&format=json&q=" + latitude + "," + longitude + "&date=" + fdate;
		String wwoJson = fetch(wwoUrl, env);
		
		JSONObject wwoInfo = JSONObject.fromObject(wwoJson);
		List<WeatherInfo> weather = new ArrayList<WeatherInfo>();
		if (wwoInfo!=null) {
			JSONObject data = wwoInfo.getJSONObject("data");
			if (data==null) return weather;
			JSONArray weatherDataArray = data.getJSONArray("weather");
			if (weatherDataArray==null) return weather;
			JSONObject weatherData = weatherDataArray.getJSONObject(0);
			if (weatherData==null) return weather;
			JSONArray hourly = weatherData.getJSONArray("hourly");
			if (hourly!=null) {
				@SuppressWarnings("rawtypes")
				Iterator iterator = hourly.iterator();
				while (iterator.hasNext()) {
					JSONObject hourlyRecord = (JSONObject) iterator.next();
					WeatherInfo weatherInfo = new WeatherInfo();
					weatherInfo.cloudcover = Integer.valueOf(hourlyRecord.getString("cloudcover"));
					weatherInfo.humidity = Integer.valueOf(hourlyRecord.getString("humidity"));
					weatherInfo.precipMM = Float.valueOf(hourlyRecord.getString("precipMM"));
					weatherInfo.pressure = Integer.valueOf(hourlyRecord.getString("pressure"));
					weatherInfo.tempC = Integer.valueOf(hourlyRecord.getString("tempC"));
					weatherInfo.tempF = Integer.valueOf(hourlyRecord.getString("tempF"));
					weatherInfo.minuteOfDay = Integer.valueOf(hourlyRecord.getString("time"));
					
					weatherInfo.visibility = Integer.valueOf(hourlyRecord.getString("visibility"));
					weatherInfo.weatherCode = Integer.valueOf(hourlyRecord.getString("weatherCode"));
					JSONArray weatherDesc = hourlyRecord.getJSONArray("weatherDesc");

					weatherInfo.weatherDesc = weatherDesc.getJSONObject(0).getString("value");
                    weatherInfo.weatherIconUrl = null;
                    weatherInfo.weatherIconUrlDay = null;
                    weatherInfo.weatherIconUrlNight = null;
					weatherInfo.winddirDegree = Integer.valueOf(hourlyRecord.getString("winddirDegree"));
					weatherInfo.windspeedMiles = Integer.valueOf(hourlyRecord.getString("windspeedMiles"));
					
					weather.add(weatherInfo);
				}
			}
		}
		return weather;
	}
}
