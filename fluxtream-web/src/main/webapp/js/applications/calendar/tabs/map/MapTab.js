define(["applications/calendar/tabs/Tab",
        "applications/calendar/App",
       "applications/calendar/tabs/map/MapConfig"], function(Tab, Calendar, Config) {

	var map = null;
    var infoWindow = null;
    var currentHighlightedLine = null;
    var config = null;
    var connectorSelected = null;
    var gpsLine = null;
    var gpsTimestamps = null;
    var gpsPositions = null;
    var highlightSection = null;

    function render(digest, timeUnit) {
        this.getTemplate("text!applications/calendar/tabs/map/map.html", "map", function(){setup(digest);});
    }

    function setup(digest) {
        $("#tooltips").load("/calendar/tooltips");
        App.fullHeight();
        currentHighlightedLine = null;
        connectorSelected = null;
        gpsLine= null;
        gpsTimestamps = null;
        gpsPositions = null;
        highlightSection = null;
        dataMarkers = [];
        config = Config.getConfig();
        $("#the_map").empty();
        $("#selectedConnectors").empty();
        $("#mapFit").unbind("click");
        var myOptions = {
            zoom : 8,
            scrollwheel : true,
            streetViewControl : false,
            mapTypeId : google.maps.MapTypeId.ROADMAP
        };
        if (digest.homeAddress != null && digest.homeAddress.isSet){
           myOptions.center = new google.maps.LatLng(digest.homeAddress.latitude,digest.homeAddress.longitude);
        }
        else{
            myOptions.center = new google.maps.LatLng(0,0);
        }

        map = new google.maps.Map(document.getElementById("the_map"), myOptions);
        if (digest!=null && digest.cachedData!=null &&
            typeof(digest.cachedData.google_latitude)!="undefined"
                && digest.cachedData.google_latitude !=null &&
            digest.cachedData.google_latitude.length>0) { //make sure gps data is available before trying to display it

            infoWindow = new google.maps.InfoWindow();
            gpsPositions =new Array();
            gpsTimestamps =new Array();
            var averageLat = 0;
            var averageLon = 0;
            var minLat = 90; //initialized to the largest valid latitude
            var maxLat = 0; //initialized to the smallest valid latitude
            var minLon = 180; //initialized to the largest valid longitude
            var maxLon = -180; //initialized to the smallest valid longitude
            for (var i = 0; i < digest.cachedData.google_latitude.length; i++){
                var lat = digest.cachedData.google_latitude[i].position[0];
                var lon = digest.cachedData.google_latitude[i].position[1];
                gpsPositions[i] = new google.maps.LatLng(lat,lon);
                gpsTimestamps[i] = digest.cachedData.google_latitude[i].start;
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
            //bound the map to the area which the gps data spans
            map.fitBounds(new google.maps.LatLngBounds(new google.maps.LatLng(minLat,minLon), new google.maps.LatLng(maxLat,maxLon)));
            gpsLine = new google.maps.Polyline({map:map, path:gpsPositions});

            var checkedContainer = $("#selectedConnectors");
            for(var objectTypeName in digest.cachedData) {
                if (digest.cachedData[objectTypeName]==null||typeof(digest.cachedData[objectTypeName])=="undefined")
                    continue;
                var markerArray = addDataToMap(digest.cachedData[objectTypeName], objectTypeName);
                if (markerArray != null || objectTypeName == "google_latitude"){
                    var button = $('<button class="btnList btn btnListChecked enabled">' + objectTypeName + '</button>');
                    button.click({button:button,markerArray:markerArray,objectTypeName:objectTypeName,isGoogleLatitude:objectTypeName == "google_latitude"},function(event){
                        buttonClicked(event.data.button,event.data.markerArray,event.data.objectTypeName,event.data.isGoogleLatitude);
                    });
                    checkedContainer.append(button);
                    checkedContainer.append("&nbsp;");
                }
            }

            $("#mapFit").show();
            $("#mapFit").click(function(){
                zoomOnTimespan(gpsTimestamps[0],gpsTimestamps[gpsTimestamps.length-1]);
            });

        } else {
            $("#mapFit").hide();
            $("#selectedConnectors").append("<div class=\"emptyList\">(no location data)</div>");
        }
	}

    function addDataToMap(connectorData, connectorInfoId){
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
        return addItemsToMap(connectorData,category);
    }

    function addItemsToMap(items,category){
        var markerArray = new Array();
        for (var i = 0; i < items.length; i++){
            var startTimestamp = items[i].start;
            var endTimestamp = items[i].end;
            if (startTimestamp > gpsTimestamps[gpsTimestamps.length - 1] || (endTimestamp == null && startTimestamp < gpsTimestamps[0]))
                continue;

            if (endTimestamp == null){
                markerArray[markerArray.length] = addItemToMap(items[i],getLatLngOnGPSLine(startTimestamp),null,null,null,category);
            }
            else{
                var endLatLng = getLatLngOnGPSLine(endTimestamp);
                markerArray[markerArray.length] = addItemToMap(items[i],getLatLngOnGPSLine(startTimestamp),getFirstIndexAfter(startTimestamp),endLatLng,getFirstIndexBefore(endTimestamp),category);
            }
        }
        if (markerArray.length == 0)
            markerArray = null;
        return markerArray;
    }

    function addItemToMap(item,startLatLng,startIndex,endLatLng,endIndex,category){
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
            if (endLatLng != null){
                var newlatlngs = Array();
                newlatlngs[0] = startLatLng;
                for (var i = 0; startIndex + i <= endIndex; i++){
                    newlatlngs[i+1] = gpsPositions[startIndex + i];
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
                if (highlightSection != null)
                    highlightSection.setMap(null);
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
                if (highlightSection != null)
                    highlightSection.setMap(map);
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

    function getLatLngOnGPSLine(time){
        if (time <= gpsTimestamps[0])
            return gpsPositions[0];
        if (time >= gpsTimestamps[gpsTimestamps.length - 1])
            return gpsPositions[gpsPositions.length-1];
        var endIndex;
        for (endIndex = 1; endIndex < gpsTimestamps.length && gpsTimestamps[endIndex] < time; endIndex++);
        var startIndex = endIndex - 1;
        var percentThrough = (time - gpsTimestamps[startIndex]) / (gpsTimestamps[endIndex] - gpsTimestamps[startIndex]);
        var lat = (gpsPositions[endIndex].lat() - gpsPositions[startIndex].lat()) * percentThrough + gpsPositions[startIndex].lat();
        var lon = (gpsPositions[endIndex].lng() - gpsPositions[startIndex].lng()) * percentThrough + gpsPositions[startIndex].lng();
        return new google.maps.LatLng(lat,lon);
    }

    function getFirstIndexAfter(time){
        var endIndex;
        for (endIndex = 0; endIndex < gpsTimestamps.length && gpsTimestamps[endIndex] < time; endIndex++);
        return endIndex;
    }

    function getFirstIndexBefore(time){
        if (time <= gpsTimestamps[0])
            return -1;
        var endIndex;
        for (endIndex = 1; endIndex < gpsTimestamps.length && gpsTimestamps[endIndex] < time; endIndex++);
        return endIndex - 1;
    }


    function highlightTimespan(start,end){
        if (highlightSection != null){
            highlightSection.setMap(null);
            highlightSection = null;
        }
        if (start <= gpsTimestamps[0] && end >= gpsTimestamps[gpsTimestamps.length]){
            gpsLine.setOptions({strokeColor: "black"});
            return;
        }
        gpsLine.setOptions({strokeColor: "grey"});
        var newPoints = new Array();
        newPoints[0] = getLatLngOnGPSLine(start);
        var startIndex = getFirstIndexAfter(start);
        var endIndex = getFirstIndexBefore(end);
        for (var i = 0; i + startIndex <= endIndex; i++){
            newPoints[i+1] = gpsPositions[i+startIndex];
        }
        newPoints[newPoints.length] = getLatLngOnGPSLine(end);
        highlightSection = new google.maps.Polyline({map: gpsLine.getMap(), strokeColor:"black", path: newPoints, zIndex: 99});
    }

    function zoomOnTimespan(start,end){
        var minLat, maxLat, minLng, maxLng;
        var startPoint = getLatLngOnGPSLine(start);
        var endPoint = getLatLngOnGPSLine(end);
        if (startPoint.lat() < endPoint.lat()){
            minLat = startPoint.lat();
            maxLat = endPoint.lat();
        }
        else{
            minLat = endPoint.lat();
            maxLat = startPoint.lat();
        }
        if (startPoint.lng() < endPoint.lng()){
            minLng = startPoint.lng();
            maxLng = endPoint.lng();
        }
        else{
            minLng = endPoint.lng();
            maxLng = endPoint.lng();
        }
        for (var i = 0; i < gpsPositions.length; i++){
            if (gpsPositions[i].lat() < minLat)
                minLat = gpsPositions[i].lat();
            else if (gpsPositions[i].lat() > maxLat)
                maxLat = gpsPositions[i].lat();
            if (gpsPositions[i].lng() < minLng)
                minLng = gpsPositions[i].lng();
            else if (gpsPositions[i].lng() > maxLng)
                maxLng = gpsPositions[i].lng();
        }
        map.fitBounds(new google.maps.LatLngBounds(new google.maps.LatLng(minLat,minLng), new google.maps.LatLng(maxLat,maxLng)));
    }

    var mapTab = new Tab("map", "Candide Kemmler", "icon-map-marker", true);
    mapTab.render = render;
    return mapTab;

});
