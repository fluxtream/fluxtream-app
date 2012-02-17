
function showLocationBreakdown(items, color) {
	if (typeof(items)=="undefined"||items==null) {
		return;
	}
	switch (FlxState.vtype) {
	case "CLOCK":
		showWheelBreakdown(items, color);
		break;
	}
}

function showWheelBreakdown(items, color) {
	if (typeof(items)=="undefined") return;
	for (i = 0; i < items.length; i++) {
		try {
			var item = items[i],
				instantWidth=10;
			FlxState.clockCircles.push(
				function() {
					var start = item.startMinute;
					var end = item.endMinute;
					if (start>end) { start = 0; }
					var span = paintSpan(FlxState.paper, start,(start<=end?end:1440), FlxState.AT_HOME_CATEGORY.orbit, color, 1);
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


function paintTLSolarInfo(solarInfo) {
	if (solarInfo!=null) {
		var tlRatio = FlxState.TIMELINE_WIDTH/(60*24),
			sunriseRectWidth = solarInfo.sunrise * tlRatio,
			sunriseRect = FlxState.paper.rect(0, 0, sunriseRectWidth, FlxState.TIMELINE_HEIGHT),
			sunsetX = solarInfo.sunset * tlRatio,
			sunsetRect = FlxState.paper.rect(sunsetX, 0, FlxState.TIMELINE_WIDTH-sunsetX, FlxState.TIMELINE_HEIGHT);
		FlxState.timelineLanes.push(
			function() {
				sunriseRect.attr("fill", "rgba(199,199,199,.5)");
				sunriseRect.attr("stroke", "rgba(199,199,199,.5)");
				return sunriseRect;
			}()
		);
		FlxState.timelineLanes.push(
			function() {
				sunsetRect.attr("fill", "rgba(199,199,199,.5)");
				sunsetRect.attr("stroke", "rgba(199,199,199,.5)");
				return sunsetRect;
			}()
		);
	}
}

function paintSolarInfo(solarInfo) {
	if (solarInfo!=null) {
		var startAngle =  solarInfo.sunrise / FlxState.RATIO + FlxState.START_AT,
			endAngle = solarInfo.sunset / FlxState.RATIO + FlxState.START_AT,
			midAngle = endAngle*0.2;
		if (endAngle < 390 ) {
			var coords = fillRegion(FlxState.CLOCK_CENTER, FlxState.BODY_CATEGORY.orbit-15, FlxState.MEDIA_CATEGORY.orbit+15, startAngle, midAngle);
			FlxState.clockCircles.push(
				function() {
					var path = FlxState.paper.path(coords);
					path.attr("stroke", "rgba(199,199,199,.5)");
					path.attr("fill", "rgba(199,199,199,.5)");
					path.toBack();
					return path;
				}()
			);
			coords = fillRegion(FlxState.CLOCK_CENTER, FlxState.BODY_CATEGORY.orbit-15, FlxState.MEDIA_CATEGORY.orbit+15, midAngle, endAngle);
			FlxState.clockCircles.push(
				function() {
					var path = FlxState.paper.path(coords);
					path.attr("stroke", "rgba(199,199,199,.5)");
					path.attr("fill", "rgba(199,199,199,.5)");
					path.toBack();
					return path;
				}()
			);
		} else {
			var coords = fillRegion(FlxState.CLOCK_CENTER, FlxState.BODY_CATEGORY.orbit-15, FlxState.MEDIA_CATEGORY.orbit+15, startAngle, endAngle);
			FlxState.clockCircles.push(
				function() {
					var path = FlxState.paper.path(coords);
					path.attr("stroke", "rgba(199,199,199,.5)");
					path.attr("fill", "rgba(199,199,199,.5)");
					path.toBack();
					return path;
				}()
			);
		}
	}
}

function paintTimespan(paper, y, color, opacity) {
	var coords = line(0, y, FlxState.TIMELINE_WIDTH, y);
	FlxState.timelineLanes.push(
		function() {
			var path = paper.path(coords);
			path.attr("stroke-width", FlxState.STROKE_WIDTH);
			path.attr("stroke", color);
			path.attr("opacity", opacity);
			return path;
		}()
	);
}

function paintCircle(paper, radius, color, opacity) {
	var coords = arc(FlxState.CLOCK_CENTER, radius, 0, 360);
	FlxState.clockCircles.push(
		function() {
			var path = paper.path(coords);
			path.attr("stroke-width", FlxState.STROKE_WIDTH)
			path.attr("stroke", "rgba(255,255,255," + opacity + ")");
			return path;
		}()
	);
}

function paintTimelineSpan(paper, startTime, endTime, lane, color) {
	var tlRatio = FlxState.TIMELINE_WIDTH/(60*24),
		x1 = startTime * tlRatio,
		x2 = endTime * tlRatio,
		coords = line(x1, lane, x2, lane),
		path = paper.path(coords);
	path.attr("stroke-width", FlxState.STROKE_WIDTH)
	path.attr("stroke", color);
	return path;
}

function paintSpan(paper, startTime, endTime, radius, color, opacity) {
	var coords = arc(FlxState.CLOCK_CENTER, radius, startTime / FlxState.RATIO + FlxState.START_AT, endTime
			/ FlxState.RATIO + FlxState.START_AT),
	path = paper.path(coords);
	path.attr("stroke-width", FlxState.STROKE_WIDTH)
	path.attr("stroke", color);
	path.attr("opacity", opacity);
	return path;
}

function fillArc(paper, startTime, endTime, radius, color, opacity) {
	var coords = arc(FlxState.CLOCK_CENTER, radius, startTime / FlxState.RATIO + FlxState.START_AT, endTime
			/ FlxState.RATIO + FlxState.START_AT),
	path = paper.path(coords);
	path.attr("stroke-width", 195)
	path.attr("stroke", color);
	path.attr("opacity", opacity);
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

function line(x1, y1, x2, y2) {
	var path = "M " + x1 + " " + y1;
	path += " L " + x2 + " " + y2;
	return path;
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

function getModel() {
	$.ajax({
		url: "/home/model.json",
		dataType: "json",
		success: function(model) {
			updateTimeBrowser(model);
		}
	});
}