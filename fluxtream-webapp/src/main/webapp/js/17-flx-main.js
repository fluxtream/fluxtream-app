
function keepSessionAlive() {
    $.post("/ping");
    window.setTimeout("keepSessionAlive()", 10*60000);
}

function getFacets(page) {

	if (typeof(_gaq)!="undefined") _gaq.push(['_trackEvent', 'listview', FlxState.state, ""+page]);
	var checkedConnectors = getCheckedConnectors();
	
	$.ajax({ url: "/me/digest/"+page,
		data: checkedConnectors,
		dataType: "html",
		success: function(html) {
			$("#facetsList").html(html);
		}
	});
}

function getCheckedConnectors() {

	var connectors = $(".btnList"),
		uncheckedConnectorNames = "all",
		checkedConnectorNames = "all";

	if (connectors!=null&&connectors.length>0) {
		uncheckedConnectorNames = "";
		checkedConnectorNames = "";
		$.each(connectors, function(index) {
			var connectorName = connectors[index].getAttribute("value");
			if ($(connectors[index]).hasClass("btnListChecked")) {
				if (checkedConnectorNames!="") checkedConnectorNames += ",";
				checkedConnectorNames += connectorName;
			} else {
				if (uncheckedConnectorNames!="") uncheckedConnectorNames += ",";
				uncheckedConnectorNames += connectorName;
			}
		});
	}
	
	return {"filter" : uncheckedConnectorNames};
}

function paintCachedData(cachedData) {
	$("#bodyPanel").hide();
	$("#fitbitCalories").hide();
	$("#fitbitSteps").hide();
	$("#fitbitSleep").remove();
	$("#zeoSleep").remove();
	for(name in cachedData) {
		if (cachedData[name]==null||typeof(cachedData[name])=="undefined")
			continue;
		updateDataDisplay(cachedData[name], name);
	}
	var nolatitude = true;
	for(name in cachedData) {
		switch(name) {
		case "google_latitude":
			nolatitude = false;
			break;
		}
	}
	FlxState.hasLatitude = !nolatitude;
}

function updateDataDisplay(connectorData, connectorInfoId) {
	switch(connectorInfoId) {
	case "fitbit-activity_summary":
		drawFitbitInfo(connectorData);
		break;
	case "google_latitude":
		FlxState.locationHistory = connectorData;
		if (connectorData!=null&&typeof(connectorData)!="undefined")
			drawLatitudeInfo();
		break;
	case "withings-weight":
		drawWeightInfo(connectorData);
		break;
	case "picasa":
	case "flickr":
	case "lastfm-recent_track":
		drawTimedData(connectorData, FlxState.MEDIA_CATEGORY);
		break;
	case "sms_backup-sms":
	case "sms_backup-call_log":
	case "twitter-dm":
	case "twitter-tweet":
	case "twitter-mention":
		drawTimedData(connectorData, FlxState.SOCIAL_CATEGORY);
		break;
	case "google_calendar":
	case "toodledo-task":
		drawTimedData(connectorData, FlxState.MIND_CATEGORY);
		break;
	case "zeo":
		updateSleepWidgetZeo(connectorData);
	case "fitbit-sleep":
	case "withings-bpm":
		drawTimedData(connectorData, FlxState.BODY_CATEGORY);
		break;
	}
}

function getUpdateNeeded(apis) {
	for(i=0;i<apis.length;i++) {
		getDayInfo(apis[i]);
	}
}

function getDayInfo(connectorName) {
	$.ajax({ url: "/api/log/" + connectorName + "/"+FlxState.state, dataType: "json",
		success: function(jsonData) {
			if (!outsideTimeBoundaries(jsonData))
				updateDataDisplay(jsonData, jsonData.name);
		}
	});
}

function drawTimedData(payload, category) {
	if ((typeof(payload)!="undefined")&&payload!=null)
		showInCategory(payload, category);
}

function drawFitbitInfo(fitbitData) {
	if (fitbitData==null) return;
	fitbitData = fitbitData[0];
	if (typeof(fitbitData)=="undefined") {
		$("#fitbitSteps").show();
		$("#fitbitCalories").show();
		$("#caloriesOut").html("0");
		$("#stepsTaken").html("0");
		$("#fitbitSteps").hide();
		$("#fitbitCalories").hide();
	} else if (fitbitData.caloriesOut==0
			||fitbitData.steps==0
			||typeof(fitbitData.caloriesOut)=="undefined"){
		$("#fitbitSteps").hide();
		$("#fitbitCalories").hide();
	} else {
		$("#bodyPanel").show();
		$("#fitbitCalories").show();
		$("#fitbitSteps").show();
		$("#caloriesOut").html(fitbitData.caloriesOut);
		$("#stepsTaken").html(fitbitData.steps);
		if (typeof(fitbitData.caloriesPerMinute)!="undefined") {
			drawCalories(fitbitData.caloriesPerMinute);
		}
	}
	if (fitbitData.MET) {
		$("#fitbitMET").remove();
		$("#bodyPanel").append("<article id=\"fitbitMET\">" +
			"<div class=\"detail\"><small>MET<br/>&nbsp;</small></div>" +
			"<span>" + fitbitData.MET + "</span>"+
		"</article>");
	}
}



function drawCalories(caloriesPerMinute) {
	for (i=0;i<caloriesPerMinute.length;i++) {
		var item = caloriesPerMinute[i];
		if (item.level==0) continue;
		var color = 0, height = 0;
		switch(item.level) {
		case 1: color = "#33cccc"; break;
		case 2: color = "#ffbb33"; break;
		case 3: color = "#ff3366"; break;
		}
		start = item.minute;
		if (FlxState.vtype=="CLOCK") {
			span = paintClockSpike(FlxState.paper, start, 83, color, FlxState.STROKE_WIDTH+item.calories*2.5);
			FlxState.clockCircles.push(span);
		} else if (FlxState.vtype=="LIST") {
			span = paintTimelineSpike(FlxState.paper, start, 15, color, FlxState.STROKE_WIDTH+item.calories*2.5);
			FlxState.timelineLanes.push(span);
		}
	}
}

