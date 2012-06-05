define(["applications/calendar/tabs/clock/ClockDrawingUtils",
        "applications/calendar/tabs/clock/ClockConfig",
        "applications/calendar/tabs/Tab",
        "applications/calendar/App",
       "applications/calendar/tabs/map/MapUtils"], function(DrawingUtils, Config, Tab, Log, MapUtils) {
	
	var paper = null;
	var config = null;
    var map = null;
    var hourlyWeatherData = null;
    var solarInfo = null;
    var tempratureUnit = null;
    var distanceUnit = null;
    var dayStart, dayEnd;

	function render(digest, timeUnit) {
        hideEventInfo();
		this.getTemplate("text!applications/calendar/tabs/clock/clock.html", "clock", function() {
			 setup(digest, timeUnit);
		});
	}
	
	function setup(digest, timeUnit) {
        hourlyWeatherData = digest.hourlyWeatherData;
        solarInfo = digest.solarInfo;
        tempratureUnit = digest.settings.temperatureUnit;
        distanceUnit = digest.settings.distanceUnit;
        dayStart = digest.tbounds.start;
        dayEnd = digest.tbounds.end;
        if (digest.cachedData != null && digest.cachedData.google_latitude != null){
            map = MapUtils.newMap(new google.maps.LatLng(0,0),8,"clockMap",true);
            map.addGPSData(digest.cachedData.google_latitude);
            map.fitBounds(map.gpsBounds);
        }
        else{
            hideQTipMap();
            map = null;
        }

		var availableWidth = $("#clockTab").width();
		var edgeWidth =  Math.min(availableWidth, 600);
		$("#clockTab div:first-child").width(edgeWidth+"px");
		$("#paper").empty();
		$("#paper").width(edgeWidth);
		paper = Raphael("paper", edgeWidth, edgeWidth);
		config = Config.getConfig(edgeWidth, digest.tbounds.start, digest.tbounds.end);
		var drawingUtils = DrawingUtils.getDrawingUtils(config);
		config.clockCircles = paper.set();
		drawingUtils.paintCircle(paper, config.BODY_CATEGORY.orbit, "#ffffff", 1);
		drawingUtils.paintCircle(paper, config.AT_HOME_CATEGORY.orbit, "#ffffff", 1);
		drawingUtils.paintCircle(paper, config.OUTSIDE_CATEGORY.orbit, "#ffffff", 1);
		drawingUtils.paintCircle(paper, config.MIND_CATEGORY.orbit, "#ffffff", 1);
		drawingUtils.paintCircle(paper, config.SOCIAL_CATEGORY.orbit, "#ffffff", 1);
		drawingUtils.paintCircle(paper, config.MEDIA_CATEGORY.orbit, "#ffffff", 1);
		paintSolarInfo(digest.solarInfo);
		for(var objectTypeName in digest.cachedData) {
			if (digest.cachedData[objectTypeName]==null||typeof(digest.cachedData[objectTypeName])=="undefined")
				continue;
			updateDataDisplay(digest.cachedData[objectTypeName], objectTypeName, digest);
			if (objectTypeName==="fitbit-activity_summary" && digest.cachedData["fitbit-activity_summary"][0]) {
				drawCalories(digest.cachedData["fitbit-activity_summary"][0].caloriesPerMinute);
			}
		}
		for(i=0;i<digest.updateNeeded.length;i++) {
			getDayInfo(digest.updateNeeded[i], digest);
		}
	}

	function outsideTimeBoundaries(o) {
		if (typeof(o.tbounds)!="undefined") {
			return (o.tbounds.start!=config.start || o.tbounds.end!=config.end);
		}
		return (o.start!=config.start || o.end!=config.end);
	}

	function getDayInfo(connectorName, digest) {
		$.ajax({ url: "/api/calendar/" + connectorName + "/"+Log.tabState, dataType: "json",
			success: function(jsonData) {
				if (!outsideTimeBoundaries(jsonData))
					updateDataDisplay(jsonData, jsonData.name, digest);
			}
		});
	}
	
	function drawCalories(caloriesPerMinute) {
		if (!caloriesPerMinute)
			return;
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
			span = paintClockSpike(paper, start, 83, color, config.STROKE_WIDTH+item.calories*2.5);
			config.clockCircles.push(span);
		}
	}

	function paintClockSpike(paper, time, radius, color, height) {
		var coords = clockSpike(config.CLOCK_CENTER, radius, time / config.RATIO + config.START_AT, height),
		path = paper.path(coords);
		path.attr("stroke-width", 1);
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

	function fillRegion(center, radius1, radius2, startAngle, endAngle) {
		var startCoords = toCoords(center, radius1, startAngle),
			outerStart = toCoords(center, radius2, startAngle),
			endCoords = toCoords(center, radius1, endAngle),
			outerEnd = toCoords(center, radius2, endAngle),
			path = "M "+ startCoords[0] + "," + startCoords[1];
		path += "A " + radius1 + "," + radius1 + " 0 0,0 " + endCoords[0] + "," + endCoords[1] +" ";
		path += "L " + outerEnd[0] + "," + outerEnd[1];
		path += "A " + radius2 + "," + radius2 + " 0 0,1 " + outerStart[0] + "," + outerStart[1] + " Z";
		return path;
	}
	
	function paintSolarInfo(solarInfo) {
		if (solarInfo!=null) {
			var startAngle =  solarInfo.sunrise / config.RATIO + config.START_AT,
				endAngle = solarInfo.sunset / config.RATIO + config.START_AT,
				midAngle = endAngle*0.2;
			if (endAngle < 390 ) {
				var coords = fillRegion(config.CLOCK_CENTER, config.BODY_CATEGORY.orbit-15, config.MEDIA_CATEGORY.orbit+15, startAngle, midAngle);
				config.clockCircles.push(
					function() {
						var path = paper.path(coords);
						path.attr("stroke", "rgba(199,199,199,.5)");
						path.attr("fill", "rgba(199,199,199,.5)");
						path.toBack();
						return path;
					}()
				);
				coords = fillRegion(config.CLOCK_CENTER, config.BODY_CATEGORY.orbit-15, config.MEDIA_CATEGORY.orbit+15, midAngle, endAngle);
				config.clockCircles.push(
					function() {
						var path = paper.path(coords);
						path.attr("stroke", "rgba(199,199,199,.5)");
						path.attr("fill", "rgba(199,199,199,.5)");
						path.toBack();
						return path;
					}()
				);
			} else {
				var coords = fillRegion(config.CLOCK_CENTER, config.BODY_CATEGORY.orbit-15, config.MEDIA_CATEGORY.orbit+15, startAngle, endAngle);
				config.clockCircles.push(
					function() {
						var path = paper.path(coords);
						path.attr("stroke", "rgba(199,199,199,.5)");
						path.attr("fill", "rgba(199,199,199,.5)");
						path.toBack();
						return path;
					}()
				);
			}
		}
	}
	
	function updateDataDisplay(connectorData, connectorInfoId, digest) {
		switch(connectorInfoId) {
		case "fitbit-activity_summary":
//			drawFitbitInfo(connectorData);
			break;
		case "google_latitude":
			if (connectorData!=null&&typeof(connectorData)!="undefined")
				locationBreakdown(connectorData, digest);
			break;
		case "withings-weight":
//			drawWeightInfo(connectorData);
			break;
		case "picasa-photo":
		case "flickr":
		case "lastfm-recent_track":
        case "lastfm-loved_track":
			drawTimedData(connectorData, config.MEDIA_CATEGORY);
			break;
		case "sms_backup-sms":
		case "sms_backup-call_Calendar":
		case "twitter-dm":
		case "twitter-tweet":
		case "twitter-mention":
			drawTimedData(connectorData, config.SOCIAL_CATEGORY);
			break;
		case "google_calendar-entry":
		case "toodledo-task":
			drawTimedData(connectorData, config.MIND_CATEGORY);
			break;
		case "zeo":
//			updateSleepTabZeo(connectorData);
		case "fitbit-sleep":
		case "withings-bpm":
			drawTimedData(connectorData, config.BODY_CATEGORY);
			break;
		}
	}

	function drawTimedData(payload, category) {
		if ((typeof(payload)!="undefined")&&payload!=null)
			drawEvents(payload, category.orbit, category.color);
	}

	function drawEvents(items, orbit, color) {
		if (typeof(items)=="undefined") return;
		for (i = 0; i < items.length; i++) {
			try {
				var item = items[i];
				config.clockCircles.push(
					function() {
						var start = item.startMinute;
						var end = item.endMinute, instantWidth = 2;
						if (orbit===config.BODY_CATEGORY.orbit)
							instantWidth=18;
						if (start>end) { start = 0; }
						instantaneous = typeof(item.endMinute)=="undefined"||item.endMinute===item.startMinute;
						var span;
						if (instantaneous)
							span = paintSpan(paper, start,start+instantWidth, orbit, color, .9);
						else
							span = paintSpan(paper, start,(start<=end?end:1440), orbit, color, .9);
						span.node.item = item;
						$(span.node).css("cursor", "pointer");
						$(span.node).click({instantaneous:instantaneous}, function(event) {
                            if (!event.data.instantaneous)
                                event.timeTarget = getTimestampForPoint(event.offsetX,event.offsetY);
                            else
                                event.timeTarget = event.target.item.start;
                            event.minuteOfDay = getMinuteOfDay(event.timeTarget);
                            event.flip = event.offsetY > config.CLOCK_CENTER[1];
							showEventInfo(event);
						});
						$(span.node).mouseout(function() {
							hideEventInfo();
							this.style.cursor = "default";
						});
						return span;
					}()
				);
			} catch (e) {
				if (typeof(console)!="undefined"&&console.log)
					console.log("there was an error parsing this json: " + e);
			}
		}
	}
	
	var ttpdiv = null, lastHoveredEvent, timeout = null, markers = new Array();
	
	function showEventInfo(event) {
        hideEventInfo();
		lastHoveredEvent = event;
		var span = event.target;
		var facet = span.item;
		if (facet.type=="google_latitude")
			return;
        var target = $(event.target).parent().position();
        var tip_y = target.top + event.offsetY;
        var tip_x = target.left + event.offsetX;
        var offsetX = config.CLOCK_CENTER[0] - event.offsetX;
        var offsetY = config.CLOCK_CENTER[1] - event.offsetY;
        if (map != null){
            markers[0] = map.addItem(span.item,false);
            if (markers[0] != null){
                markers[0].doHighlighting();
                markers[0].hideMarker();
                markers[1] = new google.maps.Marker({map:map, position:map.getLatLngOnGPSLine(event.timeTarget),
                                                    icon:markers[0].getIcon(),shadow:markers[0].getShadow()});
                map.enhanceMarker(markers[1],event.timeTarget);
                markers[1].showCircle();
                map.zoomOnMarker(markers[1]);
            }
        }

        var contents = facet.getDetails();

        showToolTip(tip_x,tip_y,offsetX,offsetY,contents,event.minuteOfDay,$(event.target).attr("stroke"),$(event.target).parent().parent(),
                    markers[0] == null ? null : markers[0].getPosition());
	}

    function showToolTip(x,y, offX, offY,contents,minute,color,parent,gpsPos){
        var weatherInfo = getWeatherData(minute);
        var weatherIcon;
        if (minute < solarInfo.sunrise || minute > solarInfo.sunset){//night
            weatherIcon = weatherInfo.weatherIconUrlNight;
        }
        else{//day
            weatherIcon = weatherInfo.weatherIconUrlDay;
        }
        var orientation, tailOrientation;
        var angle = toPolar([0,0],offX,offY)[1];
        if (angle < 45 || angle > 315){
            orientation = "Left";
            tailOrientation = "right";
        }
        else if (angle > 135 && angle < 225){
            orientation = "Right";
            tailOrientation = "left";
        }
        else if (offY > 0){
            orientation = "Top";
            tailOrientation = "bottom";
        }
        else{
            orientation = "Bottom";
            tailOrientation = "top";
        }
        App.loadMustacheTemplate("applications/calendar/tabs/clock/clockTemplate.html","tooltip",function(template){
            ttpdiv = $(template.render({
                weatherDescription: weatherInfo.weatherDesc,
                temperature: tempratureUnit === "FAHRENHEIT" ? weatherInfo.tempF : weatherInfo.tempC,
                temperatureUnit: tempratureUnit === "FAHRENHEIT" ? "F" : "C",
                windSpeed: distanceUnit == "SI" ? weatherInfo.windspeedKmph : weatherInfo.windspeedMiles,
                windSpeedUnit: distanceUnit == "SI" ? "km/h" : "mph",
                humidity: weatherInfo.humidity,
                precipitation: weatherInfo.precipMM,
                precipitationUnit: "mm",
                weatherIcon: weatherIcon,
                orientation:orientation,
                oppositeOrientation:tailOrientation,
                color:color,
                time: App.formatMinuteOfDay(minute),
                tooltipData:contents
            }));
            ttpdiv.css("position","absolute");
            ttpdiv.css("zIndex","100");
            var tail = ttpdiv.find(".flx-toolTipTail-" + orientation);
            parent.append(ttpdiv);

            var displayX = x;
            var displayY = y;

            if (orientation == "Left" || orientation == "Right"){
                displayY += offY - ttpdiv.height()/2;
                tail.css("top",ttpdiv.height()/2 - offY - 10);
            }
            else{
                displayX += offX - ttpdiv.width()/2;
                tail.css("left",ttpdiv.width()/2 - offX - 10);
            }
            switch(orientation){
                case "Left":
                    displayX += 10;
                    break;
                case "Right":
                    displayX -= 10 + ttpdiv.width();
                    break;
                case "Top":
                    displayY += 10;
                    break;
                case "Bottom":
                    displayY -= 10 + ttpdiv.height();
                    break;
            }


            ttpdiv.css("left",displayX);
            ttpdiv.css("top",displayY);

            $("#mapPlaceHolder").append(document.getElementById("clockMap"));
            var lastSeen = $("#lastSeenLocation");
            App.geocoder.geocode({'latLng': gpsPos}, function(results,status){
                if (status == google.maps.GeocoderStatus.OK) {
                    for (var i = 2; i >= 0; i--){
                        if (results[i]){
                            lastSeen.text(results[i].formatted_address);
                            return;
                        }

                    }
                }
            });
       });

    }

    //hourlyWeatherData
    function getWeatherData(minuteOfDay){
        if (hourlyWeatherData == null)
            return null;
        var i;
        for (i = 0; i < hourlyWeatherData.length - 1 && hourlyWeatherData[i].minuteOfDay < minuteOfDay; i++);
        var weatherInfo = hourlyWeatherData[i];
        return weatherInfo;
    }

    function showLocationBreakdownInfo(event) {
        hideEventInfo();
        var span = event.target;
        var facetId = span.item.id;
        var target = $(event.target).parent().position();
        var tip_y = target.top + event.offsetY;
        var tip_x = target.left + event.offsetX;
        var offsetX = config.CLOCK_CENTER[0] - event.offsetX;
        var offsetY = config.CLOCK_CENTER[1] - event.offsetY;

        if (map != null){
            map.highlightTimespan(span.item.start,span.item.end);
            //map.zoomOnTimespan(span.item.start,span.item.end);
            markers[0] = new google.maps.Marker({map:map, position:map.getLatLngOnGPSLine(event.timeTarget)});
            map.enhanceMarker(markers[0],event.timeTarget);
            markers[0].showCircle();
            map.zoomOnMarker(markers[0]);
        }
        showToolTip(tip_x,tip_y,offsetX,offsetY,span.item.description,event.minuteOfDay,$(event.target).attr("stroke"),$(event.target).parent().parent(),
                    markers[0] == null ? null : markers[0].getPosition());
    }
	
	function hideEventInfo() {
        if (map != null){
            hideQTipMap();
            for (var i = 0; i < markers.length; i++){
                if (markers[i] != null)
                    markers[i].setMap(null);
            }
            markers = new Array();
            map.fitBounds(map.gpsBounds);
            map.highlightTimespan(dayStart,dayEnd);
        }
        if (ttpdiv != null && ttpdiv.qtip != null){
            ttpdiv.remove();
            ttpdiv = null;
        }
	}

	function arc(center, radius, startAngle, endAngle) {
		if (endAngle - startAngle < 2)
			endAngle += 1;
		var angle = startAngle,
			coords = toCoords(center, radius, angle),
			path = "M " + coords[0] + " " + coords[1];
		while (angle <= endAngle) {
			coords = toCoords(center, radius, angle);
			path += " L " + coords[0] + " " + coords[1];
			angle += 1;
		}
		return path;
	}

	function toCoords(center, radius, angle) {
		var radians = (angle / 180) * Math.PI,
			x = center[0] + Math.cos(radians) * radius,
			y = center[1] + Math.sin(radians) * radius;
		return [ x, y ];
	}
	
	function paintSpan(paper, startTime, endTime, radius, color, opacity) {
		var coords = arc(config.CLOCK_CENTER, radius, startTime / config.RATIO + config.START_AT, endTime
				/ config.RATIO + config.START_AT),
		path = paper.path(coords);
		path.attr("stroke-width", config.STROKE_WIDTH);
		path.attr("stroke", color);
		path.attr("opacity", opacity);
		return path;
	}

    function toPolar(center, x, y){
        x -= center[0];
        y -= center[1];
        var r = Math.sqrt(x * x + y * y);
        var theta;
        if (x == 0){
            if (y > 0)
                theta = Math.PI / 2;
            else
                theta = 3 * Math.PI / 2;
        }
        else if (y == 0){
            if (x > 0)
                theta = 0;
            else
                theta = Math.PI;
        }
        else if (x > 0)
            theta = Math.atan(y/x);
        else
            theta = Math.PI + Math.atan(y/x);
        theta *= 180 / Math.PI;
        if (theta < 0)
            theta += 360;
        return [r,theta];
    }

    function getTimestampForPoint(x,y){
        var angleClick = toPolar(config.CLOCK_CENTER,x,y)[1] - config.START_AT;
        if (angleClick < 0)
            angleClick += 360;
        return dayStart+angleClick*config.RATIO*60000;
    }

    function getMinuteOfDay(timestamp){
        var minute = timestamp;
        minute -= dayStart;
        minute /= 60 * 1000;
        return minute;
    }
	
	function showLocationBreakdown(items, color) {
		if (typeof(items)=="undefined"||items==null)
			return;
		showWheelBreakdown(items, color);
	}

	function showWheelBreakdown(items, color) {
		if (typeof(items)=="undefined") return;
		for (i = 0; i < items.length; i++) {
			try {
				var item = items[i],
					instantWidth=10;
				config.clockCircles.push(
					function() {
						var start = item.startMinute;
						var end = item.endMinute;
						if (start>end) { start = 0; }
						var span = paintSpan(paper, start,(start<=end?end:1440), config.AT_HOME_CATEGORY.orbit, color, 1, config);
						span.node.item = item;
						$(span.node).click(function(event) {
                            event.timeTarget = getTimestampForPoint(event.offsetX,event.offsetY);
                            event.minuteOfDay = getMinuteOfDay(event.timeTarget);
							this.style.cursor = "pointer";
                            event.flip = event.offsetY > config.CLOCK_CENTER[1];
							showLocationBreakdownInfo(event);
						});
						$(span.node).mouseout(function() {
							hideEventInfo();
							this.style.cursor = "default";
						});
						return span;
					}()
				);
			} catch (e) {
				if (typeof(console)!="undefined"&&console.log)
					console.log("there was an error parsing this json: " + e);
			}
		}
	}

	function locationBreakdown(positions, digest) {
        var homeAddress = {latitude:0,longitude:0};
        if (digest.addresses.ADDRESS_HOME != null && digest.addresses.ADDRESS_HOME.length != 0)
            homeAddress = digest.addresses.ADDRESS_HOME[0];
		var pos1 = new google.maps.LatLng(homeAddress.latitude, homeAddress.longitude),
			i=0, checkin, pos2, notAtHome, lastCollection, currentCollection,
			mergedAtHome = new Array(), mergedOutside = new Array(), farAwayPositionsCount = 0;
		for (; i<positions.length; i++) {
			pos2 = new google.maps.LatLng(positions[i].position[0], positions[i].position[1]);
			var distance = google.maps.geometry.spherical.computeDistanceBetween(pos1, pos2);
			if (distance<500) {
				positions[i].at = "home";
			} else if (distance>1000*300) {
				farAwayPositionsCount++;
			}
		}
		if (farAwayPositionsCount==positions.length) {
			config.traveling = true;
		} else {
			config.traveling = false;
		}
		notAtHome = new Array();
		atHome = new Array();
		for (i=0; i<positions.length; i++) {
			if (positions[i].at!="home")
				currentCollection = notAtHome;
			else
				currentCollection = atHome;
			currentCollection.push(positions[i]);
			if (currentCollection==lastCollection)
				continue;
			else {
				if (lastCollection!=null) {
					lastCollection.push(positions[i]);
					lastCollection.push("stop");
				}
				lastCollection = currentCollection;
			}
		}
		atHome = mergePositionFamilies(atHome, "home");
		notAtHome = mergePositionFamilies(notAtHome, "out");
		showLocationBreakdown(atHome, "#4c99c5");
		showLocationBreakdown(notAtHome, "#5cae5c");
	}

	function mergePositionFamilies(positionFamilies, where) {
		var result = [];
		var positions = [];
		for (var i=0; i<positionFamilies.length; i++) {
			if (positionFamilies[i]==="stop") {
				result.push(mergePositions(positions, where));
				positions = [];
				continue;
			}
			positions.push(positionFamilies[i]);
		}
		result.push(mergePositions(positions, where));
		return result;
	}

	function mergePositions(positions, where) {
		if (positions.length===0) return positions;
		var first = positions[0];
		var last = positions[positions.length-1];
		return {
			start: first.start,
			end : last.start,
			description: "You were " + where
				+ " from " + App.formatMinuteOfDay(first.startMinute)
				+ " to " + App.formatMinuteOfDay(last.startMinute),
			startMinute: first.startMinute,
			endMinute: last.startMinute,
			type: "google_latitude"
		};
	}

    function hideQTipMap(){
        document.getElementById("clockMapContainer").appendChild(document.getElementById("clockMap"));
    }


    function qTipUpdate(){
        $("#mapPlaceHolder").append(document.getElementById("clockMap"));
        /*if ($("#mapPlaceHolder").offset().left != $("#mapPlaceHolder").position().left){
            var mapdiv = document.getElementById("clockMapContainer");
            var left = $("#mapPlaceHolder").offset().left;
            var top = $("#mapPlaceHolder").offset().top;
            mapdiv.style.top = top + "px";
            mapdiv.style.left = left + "px";
        }
        else{
            setTimeout("document.qTipUpdate();",10);
        }*/
    }

	var clockTab = new Tab("clock", "Candide Kemmler", "icon-time", true);
    document.qTipUpdate = qTipUpdate;
    document.hideQTipMap = hideQTipMap;
	clockTab.render = render;
	return clockTab;
	
});
