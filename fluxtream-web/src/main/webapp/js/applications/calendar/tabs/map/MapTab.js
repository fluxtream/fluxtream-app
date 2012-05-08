define(["applications/calendar/tabs/Tab",
        "applications/calendar/App",
       "applications/calendar/tabs/map/MapConfig"], function(Tab, Calendar, Config) {

	var map = null;
    var infoWindow = null;
    var currentHighlightedLine = null;
    var config = null;
    var connectorSelected = null;
    var gpsLine = null;
	
	function render(digest, timeUnit) {
		this.getTemplate("text!applications/calendar/tabs/map/map.html", "map", function(){setup(digest);});
	}
	
	function setup(digest) {
        $("#tooltips").load("/calendar/tooltips");
		App.fullHeight();
        currentHighlightedLine = null;
        connectorSelected = null;
        gpsLine= null;
        dataMarkers = [];
        config = Config.getConfig();
        if (digest!=null && digest.cachedData!=null &&
            typeof(digest.cachedData.google_latitude)!="undefined"
            && digest.cachedData.google_latitude !=null &&
            digest.cachedData.google_latitude.length>0) { //make sure gps data is available before showing the map
            if ($("#the_map > .emptyList").length>0)
                $("#the_map").empty();
            $("#selectedConnectors").empty();
            var myOptions = {
                zoom : 11,
                scrollwheel : true,
                streetViewControl : false,
                mapTypeId : google.maps.MapTypeId.ROADMAP
            };
            map = new google.maps.Map(document.getElementById("the_map"),
                myOptions);
            infoWindow = new google.maps.InfoWindow();
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
            gpsLine = new google.maps.Polyline({map:map, path:myLatLngs});


            var checkedContainer = $("#selectedConnectors");
            for(var objectTypeName in digest.cachedData) {
                if (digest.cachedData[objectTypeName]==null||typeof(digest.cachedData[objectTypeName])=="undefined")
                    continue;
                var markerArray = addDataToMap(digest.cachedData[objectTypeName], objectTypeName, myLatLngs, associatedTimes);
                if (markerArray != null || objectTypeName == "google_latitude"){
                    var button = $('<button class="btnList btn btnListChecked enabled">' + objectTypeName + '</button>');
                    button.click({button:button,markerArray:markerArray,objectTypeName:objectTypeName,isGoogleLatitude:objectTypeName == "google_latitude"},function(event){
                        buttonClicked(event.data.button,event.data.markerArray,event.data.objectTypeName,event.data.isGoogleLatitude);
                    });
                    checkedContainer.append(button);
                    checkedContainer.append("&nbsp;");
                }
            }
        } else {
            $("#the_map").empty();
            $("#selectedConnectors").empty();
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
        var category;
        switch (connectorInfoId){
            case "sms_backup-sms":
            case "sms_backup-call_Calendar":
            case "twitter-dm":
            case "twitter-tweet":
            case "twitter-mention":
                category = config.SOCIAL_CATEGORY;
                break;
            case "google_calendar":
            case "toodledo-task":
                category = config.MIND_CATEGORY;
                break;
            case "fitbit-sleep":
            case "withings-bpm":
                category = config.BODY_CATEGORY;
                break;
            case "picasa":
            case "flickr":
            case "lastfm-recent_track":
                category = config.MEDIA_CATEGORY;
                break;
            default:
                return null;
        }
        return addItemsToMap(connectorData,latLngs,timestamps,category);
    }

    function addItemsToMap(items,latlngs,timestamps,category){
        var markerArray = new Array();
        for (var i = 0; i < items.length; i++){
            var startTimestamp = items[i].start;
            var endTimestamp = items[i].end;
            if (startTimestamp > timestamps[timestamps.length - 1] || (endTimestamp == null && startTimestamp < timestamps[0]))
                continue;

            var startFinishIndex;
            for (startFinishIndex = 0; startFinishIndex < timestamps.length && timestamps[startFinishIndex] < startTimestamp; startFinishIndex++);
            var startBeginIndex = startFinishIndex - 1;

            var percentThrough = (startTimestamp - timestamps[startBeginIndex]) / (timestamps[startFinishIndex] - timestamps[startBeginIndex]);
            var lat = (latlngs[startFinishIndex].lat() - latlngs[startBeginIndex].lat()) * percentThrough + latlngs[startBeginIndex].lat();
            var lon = (latlngs[startFinishIndex].lng() - latlngs[startBeginIndex].lng()) * percentThrough + latlngs[startBeginIndex].lng();
            var startLatLng = new google.maps.LatLng(lat,lon);

            if (endTimestamp == null){
                markerArray[markerArray.length] = addItemToMap(items[i],startLatLng,null,null,null,null,category);
            }
            else{
                var endFinishIndex, endBeginIndex;
                if (endTimestamp > timestamps[timestamps.length - 1]){
                    endFinishIndex = endBeginIndex = timestamps.length - 1;
                }
                else{
                    for (endFinishIndex = 0; endFinishIndex < timestamps.length && timestamps[endFinishIndex] < endTimestamp; endFinishIndex++);
                    endBeginIndex = endFinishIndex - 1;
                }
                var endLatLng;
                if (endFinishIndex == endBeginIndex)
                    endLatLng = latlngs[endFinishIndex];
                else{
                    percentThrough = (endTimestamp - timestamps[endBeginIndex]) / (timestamps[endFinishIndex] - timestamps[endBeginIndex]);
                    lat = (latlngs[endFinishIndex].lat() - latlngs[endBeginIndex].lat()) * percentThrough + latlngs[endBeginIndex].lat();
                    lon = (latlngs[endFinishIndex].lng() - latlngs[endBeginIndex].lng()) * percentThrough + latlngs[endBeginIndex].lng();
                    endLatLng = new google.maps.LatLng(lat,lon);
                }
                markerArray[markerArray.length] = addItemToMap(items[i],startLatLng,startFinishIndex,endLatLng,endBeginIndex,latlngs,category);
            }
        }
        if (markerArray.length == 0)
            markerArray = null;
        return markerArray;
    }

    function addItemToMap(item,startLatLng,startIndex,endLatLng,endIndex,latlngs,category){
        var marker = new google.maps.Marker({map:map, position:startLatLng, icon:category.icon, shadow:category.shadow});
        google.maps.event.addListener(marker, "click", function(){
            connectorSelected = item.type;
            var tooltip = $("#" + item.type + "_" + item.id).html();
            if (tooltip == null)
                tooltip = "no description available";
            infoWindow.setContent(tooltip);
            infoWindow.open(map,marker);
            if (currentHighlightedLine != null){
                currentHighlightedLine.setMap(null);
                currentHighlightedLine = null
            }
            if (latlngs != null){
                var newlatlngs = Array();
                newlatlngs[0] = startLatLng;
                for (var i = 0; startIndex + i <= endIndex; i++){
                    newlatlngs[i+1] = latlngs[startIndex + i];
                }
                newlatlngs[newlatlngs.length] = endLatLng;

                currentHighlightedLine = new google.maps.Polyline({map: gpsLine.getMap(), strokeColor:"orange", path: newlatlngs, zIndex: 100});
            }
        });
        return marker;
    }

    function buttonClicked(button,markers,connectorName,isGoogleLatitude){
        if (button.hasClass("disabled"))
            return;
        button.removeClass("enabled");
        button.addClass("disabled");
        if (button.hasClass("btnListChecked")){
            button.removeClass("btnListChecked");
            button.addClass("btn-inverse");
            if (connectorSelected === connectorName){
                connectorSelected = null;
                if (currentHighlightedLine != null){
                    currentHighlightedLine.setMap(null);
                    currentHighlightedLine = null;
                }
                infoWindow.close();
            }
            if (isGoogleLatitude){
                gpsLine.setMap(null);
                if (currentHighlightedLine != null)
                    currentHighlightedLine.setMap(null);
            }
            else{
                for (var i = 0; i < markers.length; i++){
                    markers[i].setMap(null);
                }
            }
        }
        else{
            button.removeClass("btn-inverse");
            button.addClass("btnListChecked");
            if (isGoogleLatitude){
                gpsLine.setMap(map);
                if (currentHighlightedLine != null)
                    currentHighlightedLine.setMap(map);
            }
            else{
                for (var i = 0; i < markers.length; i++){
                    markers[i].setMap(map);
                }
            }
        }

        button.removeClass("disabled");
        button.addClass("enabled");
    }

	var mapTab = new Tab("map", "Candide Kemmler", "icon-map-marker", true);
	mapTab.render = render;
	return mapTab;

});
