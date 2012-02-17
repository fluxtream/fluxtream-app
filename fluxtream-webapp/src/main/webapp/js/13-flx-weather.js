function getWeather() {
	$.ajax({ url: "/me/weatherInfo.json?timeHash="+FlxState.timeHash, dataType: "json",
		success: function(weatherInfo) {
			if (outsideTimeBoundaries(weatherInfo)) return;
			if (typeof(weatherInfo.hourly)!="undefined"&&weatherInfo.hourly!=null)
				FlxState.weather = weatherInfo;
		}
	});
}

function showEventWeather(startMinute) {
	if (typeof(FlxState.weather)=="undefined"||FlxState.weather==null) return;
	var lastHourly = FlxState.weather.hourly[0];
	for (i=0; i<FlxState.weather.hourly.length; i++) {
		hourly = FlxState.weather.hourly[i];
		if (hourly.minuteOfDay>startMinute)
			break;
		lastHourly = hourly;
	}
	var humidity = lastHourly.humidity;
	var precipitation = lastHourly.precipMM;
	var tempC = lastHourly.tempC;
	var weatherDesc = lastHourly.weatherDesc;
	var weatherIconUrl = lastHourly.weatherIconUrl;
	var windspeedKmph = lastHourly.windspeedKmph;
	$("#weatherPanel").show();
	$("#weatherPrecipitation").html(precipitation + " mm");
	$("#weatherHumidity").html(humidity + " %");
	$("#weatherDesc").html(weatherDesc);
	$("#weatherTemp").html(tempC + " &deg;C");
	$("#weatherIcon")[0].src = weatherIconUrl;
	$("#windSpeed").html(windspeedKmph + " km/h");
}