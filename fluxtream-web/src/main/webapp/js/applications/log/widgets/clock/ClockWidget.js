define(["applications/log/widgets/clock/ClockdrawingUtils",
        "applications/log/widgets/clock/ClockConfig"], function(DrawingUtils, Config) {
	
	var paper = null;

	function render(digest, timeUnit) {
		require(["text!applications/log/widgets/clock/clock.html"], function(template) {
			$("#widgets").append(template);
			$("#tooltips").load("/log/tooltips");
			var availableWidth = $("#clockWidget").width();
			var edgeWidth =  Math.min(availableWidth, 600);
			$("#paper").width(edgeWidth);
			paper = Raphael("paper", edgeWidth, edgeWidth);
			var config = Config.getConfig(edgeWidth);
			var drawingUtils = DrawingUtils.getDrawingUtils(config);
			config.clockCircles = paper.set();
			drawingUtils.paintCircle(paper, config.BODY_CATEGORY.orbit, "#ffffff", 1);
			drawingUtils.paintCircle(paper, config.AT_HOME_CATEGORY.orbit, "#ffffff", 1);
			drawingUtils.paintCircle(paper, config.OUTSIDE_CATEGORY.orbit, "#ffffff", 1);
			drawingUtils.paintCircle(paper, config.MIND_CATEGORY.orbit, "#ffffff", 1);
			drawingUtils.paintCircle(paper, config.SOCIAL_CATEGORY.orbit, "#ffffff", 1);
			drawingUtils.paintCircle(paper, config.MEDIA_CATEGORY.orbit, "#ffffff", 1);
			paintSolarInfo(digest.solarInfo, paper, config);
			for(name in digest.cachedData) {
				if (digest.cachedData[name]==null||typeof(digest.cachedData[name])=="undefined")
					continue;
				updateDataDisplay(digest.cachedData[name], name, paper, config, digest);
				if (name==="fitbit-activity_summary" && digest.cachedData["fitbit-activity_summary"][0]) {
					drawCalories(digest.cachedData["fitbit-activity_summary"][0].caloriesPerMinute, paper, config);
				}
			}
		});
	}
	
	function drawCalories(caloriesPerMinute, paper, config) {
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
			span = paintClockSpike(paper, start, 83, color, config.STROKE_WIDTH+item.calories*2.5, config);
			config.clockCircles.push(span);
		}
	}

	function paintClockSpike(paper, time, radius, color, height, config) {
		var coords = clockSpike(config.CLOCK_CENTER, radius, time / config.RATIO + config.START_AT, height),
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
	
	function paintSolarInfo(solarInfo, paper, config) {
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
	
	function updateDataDisplay(connectorData, connectorInfoId, paper, config, digest) {
		switch(connectorInfoId) {
		case "fitbit-activity_summary":
//			drawFitbitInfo(connectorData);
			break;
		case "google_latitude":
			if (connectorData!=null&&typeof(connectorData)!="undefined")
				locationBreakdown(connectorData, digest, config);
			break;
		case "withings-weight":
//			drawWeightInfo(connectorData);
			break;
		case "picasa":
		case "flickr":
		case "lastfm-recent_track":
			drawTimedData(connectorData, config.MEDIA_CATEGORY, paper, config);
			break;
		case "sms_backup-sms":
		case "sms_backup-call_log":
		case "twitter-dm":
		case "twitter-tweet":
		case "twitter-mention":
			drawTimedData(connectorData, config.SOCIAL_CATEGORY, paper, config);
			break;
		case "google_calendar":
		case "toodledo-task":
			drawTimedData(connectorData, config.MIND_CATEGORY, paper, config);
			break;
		case "zeo":
//			updateSleepWidgetZeo(connectorData);
		case "fitbit-sleep":
		case "withings-bpm":
			drawTimedData(connectorData, config.BODY_CATEGORY, paper, config);
			break;
		}
	}

	function drawTimedData(payload, category, paper, config) {
		if ((typeof(payload)!="undefined")&&payload!=null)
			drawEvents(payload, category.orbit, category.color, paper, config);
	}

	function drawEvents(items, orbit, color, paper, config) {
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
							span = paintSpan(paper, start,start+instantWidth, orbit, color, .9, config);
						else
							span = paintSpan(paper, start,(start<=end?end:1440), orbit, color, .9, config);
						span.node.item = item;
						$(span.node).css("cursor", "pointer");
						$(span.node).click(function() {
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
	
	var ttpdiv, lastHoveredEvent;
	
	function showEventInfo(event) {
		ttpdiv = $("#tooltip");
		lastHoveredEvent = event;
		var span = event.target;
		var facetId = span.item.id;
		var facetType = span.item.type;
		if (facetType=="google_latitude") 
			return;
		var tip_y = event.pageY;
		var tip_x = event.pageX;
		var tooltip = $("#" + facetType + "_" + facetId);
		ttpdiv.qtip({
		   content: {
		      text: tooltip.html()
		   },
		   style: {
		      classes: 'ui-tooltip-light ui-tooltip-shadow ui-tooltip-rounded',
		   },
		   position: {
	           target: [tip_x,tip_y], // ... in the window
		   	   my: "top center",
		   	   adjust: { y: 13 }
		   },
	       show: {
	          ready: true, // Show it straight away
	       },
	       hide: {
			  effect: function(offset) {
			      $(this).slideDown(100); // "this" refers to the tooltip
			  },
			  inactive : 4500	
	       }
		});
	}
	
	function hideEventInfo() {
		ttpdiv.qtip('hide');
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
	
	function paintSpan(paper, startTime, endTime, radius, color, opacity, config) {
		var coords = arc(config.CLOCK_CENTER, radius, startTime / config.RATIO + config.START_AT, endTime
				/ config.RATIO + config.START_AT),
		path = paper.path(coords);
		path.attr("stroke-width", config.STROKE_WIDTH)
		path.attr("stroke", color);
		path.attr("opacity", opacity);
		return path;
	}
	
	function showLocationBreakdown(items, color, config) {
		if (typeof(items)=="undefined"||items==null)
			return;
		showWheelBreakdown(items, color, config);
	}

	function showWheelBreakdown(items, color, config) {
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
						console.log("painting span: " + start + ", " + end);
						var span = paintSpan(paper, start,(start<=end?end:1440), config.AT_HOME_CATEGORY.orbit, color, 1, config);
						span.node.item = item;
						$(span.node).mouseover(function(event) {
							this.style.cursor = "pointer";
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

	function locationBreakdown(positions, digest, config) {
		var pos1 = new google.maps.LatLng(digest.homeAddress.latitude, digest.homeAddress.longitude),
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
					lastCollection.push("stop")
				}
				lastCollection = currentCollection;
			}
		}
		atHome = mergePositionFamilies(atHome, "home");
		notAtHome = mergePositionFamilies(notAtHome, "out");
		showLocationBreakdown(atHome, "#4c99c5", config);
		showLocationBreakdown(notAtHome, "#5cae5c", config);
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
				+ " from " + minutesToWallClockTime(first.startMinute)
				+ " to " + minutesToWallClockTime(last.startMinute),
			startMinute: first.startMinute,
			endMinute: last.startMinute,
			type: "google_latitude"
		};
	}

	function minutesToWallClockTime(minutes) {
		if (minutes<60) {
			if (minutes<10) minutes = "0" + minutes;
			return "0:"+minutes + " AM";
		}
		else {
			var hour = Math.floor(minutes/60);
			var minutes = Math.floor(minutes%60);
			if (minutes<10) minutes = "0" + minutes;
			if (hour<12)
				return hour + ":" + minutes + " AM";
			else
				return hour + ":" + minutes + " PM";
		}
	}
	
	function showLocationBreakdownInfo(event) {
		ttpdiv = $("#tooltip");
		config.lastHoveredEvent = event;
		var span = event.target;
		var facetId = span.item.id;
		var tip_y = event.pageY;
		var tip_x = event.pageX;
		ttpdiv.qtip({
		   content: {
		      text: span.item.description
		   },
		   style: {
		      classes: 'ui-tooltip-light ui-tooltip-shadow ui-tooltip-rounded',
		   },
		   position: {
	           target: [tip_x,tip_y], // ... in the window
		   	   my: "top center",
		   	   adjust: { y: 13 }
		   },
	       show: {
	          ready: true, // Show it straight away
	       },
	       hide: {
			  effect: function(offset) {
			      $(this).slideDown(100); // "this" refers to the tooltip
			  },
			  inactive : 4500
	       }
		});
	}
	
	console.log("HAHAHHAHAHAHAHAHHAHAHA");
	var clockWidget = {};
	clockWidget.render = render;
	return clockWidget;
	
});