function paintClockSpike(paper, time, radius, color, height) {
	var coords = clockSpike(FlxState.CLOCK_CENTER, radius, time / FlxState.RATIO + FlxState.START_AT, height),
	path = paper.path(coords);
	path.attr("stroke-width", 1)
	path.attr("stroke", color);
	return path;
}

function clockSpike(center, radius, angle, height) {
	var coords1 = toCoords(center, radius, angle),
		coords2 = toCoords(center, radius + height, angle),
		path = "M " + coords1[0] + " " + coords1[1];
	path += " L " + coords2[0] + " " + coords2[1];
	return path;
}

function paintTimelineSpike(paper, startTime, lane, color, height) {
	var tlRatio = FlxState.TIMELINE_WIDTH/(60*24),
		x1 = startTime * tlRatio,
		coords = line(x1, 23, x1, 23-height),
		path = paper.path(coords);
	path.attr("stroke-width", 1)
	path.attr("stroke", color);
	return path;
}

function drawWeightInfo(measures) {
	$("#weightWidget").hide();
	if (measures==null||typeof(measures)=="undefined") return;
	todaysMeasures = new Array();
	for (i=0; i<measures.length; i++)
		if (measures[i].daysAgo===0)
			todaysMeasures.push(measures[i]);
	if (measures.length>0) {
		$("#bodyPanel").show();
		$("#weightWidget").show();
		$("#lastWeight").html(measures[measures.length-1].description);
	}
	if (todaysMeasures.length>0) {
		drawTimedData(todaysMeasures, FlxState.BODY_CATEGORY);
	}
}

function updateSleepWidgetFitbit(sleepInfo) {
	if (sleepInfo&&sleepInfo.length>0) {
		var sleptH = Math.floor(sleepInfo[0].minutesAsleep/60);
		var sleptM = sleepInfo[0].minutesAsleep%60;
		if (sleptM<10) sleptM = "0" + sleptM;
		if (document.getElementById("zeoSleep")==null) {
			$("#bodyPanel").append("	<article id=\"fitbitSleep\">" +
				"<div class=\"detail\"><small>Time<br>slept</small></div>" +
				"<span id=\"stepsTaken\">" + sleptH + "h" + sleptM + "</span>"+
			"</article>");
		}
	} else {
		$("#fitbitSleep").remove();
	}
}

function updateSleepWidgetZeo(payload) {
	if (payload&&payload.length>0) {
		var sleptH = Math.floor(payload[0].minutesAsleep/60);
		var sleptM = payload[0].minutesAsleep%60;
		if (sleptM<10) sleptM = "0" + sleptM;
		$("#fitbitSleep").remove();
		$("#bodyPanel").append("	<article id=\"zeoSleep\">" +
			"<div class=\"detail\"><small>Time<br>slept</small></div>" +
			"<span id=\"timeSlept\">" + sleptH + "h" + sleptM + "</span>"+
		"</article>");
	} else {
		$("#zeoSleep").remove();
	}

}

function outsideTimeBoundaries(o) {
	if (typeof(o.tbounds)!="undefined") {
		return (o.tbounds.start!=FlxState.start || o.tbounds.end!=FlxState.end);
	}
	return (o.start!=FlxState.start || o.end!=FlxState.end);
}

function updateTimeBrowser(model) {
	if (window.history.pushState)
		window.history.pushState(model.timeHash, "Fluxtream - " + model.state, "/home/" + model.state);
	if (model.comment!="") {
		$("#commentMain").html(model.comment);
	}
	else
		$("#commentMain").html($("#commentMain").attr("title"));
	$("#searchBox").val($("#searchBox").attr("title"));
	$("#weatherPanel").hide();
	FlxState.weather = null;
	FlxState.state = model.state;
	FlxState.timeHash = model.timeHash;
	setToday(model.isToday);
	setTimeUnit(model.timeUnit);
	setCurrentTimespanLabel(model.currentTimespanLabel);
	setVisualizationType(model.visualizationType);
	collapseComment();
}

function setVisualizationType(visualizationType) {
	FlxState.vtype = visualizationType;
	var sameViz = typeof(FlxState.previous_vtype)!="undefined"&&FlxState.previous_vtype==visualizationType;
	if (sameViz) {
		updateVisualization();
		return;
	}
	FlxState.previous_vtype = visualizationType;
	$("#mainCont").empty();
	$("#footer").hide();
	switch(visualizationType) {
	case "TOOLS":
	case "CLOCK":
	case "LIST":
		getViewTemplate(visualizationType);
		break;
	case "SEARCH":
		getSearchResults();
		break;
	}
}

function updateVisualization() {
	switch(FlxState.vtype) {
	case "TOOLS":
		$(".navCont").css("font-color", "0xfff");
		console.log("we should load the 'tools' module, perhaps");
		break;
	case "CLOCK":
		$(".navCont").css("font-color", "0xfff");
		doDrawClock();
		break;
	case "LIST":
		$(".navCont").css("font-color", "0xfff");
		getFacets(0);
		doDrawTimeline();
		break;
	case "SEARCH":
		$(".navCont").css("font-color", "0xccc");
		getSearchResults(0);
		break;
	}
}

function getViewTemplate(vtype) {
	$.get('/views/'+vtype.toLowerCase(), function(view) {
		$('#mainCont').html(view);
		$("#footer").show();
		updateVisualization();
		showMap();
	});
}
