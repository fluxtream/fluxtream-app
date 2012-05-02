define(["applications/calendar/tabs/Tab",
        "applications/calendar/App"], function(Tab, Calendar) {

	var map = null;
	
	function render(digest, timeUnit) {
		this.getTemplate("text!applications/calendar/tabs/map/map.html", "map", function(){setup(digest);});
	}
	
	function setup(digest) {
		App.fullHeight();
        if (digest!=null && digest.cachedData!=null &&
            typeof(digest.cachedData.google_latitude)!="undefined"
            && digest.cachedData.google_latitude !=null &&
            digest.cachedData.google_latitude.length>0) { //make sure gps data is available before showing the map
            if ($("#the_map > .emptyList").length>0)
                $("#the_map").empty();
            var myOptions = {
                zoom : 11,
                scrollwheel : true,
                streetViewControl : false,
                mapTypeId : google.maps.MapTypeId.ROADMAP
            };
            map = new google.maps.Map(document.getElementById("the_map"),
                myOptions);
            var myLatLngs=new Array();
            var associatedTimes=new Array();
            var averageLat = 0;
            var averageLon = 0;
            var minLat = 90; //initialized to the largest valid latitude
            var maxLat = 0; //initialized to the smallest valid latitude
            var minLon = 180; //initialized to the largest valid longitude
            var maxLon = -180; //initialized to the smallest valid longitude
            for (var i = 0; i < digest.cachedData.google_latitude.length; i++){
                var lat = digest.cachedData.google_latitude[i].position[0];
                var lon = digest.cachedData.google_latitude[i].position[1];
                myLatLngs[i] = new google.maps.LatLng(lat,lon);
                associatedTimes[i] = digest.cachedData.google_latitude[i].start;
                averageLat += (lat - averageLat) / (i + 1); //incremental average calculation
                averageLon += (lon - averageLon) / (i + 1); //incremental average calculation
                if (lat < minLat)
                    minLat = lat;
                if (lat > maxLat)
                    maxLat = lat;
                if (lon < minLon)
                    minLon = lon;
                if (lon > maxLon)
                    maxLon = lon;
            }
            setMapPosition(averageLat,averageLon, 9); //center the map to the average gps location
            //bound the map to the area which the gps data spans
            map.fitBounds(new google.maps.LatLngBounds(new google.maps.LatLng(minLat,minLon), new google.maps.LatLng(maxLat,maxLon)));
            new google.maps.Polyline({map:map, path:myLatLngs});
            for(var objectTypeName in digest.cachedData) {
                if (digest.cachedData[objectTypeName]==null||typeof(digest.cachedData[objectTypeName])=="undefined")
                    continue;
                addDataToMap(digest.cachedData[objectTypeName], objectTypeName, myLatLngs, associatedTimes);
            }
        } else {
            $("#the_map").empty();
            $("#the_map").removeAttr("style");
            $("#the_map").append("<div class=\"emptyList\">(no location data)</div>");
        }
	}
	
	function setMapPosition(pos_x, pos_y, zoomLevel) {
		var center = new google.maps.LatLng(pos_x, pos_y);
		map.setCenter(center);
		map.setZoom(zoomLevel);
	}

    function addDataToMap(connectorData, connectorInfoId, latLngs, timestamps){
        switch (connectorInfoId){
            case "lastfm-recent_track":
                addItemsToMap(connectorData,latLngs,timestamps)

        }
    }

    function addItemsToMap(items,latlngs,timestamps){
        for (var i = 0; i < items.length; i++){
            var timestamp = items[i].start;
            if (timestamp < timestamps[0] || timestamp > timestamps[timestamps.length - 1])
                continue;
            var endIndex;
            for (endIndex = 1; endIndex < timestamps.length; endIndex++){
                if (timestamps[endIndex] >= timestamp)
                    break;
            }
            var startIndex = endIndex - 1;
            if (startIndex < 0)
                startIndex = 0;
            if (startIndex == endIndex)
                addItemToMap(items[i],latlngs[startIndex]);
            else{
                var percentThrough = (timestamp - timestamps[startIndex]) / (timestamps[endIndex] - timestamps[startIndex]);
                var lat = (latlngs[endIndex].lat() - latlngs[startIndex].lat()) * percentThrough + latlngs[startIndex].lat();
                var lon = (latlngs[endIndex].lng() - latlngs[startIndex].lng()) * percentThrough + latlngs[startIndex].lng();
                addItemToMap(items[i],new google.maps.LatLng(lat,lon));
            }
        }
    }

    function addItemToMap(item,latlng){
        new google.maps.Marker({map:map, position:latlng});
    }

	var mapTab = new Tab("map", "Candide Kemmler", "icon-map-marker", true);
	mapTab.render = render;
	return mapTab;

});
