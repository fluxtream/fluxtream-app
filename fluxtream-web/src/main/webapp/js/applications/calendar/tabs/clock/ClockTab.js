define(["applications/calendar/tabs/clock/ClockDrawingUtils",
        "applications/calendar/tabs/clock/ClockConfig",
        "core/Tab",
        "applications/calendar/App",
        "applications/calendar/tabs/map/MapUtils",
        "App",
        "applications/calendar/tabs/photos/PhotoUtils",
        "libs/moves-colorcodes"], function(DrawingUtils, Config, Tab, Calendar, MapUtils, App, PhotoUtils, MovesColors) {
	
	var paper = null;
	var config = null;
    var map = null;
    var solarInfo = null;
    var tempratureUnit = null;
    var distanceUnit = null;
    var dayStart, dayEnd;
    var clockCircleElements = {};
    var selectedConnectors;
    var connectorEnabled;
    var dgst;
    var currentFacetTooltip = null;

    var tooltipTemplate;


    var lastTimestamp = null;

    var setTabParam;

    App.addHideTooltipListener(hideEventInfo);

	function render(params) {
        setTabParam = params.setTabParam;
        setTabParam(null);
        if (params.digest.delta && params.facetToShow == null)
            params.facetToShow = currentFacetTooltip;
        hideEventInfo();
        this.getTemplate("text!applications/calendar/tabs/clock/clock.html", "clock", function() {
            if (lastTimestamp == params.digest.generationTimestamp && !params.forceReload){
                displayFacet(params.facetToShow);
                params.doneLoading();
                return;
            }
            else
                lastTimestamp = params.digest.generationTimestamp;
            App.loadMustacheTemplate("applications/calendar/tabs/clock/clockTemplate.html","tooltip",function(template){
                tooltipTemplate = template;
                setup(params.digest, params.timeUnit, params.connectorEnabled);
                displayFacet(params.facetToShow);
                params.doneLoading();
            });
		});
	}

    function displayFacet(facet){
        if (facet == null)
            return;

        var element = $(paper.canvas).find("." + facet.type + "-" + facet.id);
        if (element.length > 0){
            element.click();
        }
        else{
            console.warn("couldn't find span for item!");
            console.warn(facet);
        }
    }

    function setup(digest, timeUnit, cEn) {
        dgst = digest;
        selectedConnectors = digest.selectedConnectors;
        connectorEnabled = cEn;
        solarInfo = digest.metadata.solarInfo;
        tempratureUnit = digest.settings.temperatureUnit;
        distanceUnit = digest.settings.distanceUnit;
        dayStart = digest.tbounds.start;
        dayEnd = digest.tbounds.end;
        if (map == null)
            map = MapUtils.newMap(new google.maps.LatLng(0,0),16,"clockMap",true);
        else
            map.reset();

        if (digest.facets != null && digest.facets["google_latitude-location"] != null)
            map.addGPSData(digest.facets["google_latitude-location"],App.getFacetConfig("google_latitude-location"),false);
        for (var objectType in digest.facets){
            if (objectType == "google_latitude-location")
                continue;//we already showed google latitude data if it existed
            map.addGPSData(digest.facets[objectType],App.getFacetConfig(objectType),false)
        }
        map.fitBounds(map.gpsBounds);
        /*if (digest.facets != null && digest.facets["google_latitude-location"] != null){
            map.addGPSData(digest.facets["google_latitude-location"],false);

            map.fitBounds(map.gpsBounds);
        }
        else{
            var addressToUse = {latitude:0,longitude:0};
            if (digest.addresses.ADDRESS_HOME != null && digest.addresses.ADDRESS_HOME.length != 0)
                addressToUse = digest.addresses.ADDRESS_HOME[0];
            map.setCenter(new google.maps.LatLng(addressToUse.latitude,addressToUse.longitude));
            hideQTipMap();
        }      */
        map.addAddresses(digest.addresses, false);

		var availableWidth = $("#clockTab").width();
		var edgeWidth =  Math.min(availableWidth, 600);
		$("#clockTab div:first-child").width(edgeWidth+"px");
		$("#paper").empty();
		$("#paper").width(edgeWidth);
		paper = Raphael("paper", edgeWidth, edgeWidth);
		config = Config.getConfig(edgeWidth, digest.tbounds.start, digest.tbounds.end);
		var drawingUtils = DrawingUtils.getDrawingUtils(config);
		config.clockCircles = paper.set();

        config.BODY_CATEGORY.orbit *= config.ORBIT_RATIO;
        config.AT_HOME_CATEGORY.orbit *= config.ORBIT_RATIO;
        config.OUTSIDE_CATEGORY.orbit *= config.ORBIT_RATIO;
        config.MIND_CATEGORY.orbit *= config.ORBIT_RATIO;
        config.SOCIAL_CATEGORY.orbit *= config.ORBIT_RATIO;
        config.MEDIA_CATEGORY.orbit *= config.ORBIT_RATIO;

		drawingUtils.paintCircle(paper, config.BODY_CATEGORY.orbit, "#ffffff", 1);
		drawingUtils.paintCircle(paper, config.AT_HOME_CATEGORY.orbit, "#ffffff", 1);
		drawingUtils.paintCircle(paper, config.OUTSIDE_CATEGORY.orbit, "#ffffff", 1);
		drawingUtils.paintCircle(paper, config.MIND_CATEGORY.orbit, "#ffffff", 1);
		drawingUtils.paintCircle(paper, config.SOCIAL_CATEGORY.orbit, "#ffffff", 1);
		drawingUtils.paintCircle(paper, config.MEDIA_CATEGORY.orbit, "#ffffff", 1);
		paintSolarInfo(digest.metadata.solarInfo);
		for(var objectTypeName in digest.facets) {
			if (digest.facets[objectTypeName]==null||typeof(digest.facets[objectTypeName])=="undefined")
				continue;
			updateDataDisplay(digest.facets[objectTypeName], objectTypeName, digest);
		}

        var hasData = $(paper.canvas).find(".facet").length != 0;
        $("#clockTab .noDataOverlay").css("display", hasData ? "none" : "block");
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
				var coords = fillRegion(config.CLOCK_CENTER, config.BODY_CATEGORY.orbit * config.ORBIT_RATIO -15, config.MEDIA_CATEGORY.orbit+15, startAngle, midAngle);
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
        var facetConfig = App.getFacetConfig(connectorInfoId);
        if (facetConfig.clock == null)
            return;
        if (facetConfig.gps){
            locationBreakdown(connectorData,digest);
        }
        else{
            facetConfig.clock.orbit *= config.ORBIT_RATIO;
            drawTimedData(connectorData,facetConfig.clock);
        }
	}

	function drawTimedData(payload, category) {
		if ((typeof(payload)!="undefined")&&payload!=null) {
            if (typeof(payload[0])=="undefined") {
                console.log("warning: null payload");
                console.log(payload);
                return;
            }
            if (typeof(payload.length)!="undefined"&&payload.length>0&&payload[0].type==="moves-move") {
                for (var i=0; i<payload.length; i++)
                    drawEvents(payload[i].activities, category.orbit);
            } else
    			drawEvents(payload, category.orbit);
        }
	}

	function drawEvents(items, orbit) {
		if (typeof(items)=="undefined") return;
		for (var i = 0; i < items.length; i++) {
			try {
				var item = items[i];
                if (item.type==="google_calendar-entry"&&item.allDay)
                    continue;
                var color = getItemColor(item);
                var strokeWidth = getStrokeWidth(item);
                var strokeCap = getStrokeCap(item);
                var outline = App.getFacetConfig(item.type).clockOutline;
                var dayStart = dgst.metadata.mainCity.dayStart;
                var dayEnd = dgst.metadata.mainCity.dayEnd;

                if (typeof(item.localTime)!="undefined"&&item.localTime) {
                    if (item.startMinute>item.endMinute)
                        item.startMinute = 0;
                } else {
                    if (item.start <= dayStart)
                        item.startMinute = 0;
                    if (item.end >= dayEnd)
                        item.endMinute = 1440;
                    while (item.endMinute > 1440)
                        item.endMinute -= 1440;
                }

				config.clockCircles.push(
					function() {
						var start = item.startMinute;
						var end = item.endMinute, instantWidth = 4;
						if (orbit===config.BODY_CATEGORY.orbit)
							instantWidth=18;
						//if (start>end) { start = 0; }
						var instantaneous = typeof(item.endMinute)=="undefined"||item.endMinute===item.startMinute,
                            span;
						if (instantaneous){
                            end = start+instantWidth;
                        }
                        span = paintSpan(paper, start, end, orbit, color, .9, strokeWidth, strokeCap, outline);
						span.node.item = item;
                        $(span.node).attr("class", item.type + "-" + item.id + " facet");
                        $(span.node).attr("notthide",true);
						$(span.node).css("cursor", "pointer");
						$(span.node).click({instantaneous:instantaneous}, function(event) {
                            if (typeof(event.offsetX) == "undefined"){
                                if (typeof(event.originalEvent) == "undefined" || typeof(event.originalEvent.layerX) == "undefined"){
                                    var middleTime = (start + end) / 2;
                                    var elementPosition = toCoords(config.CLOCK_CENTER,orbit,middleTime / config.RATIO + config.START_AT)
                                    event.offsetX = elementPosition[0];
                                    event.offsetY = elementPosition[1];
                                }
                                else{
                                    event.offsetX = event.originalEvent.layerX;
                                    event.offsetY = event.originalEvent.layerY;
                                }
                            }
                            if (!event.data.instantaneous)
                                event.timeTarget = getTimestampForPoint(event.offsetX,event.offsetY);
                            else
                                event.timeTarget = event.target.item.start;
                            event.minuteOfDay = getMinuteOfDay(event.timeTarget);
							showEventInfo(event);
						});
                        if (clockCircleElements[item.type] == null)
                            clockCircleElements[item.type] = [];
                        clockCircleElements[item.type][clockCircleElements[item.type].length] = span.node;
                        //we only want to consider the main type and the facet type, not the facet subtypes present in moves-move for example
                        var itemTypeSearch = item.type.split("-").slice(0,2).join("-");
                        for (var i = 0; i < selectedConnectors.length; i++){
                            var found = false;
                            for (var j = 0; !found && j < selectedConnectors[i].facetTypes.length; j++){

                                found = itemTypeSearch == selectedConnectors[i].facetTypes[j];
                            }
                            if (found){
                                span.node.style.display = connectorEnabled[selectedConnectors[i].connectorName] ? "inline" : "none";
                                if (typeof (span.node.outline)!="undefined") {
                                    span.node.outline.node.style.display = connectorEnabled[selectedConnectors[i].connectorName] ? "inline" : "none";
                                }
                                break;
                            }
                        }
						return span;
					}()
				);
			} catch (e) {
				if (typeof(console)!="undefined"&&console.log)
					console.log("there was an error parsing this json: " + e);
			}
		}
	}

    function getStrokeCap(item) {
        if (item.type==="moves-place")
            return "butt";
        else
            return "butt";
    }

    function getStrokeWidth(item) {
        if (item.type==="moves-move-activity")
            return config.STROKE_WIDTH/3;
        else
            return config.STROKE_WIDTH;
    }

    function getItemColor(item) {
        if (item.color)
            return item.color;
        if (item.type==="moves-place")
            return config.MOVES_PLACE_COLOR;
        else if (item.type==="moves-move-activity") {
            for (var i=0;i<MovesColors.activities.length;i++) {
                if (item.activity.toLowerCase()===MovesColors.activities[i].group||
                    item.activity.toLowerCase()===MovesColors.activities[i].activity){
                    return "#"+MovesColors.activities[i].color;
                }
            }
            return "#000";
        } else
            return App.getConnectorConfig(App.getFacetConnector(item.type)).color;
    }
	
	var ttpdiv = null, lastHoveredEvent, timeout = null, markers = new Array();
	
	function showEventInfo(event) {
        hideEventInfo();
		lastHoveredEvent = event;
		var span = event.target;
		var facet = span.item;
		if (facet.type=="google_latitude-location")
			return;
        if (facet.type==="moves-move-activity") {
            console.log("moves-move-activity");
        }
        //var target = $(event.target).parent().position();
        var target = {top:0, left: 0}; //hacky fix since the SVG positioning seems to be unreliable on different browsers
        var tip_y = target.top + event.offsetY;
        var tip_x = target.left + event.offsetX;
        var offsetX = config.CLOCK_CENTER[0] - event.offsetX;
        var offsetY = config.CLOCK_CENTER[1] - event.offsetY;
        map.executeAfterReady(function(){
            hideEventInfo();
            markers[0] = map.addItem(span.item,false);
            if (markers[0] != null){
                markers[0].doHighlighting();
                markers[0].showCircle();
                map.zoomOnMarker(markers[0]);
                //markers[0].hideMarker();
                /* markers[1] = new google.maps.Marker({map:map, position:map.getLatLngOnGPSLine(event.timeTarget),
                 icon:markers[0].getIcon(),shadow:markers[0].getShadow(),clickable:false});
                 map.enhanceMarker(markers[1],event.timeTarget);
                 markers[1].showCircle();
                 map.zoomOnMarker(markers[1]); */
            }

            var contents = facet.getDetails();

            showToolTip(tip_x,tip_y,offsetX,offsetY,contents,event.minuteOfDay,$(event.target).attr("stroke"),$(event.target).parent().parent(),
                markers[0] == null ? null : markers[0].getPosition(),App.getFacetConfig(facet.type).device_name,App.getFacetConfig(facet.type).channel_name,facet,false);
        });
	}

    function showToolTip(x,y, offX, offY,contents,minute,color,parent,gpsPos,sourceName, channelName,facet,locationInfo){
        currentFacetTooltip = facet;
        var weatherInfo = getWeatherData(minute);
        var weatherIcon;
        if (solarInfo != null && (minute < solarInfo.sunrise || minute > solarInfo.sunset)){//night
            weatherIcon =  weatherInfo == null ? "images/clear.gif" : weatherInfo.weatherIconUrlNight;
        }
        else{//day
            weatherIcon = weatherInfo == null ? "images/clear.gif" : weatherInfo.weatherIconUrlDay;
        }
        weatherIcon = "/" + FLX_RELEASE_NUMBER + "/" + weatherIcon;
        var orientation, tailOrientation;
        var angle = App.toPolar([0,0],offX,offY)[1];
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
        ttpdiv = $(tooltipTemplate.render({
            weatherDescription: weatherInfo == null ? "no weather info available" : weatherInfo.weatherDesc,
            temperature: weatherInfo == null ? "?" : tempratureUnit === "FAHRENHEIT" ? weatherInfo.tempF : weatherInfo.tempC,
            temperatureUnit: tempratureUnit === "FAHRENHEIT" ? "F" : "C",
            windSpeed: weatherInfo == null ? "?" : distanceUnit == "SI" ? weatherInfo.windspeedKmph : weatherInfo.windspeedMiles,
            windSpeedUnit: distanceUnit == "SI" ? "km/h" : "mph",
            humidity: weatherInfo == null ? "?" : weatherInfo.humidity,
            precipitation: weatherInfo == null ? "?" : weatherInfo.precipMM,
            precipitationUnit: "mm",
            weatherIcon: weatherIcon,
            orientation:orientation,
            oppositeOrientation:tailOrientation,
            color:color,
            time: App.formatMinuteOfDay(minute)[0],
            ampm: App.formatMinuteOfDay(minute)[1],
            tooltipData:contents.outerHTML(),
            //showBodyTrackLinks: sourceName != null,
            showBodyTrackLinks: false,
            showCommentEdit: locationInfo != null
        }));
        ttpdiv.css("position","absolute");
        ttpdiv.css("zIndex","100");
        var tail = ttpdiv.find(".flx-toolTipTail-" + orientation);
        parent.append(ttpdiv);

        var repositionTooltip = function(){
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

        }



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

        repositionTooltip();

        ttpdiv.imagesLoaded(function(){
            repositionTooltip();
        });

        $("#tootltipLoadList").click(function(event){
            event.preventDefault();
            $(".calendar-list-tab").click();
        });

        ttpdiv.find("#tooltipLoadTimeLine").click(function(event){
            setTabParam(facet.start);
            $(".calendar-timeline-tab").click();
            return false;
        });

       ttpdiv.find("#tooltipLoadBodyTrack").click(function(event){
            event.preventDefault();
            App.renderApp('bodytrack','grapher', {
                cursorPos: facet.start / 1000,
                rebuildURL: true,
                channelAdd: sourceName + "." + channelName,
                tbounds: dgst.tbounds
            });
        });

        contents.on("contentchange",function(){
            var classString = contents.attr("class");
            if (classString == null)
                classString = "";
            var classes = classString.split(" ");
            var classString = "";
            for (var i = 0, li = classes.length; i < li; i++){
                classString += "." + classes[i];
            }
            ttpdiv.find(classString).outerHTML(contents.outerHTML());

            var commentEdit =  ttpdiv.find(".facet-edit a");

            commentEdit.css("display","none");
            ttpdiv.find("#tooltipEditComment").unbind('click').click(function(){
                event.preventDefault();
                commentEdit.click();
            });

            App.apps.calendar.rebindDetailsControls(ttpdiv,dgst.facets);

            ttpdiv.find(".flx-photo").click(function(event){
                var dTarget = $(event.delegateTarget);
                PhotoUtils.showPhotoDialog(dTarget.attr("data-deviceName"), dTarget.attr("data-channelName"),
                                           dTarget.attr("data-id"), dTarget.attr("data-timestamp"),{minTime:dgst.tbounds.start,maxTime:dgst.tbounds.end});
            });

        });

       ttpdiv.find("#tooltipLoadBodyTrack").click(function(event){
            event.preventDefault();
            App.renderApp('bodytrack','grapher', {
                cursorPos: facet.start / 1000,
                rebuildURL: true,
                channelAdd: sourceName + "." + channelName,
                tbounds: dgst.tbounds
            });
        });

        contents.trigger("contentchange");

    }

    function getWeatherData(minuteOfDay){
        if (Calendar.weather == null || Calendar.weather.hourlyWeatherData == null)
            return null;
        var i;
        var wwoMinuteOfDay = minuteOfDay/60*100;
        for (i = 0; i < Calendar.weather.hourlyWeatherData.length; i++) {
            if(Calendar.weather.hourlyWeatherData[i].minuteOfDay > wwoMinuteOfDay) {
                break;
            }
        }
        if (i>0) i -= 1;
        var weatherInfo = Calendar.weather.hourlyWeatherData[i];
        return weatherInfo;
    }

	function hideEventInfo() {
        currentFacetTooltip = null;
        if (map != null){
            map.executeAfterReady(function(){
                hideQTipMap();
                for (var i = 0; i < markers.length; i++){
                    if (markers[i] != null)
                        markers[i].setMap(null);
                }
                markers = new Array();
                map.fitBounds(map.gpsBounds);
                map.highlightTimespan(dayStart,dayEnd);
            });
        }
        if (ttpdiv != null){
            ttpdiv.remove();
            ttpdiv = null;
        }
	}

	function arc(center, radius, startAngle, endAngle) {
		var angle = startAngle,
			coords = toCoords(center, radius, angle),
			path = "M " + coords[0] + " " + coords[1];
        if (angle > endAngle){
            angle -= 360;
        }
		while (angle <= endAngle) {
			coords = toCoords(center, radius, angle);
			path += " L " + coords[0] + " " + coords[1];
			angle += 1;
		}
        coords = toCoords(center, radius, endAngle);//ensure we go all the way to the end angle and not short a bit
        path += " L " + coords[0] + " " + coords[1];
		return path;
	}

	function toCoords(center, radius, angle) {
		var radians = (angle / 180) * Math.PI,
			x = center[0] + Math.cos(radians) * radius,
			y = center[1] + Math.sin(radians) * radius;
		return [ x, y ];
	}
	
	function paintSpan(paper, startTime, endTime, radius, color, opacity, strokeWidth, strokeCap, outline) {
        var shadow;
        if (outline) {
            shadow = paintSpan(paper,startTime,endTime,radius,"black",1,strokeWidth,strokeCap,false);
            strokeWidth -= 1;
        }
        var degreesPerPixel = 360 / (Math.PI * 2 * radius);
        var start = startTime / config.RATIO + config.START_AT + (outline ? (degreesPerPixel * 0.5) : 0);
        var end =  endTime / config.RATIO + config.START_AT - (outline ? (degreesPerPixel * 0.5) : 0);
        if (end < start)
            end = start;
		var coords = arc(config.CLOCK_CENTER, radius, start, end),
		path = paper.path(coords);
        path.attr("stroke-linecap", strokeCap);
		path.attr("stroke-width", strokeWidth);
		path.attr("stroke", color);
		path.attr("opacity", opacity);
        if (outline)
            path.node.outline = shadow;
		return path;
	}

    function getTimestampForPoint(x,y){
        var angleClick = App.toPolar(config.CLOCK_CENTER,x,y)[1] - config.START_AT;
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

	function locationBreakdown(positions, digest) {
	}

    function hideQTipMap(){
        document.getElementById("clockMapContainer").appendChild(document.getElementById("clockMap"));
    }


    function qTipUpdate(){
        $("#mapPlaceHolder").append(document.getElementById("clockMap"));
    }

    function connectorToggled(connectorName,objectTypeNames,enabled){
        for (var i = 0; i < objectTypeNames.length; i++)
            toggleConnectorObjectType(objectTypeNames[i], enabled);
        if (connectorName==="moves")
            toggleConnectorObjectType("moves-move-activity", enabled);
        updateDataDisplay(dgst.facets["google_latitude-location"], "google_latitude-location", dgst);
    }

    function toggleConnectorObjectType(objectTypeName, enabled) {
        if (clockCircleElements[objectTypeName] == null)
            return;
        for (var j = 0; j < clockCircleElements[objectTypeName].length; j++) {
            clockCircleElements[objectTypeName][j].style.display = enabled ? "inline" : "none";
            if (typeof (clockCircleElements[objectTypeName][j].outline)!="undefined") {
                console.log("toggling outline");
                clockCircleElements[objectTypeName][j].outline.node.style.display = enabled ? "inline" : "none";
            }
        }
    }

    function connectorDisplayable(connector){
        for (var i = 0; i < connector.facetTypes.length; i++){
            var config = App.getFacetConfig(connector.facetTypes[i]);
            if (config.clock != null)
                return true;
        }
        return false;
    }

	var clockTab = new Tab("calendar", "clock", "Candide Kemmler", "icon-time", true);
    document.qTipUpdate = qTipUpdate;
    document.hideQTipMap = hideQTipMap;
	clockTab.render = render;
    clockTab.connectorToggled = connectorToggled;
    clockTab.connectorDisplayable = connectorDisplayable;
	return clockTab;
	
});
