
function setMapPosition(pos_x, pos_y, zoomLevel) {
	var center = new google.maps.LatLng(pos_x, pos_y);
	FlxState.map.setCenter(center);
	FlxState.map.setZoom(zoomLevel);
	if (FlxState.positionMarker!=null)
		FlxState.positionMarker.setMap(null);
	FlxState.positionMarker = new google.maps.Marker({
	    position: center,
	    title: "Me",
	    map: FlxState.map
	});
	FlxState.positionMarker.setZIndex(20);
	showHomeOnMap(false);
}

function drawLatitudeInfo() {
	if (FlxState.locationHistory==null) return;
	if (typeof(FlxState.locationHistory)=="undefined") return;
	if (FlxState.locationHistory.length==0) return;
	if (FlxState.vtype=="CLOCK") {
		if (!FlxState.homeAddress||!FlxState.homeAddress.isSet)
			showInCategory(FlxState.locationHistory, FlxState.AT_HOME_CATEGORY);
		else {
			locationBreakdown(FlxState.locationHistory);
		}
	}
}

function locationBreakdown(positions) {
	var pos1 = new google.maps.LatLng(FlxState.homeAddress.latitude, FlxState.homeAddress.longitude),
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
		FlxState.traveling = true;
	} else {
		FlxState.traveling = false;
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

function showHomeOnMap(andCenterIt) {
	if (FlxState.homeAddress&&typeof(FlxState.homeAddress.latitude)!="undefined") {
		var home = new google.maps.LatLng(FlxState.homeAddress.latitude,
				FlxState.homeAddress.longitude);
		if (FlxState.homeMarker!=null)
			FlxState.homeMarker.setMap(null);
		if (andCenterIt)
			FlxState.map.setCenter(home);
		FlxState.homeMarker = new google.maps.Marker({
			icon: flxHomeImage,
			shadow: flxHomeShadow,
			shape: flxHomeShape,
		    map: FlxState.map,
			title: FlxState.homeAddress.address,
		    zIndexProcess: function() { return 5; },
			position: home
		});	
		FlxState.homeMarker.setZIndex(10);
	}
}

function drawFirstLocation() {
	showMap();
	getWeather();
	$("#geotime").empty();
	$(".sideBarBlock.mapSbar").show();
	if (FlxState.cities&&FlxState.cities.length>0) {
	    geocoder = new google.maps.Geocoder();
	    var cityAddress = cityLabel(FlxState.cities[0]);
	    geocoder.geocode({'address': cityAddress}, function(results, status) {
	    	if (status == google.maps.GeocoderStatus.OK) {
	    		var ne = results[0].geometry.viewport.getNorthEast();
	            var sw = results[0].geometry.viewport.getSouthWest();
	            FlxState.map.fitBounds(results[0].geometry.viewport);               
	    	} else {
	    		hideMap();
	    	}
	    });
	} else
		hideMap();
}

function getEventLocation(startMinute) {
	if (typeof(FlxState.locationHistory)=="undefined"||FlxState.locationHistory==null)
		return;
	var history = FlxState.locationHistory,
		lastSeenLocation = history[history.length-1];
	for(var i=0; i<history.length; i++) {
		var nextSeenLocation = history[i];
		if (nextSeenLocation.startMinute>startMinute) break;
		lastSeenLocation = nextSeenLocation;
	}
	return lastSeenLocation;
}

function showEventLocation(startMinute) {
	var locationInfo = getEventLocation(startMinute); 
	if (locationInfo==null||typeof(locationInfo)=="undefined") return;
	if (typeof(locationInfo.position)=="undefined") return;
	var position = locationInfo.position;
	$("#geotime").html("<span class=\"firstSeenLabel\">Last " +
			"seen here&nbsp;</span><span class=\"firstSeenTime\">" +
			locationInfo.time + "</span>");
	setTimeout("setMapPosition("+position[0]+", " + position[1]+ ", " + "11)", 50);
}