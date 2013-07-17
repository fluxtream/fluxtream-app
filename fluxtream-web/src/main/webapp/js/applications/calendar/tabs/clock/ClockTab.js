define(["applications/calendar/tabs/clock/ClockDrawingUtils",
        "applications/calendar/tabs/clock/ClockConfig",
        "core/Tab",
        "applications/calendar/App",
        "applications/calendar/tabs/map/MapUtils",
        "App",
        "applications/calendar/tabs/photos/PhotoUtils"], function(DrawingUtils, Config, Tab, Log, MapUtils, App, PhotoUtils) {
	
	var paper = null;
	var config = null;
    var map = null;
    var hourlyWeatherData = null;
    var solarInfo = null;
    var tempratureUnit = null;
    var distanceUnit = null;
    var dayStart, dayEnd;
    var clockCircleElements = {};
    var selectedConnectors;
    var connectorEnabled;
    var dgst;


    var photoCarouselHTML;
    var lastTimestamp = null;

    var setTabParam;

    App.addHideTooltipListener(hideEventInfo);

	function render(params) {
        setTabParam = params.setTabParam;
        setTabParam(null);
        hideEventInfo();
        this.getTemplate("text!applications/calendar/tabs/clock/clock.html", "clock", function() {
            if (lastTimestamp == params.digest.generationTimestamp && !params.forceReload){
                params.doneLoading();
                return;
            }
            else
                lastTimestamp = params.digest.generationTimestamp;
			setup(params.digest, params.timeUnit, params.connectorEnabled, params.doneLoading);
            fetchWeatherData();
		});
	}

    function fetchWeatherData() {
        $.ajax({ url: "/api/calendar/weather/"+Log.tabState, dataType: "json",
            success: function(digest) {
                if (!outsideTimeBoundaries(digest)) {
                    hourlyWeatherData = digest.hourlyWeatherData;
                }
            }
        });
    }

    function setup(digest, timeUnit, cEn, doneLoading) {
        dgst = digest;
        selectedConnectors = digest.selectedConnectors;
        connectorEnabled = cEn;
        solarInfo = digest.solarInfo;
        tempratureUnit = digest.settings.temperatureUnit;
        distanceUnit = digest.settings.distanceUnit;
        dayStart = digest.tbounds.start;
        dayEnd = digest.tbounds.end;
        if (map == null)
            map = MapUtils.newMap(new google.maps.LatLng(0,0),16,"clockMap",true);
        else
            map.reset();

        if (digest.cachedData != null && digest.cachedData["google_latitude-location"] != null)
            map.addGPSData(digest.cachedData["google_latitude-location"],App.getFacetConfig("google_latitude-location"),false);
        for (var objectType in digest.cachedData){
            if (objectType == "google_latitude-location")
                continue;//we already showed google latitude data if it existed
            map.addGPSData(digest.cachedData[objectType],App.getFacetConfig(objectType),false)
        }
        map.fitBounds(map.gpsBounds);
        /*if (digest.cachedData != null && digest.cachedData["google_latitude-location"] != null){
            map.addGPSData(digest.cachedData["google_latitude-location"],false);

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
		paintSolarInfo(digest.solarInfo);
		for(var objectTypeName in digest.cachedData) {
			if (digest.cachedData[objectTypeName]==null||typeof(digest.cachedData[objectTypeName])=="undefined")
				continue;
			updateDataDisplay(digest.cachedData[objectTypeName], objectTypeName, digest);
		}

        photoCarouselHTML = PhotoUtils.getCarouselHTML(digest);

        doneLoading();
	}

	function outsideTimeBoundaries(o) {
		if (typeof(o.tbounds)!="undefined") {
			return (o.tbounds.start!=config.start || o.tbounds.end!=config.end);
		}
		return (o.start!=config.start || o.end!=config.end);
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
            if (typeof(payload.length)!="undefined"&&payload.length>0&&payload[0].type==="moves-move") {
                for (var i=0; i<payload.length; i++)
                    drawEvents(payload[i].activities, category.orbit);
            } else
    			drawEvents(payload, category.orbit);
        }
	}

	function drawEvents(items, orbit) {
		if (typeof(items)=="undefined") return;
		for (i = 0; i < items.length; i++) {
			try {
				var item = items[i];
                var color = getItemColor(item);
                var strokeWidth = getStrokeWidth(item);
                var strokeCap = getStrokeCap(item);
				config.clockCircles.push(
					function() {
						var start = item.startMinute;
						var end = item.endMinute, instantWidth = 2;
						if (orbit===config.BODY_CATEGORY.orbit)
							instantWidth=18;
						//if (start>end) { start = 0; }
						var instantaneous = typeof(item.endMinute)=="undefined"||item.endMinute===item.startMinute,
                            span;
						if (instantaneous)
							span = paintSpan(paper, start,start+instantWidth, orbit, color, .9, strokeWidth, strokeCap);
						else{
							span = paintSpan(paper, start,/*(start<=end?end:1440)*/ end, orbit, color, .9, strokeWidth, strokeCap);
                        }
						span.node.item = item;
                        $(span.node).attr("notthide",true);
						$(span.node).css("cursor", "pointer");
						$(span.node).click({instantaneous:instantaneous}, function(event) {
                            if (typeof(event.offsetX) == "undefined"){
                                event.offsetX = event.originalEvent.layerX;
                                event.offsetY = event.originalEvent.layerY;
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
                        for (var i = 0; i < selectedConnectors.length; i++){
                            var found = false;
                            for (var j = 0; !found && j < selectedConnectors[i].facetTypes.length; j++){
                                found = item.type == selectedConnectors[i].facetTypes[j];
                            }
                            if (found){
                                span.node.style.display = connectorEnabled[selectedConnectors[i].connectorName] ? "inline" : "none";
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
        if (item.type==="moves-place")
            return config.MOVES_PLACE_COLOR;
        else if (item.type==="moves-move-activity") {
            switch (item.activityCode) {
                case "cyc":
                    return config.MOVES_CYCLING_COLOR;
                case "wlk":
                    return config.MOVES_WALKING_COLOR;
                case "trp":
                    return config.MOVES_TRANSPORT_COLOR;
                case "run":
                    return config.MOVES_RUNNING_COLOR;
                default:
                    return "#000000";
            }
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
                    markers[0] == null ? null : markers[0].getPosition(),App.getFacetConfig(facet.type).device_name,App.getFacetConfig(facet.type).channel_name,facet);
	}

    function showToolTip(x,y, offX, offY,contents,minute,color,parent,gpsPos,sourceName, channelName,facet){
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
                showBodyTrackLinks: sourceName != null
            }));
            console.log(gpsPos)
            ttpdiv.css("position","absolute");
            ttpdiv.css("zIndex","100");
            var tail = ttpdiv.find(".flx-toolTipTail-" + orientation);
            parent.append(ttpdiv);

            // WIP: have the map take all the available space when there is no weather info
            //if (weatherInfo==null) {
            //    $(".flx-toolTipWeather").hide();
            //    $(".flx-toolTipLocation").css("width", "400px");
            //    $("#clockMapContainer").css("width", "400px");
            //    $("#clockMap").css("width", "400px");
            //} else {
            //    $(".flx-toolTipWeather").show();
            //    $(".flx-toolTipLocation").css("width", "50%");
            //    $("#clockMapContainer").css("width", "200px");
            //    $("#clockMap").css("width", "200px");
            //}

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
                displayY -= 50;
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


            ttpdiv.find(".flx-photo").click(function(event){
                App.makeModal(photoCarouselHTML);
                App.carousel($(event.delegateTarget).attr("photoId"));
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
                var classes = contents.attr("class").split(" ");
                var classString = "";
                for (var i = 0, li = classes.length; i < li; i++){
                    classString += "." + classes[i];
                }
                ttpdiv.find(classString).outerHTML(contents.outerHTML());
                ttpdiv.find(".mapLink").click(function(event){
                    setTabParam($(event.delegateTarget).attr("itemid"));
                    $(".calendar-map-tab").click();
                    return false;
                });

            });

            contents.trigger("contentchange");
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
        //var target = $(event.target).parent().position();
        var target = {top:0, left:0};
        var tip_y = target.top + event.offsetY;
        var tip_x = target.left + event.offsetX;
        var offsetX = config.CLOCK_CENTER[0] - event.offsetX;
        var offsetY = config.CLOCK_CENTER[1] - event.offsetY;

        if (map != null){
            map.highlightTimespan(span.item.start,span.item.end);

            markers[0] = new google.maps.Marker({map:map, position:map.getLatLngOnGPSLine(event.timeTarget),clickable:false});
            map.enhanceMarker(markers[0],event.timeTarget);
            markers[0].showCircle();
            if (span.item.start == span.item.end)
                map.zoomOnMarker(markers[0]);
            else
                map.zoomOnTimespan(span.item.start,span.item.end);
        }
        showToolTip(tip_x,tip_y,offsetX,offsetY,span.item.address == null ? $("<span>You were out</span>") : $("<span>You were at " + span.item.address.address + "</span>"),event.minuteOfDay,$(event.target).attr("stroke"),$(event.target).parent().parent(),
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
        if (ttpdiv != null){
            ttpdiv.remove();
            ttpdiv = null;
        }
	}

	function arc(center, radius, startAngle, endAngle) {
		if (endAngle - startAngle < 2 && endAngle - startAngle >= 0)
			endAngle += 1;
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
	
	function paintSpan(paper, startTime, endTime, radius, color, opacity, strokeWidth, strokeCap) {
		var coords = arc(config.CLOCK_CENTER, radius, startTime / config.RATIO + config.START_AT, endTime
				/ config.RATIO + config.START_AT),
		path = paper.path(coords);
        path.attr("stroke-linecap", strokeCap);
		path.attr("stroke-width", strokeWidth);
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

    function mergeCollection(collection){
        if (collection.lastMerge >= collection.length)
            return;
        var first = collection[collection.lastMerge];
        var last = collection[collection.length - 1];
        var mergedObject = {
            start:first.start,
            end:last.start,
            type:first.type,
            address:collection.address,
            startMinute:first.startMinute,
            endMinute:last.startMinute
        };
        collection.splice(collection.lastMerge,collection.length - collection.lastMerge,mergedObject);
        collection.lastMerge++;
    }

	function locationBreakdown(positions, digest) {
        if (!Log.connectorEnabled["clock"]["google_latitude"])
            return;
        var collections = [];
        var currentCollection = null;
        positions = MapUtils.filterGPSData(positions);
        for (var i = 0; i < positions.length; i++){
            if (positions[i].source == "OTHER")
                continue;
            var position = positions[i];
            var pos1 = new google.maps.LatLng(position.position[0],position.position[1]);
            var addressAt = null;
            var matchStrength = 0;
            for (var addressType in digest.addresses){
                for (var j = 0; j < digest.addresses[addressType].length; j++){
                    var address = digest.addresses[addressType][j];
                    var pos2 = new google.maps.LatLng(address.latitude,address.longitude);
                    var distance = google.maps.geometry.spherical.computeDistanceBetween(pos1,pos2);
                    var strength = distance - address.radius - position.accuracy;
                    if (strength < matchStrength){
                        matchStrength = strength;
                        addressAt = address;
                    }
                }
            }
            if (currentCollection == null || currentCollection.address != addressAt){
                if (currentCollection != null){
                    currentCollection[currentCollection.length] = position;
                    mergeCollection(currentCollection);
                }
                currentCollection = null;
                for (var j = 0; currentCollection == null && j < collections.length; j++){
                    if (collections[j].address == addressAt)
                        currentCollection = collections[j];
                }
                if (currentCollection == null){
                    currentCollection = [];
                    currentCollection.address = addressAt;
                    currentCollection.lastMerge = 0;
                    collections[collections.length] = currentCollection;
                }
            }
            currentCollection[currentCollection.length] = position;//add to the current collection

        }
        for (var i = 0; i < collections.length ; i++){
            mergeCollection(collections[i]);
            drawCollection(collections[i]);
        }
	}
    function drawCollection(collection){
        for (var i = 0; i < collection.length; i++){
            var timeSegment = collection[i];
            var start  = timeSegment.startMinute;
            var end = timeSegment.endMinute;
            var color;
            switch (collection.address == null ? "null" : collection.address.type){
                case "ADDRESS_HOME":
                    color = config.AT_HOME_CATEGORY.color;
                    break;
                case "ADDRESS_WORK":
                    color = config.AT_WORK_CATEGORY.color;
                    break;
                default:
                    color = config.OUTSIDE_CATEGORY.color;
                    break;
            }
            var span = paintSpan(paper, start,(start<=end?end:1440), config.AT_HOME_CATEGORY.orbit, color, 1, config.STROKE_WIDTH, "butt");
            span.node.item = timeSegment;
            $(span.node).attr("notthide",true);
            $(span.node).css("cursor", "pointer");
            $(span.node).click(function(event) {
                if (typeof(event.offsetX) == "undefined"){
                    event.offsetX = event.originalEvent.layerX;
                    event.offsetY = event.originalEvent.layerY;
                }
                event.timeTarget = getTimestampForPoint(event.offsetX,event.offsetY);
                if (event.timeTarget < event.target.item.start)
                    event.timeTarget = event.target.item.start;
                if (event.timeTarget > event.target.item.end)
                    event.timeTarget = event.target.item.end;
                event.minuteOfDay = getMinuteOfDay(event.timeTarget);
                showLocationBreakdownInfo(event);
            });

            if (clockCircleElements[span.node.item.type] == null)
                clockCircleElements[span.node.item.type] = [];
            clockCircleElements[span.node.item.type][clockCircleElements[span.node.item.type].length] = span.node;
            for (var j = 0; j < selectedConnectors.length; j++){
                var found = false;
                for (var k = 0; !found && k < selectedConnectors[j].facetTypes.length; k++){
                    found = span.node.item.type == selectedConnectors[j].facetTypes[k];
                }
                if (found){
                    if (typeof(connectorEnabled[selectedConnectors[i]])!="undefined")
                        span.node.style.display = connectorEnabled[selectedConnectors[i].connectorName] ? "inline" : "none";
                    break;
                }
            }
            config.clockCircles.push(span);
        }

    }

    function hideQTipMap(){
        document.getElementById("clockMapContainer").appendChild(document.getElementById("clockMap"));
    }


    function qTipUpdate(){
        $("#mapPlaceHolder").append(document.getElementById("clockMap"));
    }

    function connectorToggled(connectorName,objectTypeNames,enabled){
        for (var i = 0; i < objectTypeNames.length; i++){
            if (clockCircleElements[objectTypeNames[i]] == null)
                continue;
            for (var j = 0; j < clockCircleElements[objectTypeNames[i]].length; j++)
                clockCircleElements[objectTypeNames[i]][j].style.display = enabled ? "inline" : "none";
        }
        updateDataDisplay(dgst.cachedData["google_latitude-location"], "google_latitude-location", dgst);
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
