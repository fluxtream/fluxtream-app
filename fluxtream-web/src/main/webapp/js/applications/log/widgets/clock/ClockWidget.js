define(["applications/log/widgets/clock/ClockdrawingUtils",
        "applications/log/widgets/clock/ClockConfig"], function(DrawingUtils, Config) {
	
	var paper = null;
	var ClockWidget = Backbone.View.extend({
		
		el: $("#widgets"),
		
		initialize: function() {
			_.bindAll(this);
		},
		
		render : function(digest) {
			$("#widgets").append("<div id=\"clockWidget\"class=\"span12\"><div id=\"paper\"></div></div>");
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
			for(name in digest.cachedData) {
				if (digest.cachedData[name]==null||typeof(digest.cachedData[name])=="undefined")
					continue;
				updateDataDisplay(digest.cachedData[name], name, paper, config);
			}
		}

	});

	
	function updateDataDisplay(connectorData, connectorInfoId, paper, config) {
		switch(connectorInfoId) {
		case "fitbit-activity_summary":
//			drawFitbitInfo(connectorData);
			break;
		case "google_latitude":
//			FlxState.locationHistory = connectorData;
//			if (connectorData!=null&&typeof(connectorData)!="undefined")
//				drawLatitudeInfo();
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
						return span;
					}()
				);
			} catch (e) {
				if (typeof(console)!="undefined"&&console.log)
					console.log("there was an error parsing this json: " + e);
			}
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
	
	function paintSpan(paper, startTime, endTime, radius, color, opacity, config) {
		var coords = arc(config.CLOCK_CENTER, radius, startTime / config.RATIO + config.START_AT, endTime
				/ config.RATIO + config.START_AT),
		path = paper.path(coords);
		path.attr("stroke-width", config.STROKE_WIDTH)
		path.attr("stroke", color);
		path.attr("opacity", opacity);
		return path;
	}	
	
	var clockWidget = new ClockWidget();
	return clockWidget;
	
});
