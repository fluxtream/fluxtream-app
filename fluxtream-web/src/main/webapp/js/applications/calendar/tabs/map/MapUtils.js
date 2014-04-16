define(["applications/calendar/tabs/map/MapConfig",
        "applications/calendar/App"], function(Config, Calendar) {
    var config = Config.getConfig();
    google.maps.visualRefresh = true;

    function getSplittingStrategy(splittingStrategyDesc){
        var strategyName = splittingStrategyDesc.substring(0,splittingStrategyDesc.indexOf("("));
        var params = splittingStrategyDesc.substring(splittingStrategyDesc.indexOf("(") + 1,splittingStrategyDesc.lastIndexOf(")")).split(",");
        for (var i = 0; i < params.length; i++){
            if (params[i] === ""){
                params.splice(i--,1);
            }
        }

        switch (strategyName){
            case "flatCutoff":
            {
                var cutoff = parseFloat(params[0]);
                return function(prevFacet,currentFacet){
                    return prevFacet != null && currentFacet.start - prevFacet.start > cutoff;
                }
            }
            case "uriEquality":
            {
                return function(prevFacet,currentFacet){
                    return prevFacet != null && prevFacet.uri !== currentFacet.uri;
                }
            }
            case "none":
            default:
                return function(){return false;};
        }
    }


    function addGPSData(map,gpsData, config, clickable){
        if (!(config.map && config.gps))
            return;
        var newGPSDataSet = {};
        map.gpsData[gpsData[0].type] = newGPSDataSet;
        if (map.primaryGPSData == null){
            map.primaryGPSData = newGPSDataSet;
        }

        newGPSDataSet.gpsPositions = [];
        newGPSDataSet.gpsTimestamps = [];
        newGPSDataSet.gpsAccuracies = [];
        newGPSDataSet.gpsLines = []
        if (gpsData.length == 0)
            return;
        map.markers[gpsData[0].type] = [];
        var minLat = 90; //initialized to the largest valid latitude
        var maxLat = 0; //initialized to the smallest valid latitude
        var minLng = 180; //initialized to the largest valid longitude
        var maxLng = -180; //initialized to the smallest valid longitude
        gpsData = filterGPSData(gpsData);
        var currentLinePoints = [];
        var lastSectionEnd = 0;
        var currentType = null;

        var splittingStrategy = getSplittingStrategy(config.gpsSplittingStrategy);

        function endLineSection(disjoint){
            var colorToUse = currentType == null ? config.color : config.getCustomColor(currentType);
            if (disjoint){
                currentLinePoints.splice(currentLinePoints.length - 1, 1);
            }
            var newEnd = newGPSDataSet.gpsPositions.length - (disjoint ? 2 : 1);
            newGPSDataSet.gpsLines.push({
                line: new google.maps.Polyline({
                    map:map,
                    path:currentLinePoints,
                    clickable:false,
                    strokeColor: colorToUse}),
                highlight:null,
                color: colorToUse,
                start: lastSectionEnd,
                end: newEnd
            });

            lastSectionEnd = newEnd;
            currentLinePoints = [newGPSDataSet.gpsPositions[newGPSDataSet.gpsPositions.length - 1]];
        }

        for (var i = 0; i < gpsData.length; i++){
            var lat = gpsData[i].position[0];
            var lng = gpsData[i].position[1];
            var newPoint = new google.maps.LatLng(lat,lng);
            newGPSDataSet.gpsPositions.push(newPoint);
            try {
                map.markers[gpsData[i].type].push(new google.maps.Marker({map:map,
                    position:newPoint,
                    icon:config.mapicon,
                    shadow:config.mapshadow,
                    clickable:clickable}));
                newGPSDataSet.gpsTimestamps.push(gpsData[i].start);
                newGPSDataSet.gpsAccuracies.push(gpsData[i].accuracy);
                map.enhanceMarkerWithItem(map.markers[gpsData[i].type][map.markers[gpsData[i].type].length-1],gpsData[i]);
                var bounds = map.markers[gpsData[i].type][map.markers[gpsData[i].type].length-1].getBounds();
                if (bounds.getSouthWest().lat() < minLat)
                    minLat = bounds.getSouthWest().lat();
                if (bounds.getNorthEast().lat() > maxLat)
                    maxLat = bounds.getNorthEast().lat();
                if (bounds.getSouthWest().lng() < minLng)
                    minLng = bounds.getSouthWest().lng();
                if (bounds.getNorthEast().lng() > maxLng)
                    maxLng = bounds.getNorthEast().lng();
            } catch (e) {}

            var doEndLine = false;
            var disjointSplit = false;

            var newType = currentType;



            currentLinePoints.push(newPoint);
            if (gpsData[i].uri != null){
                var parts = gpsData[i].uri.split("/");
                if (parts.length == 2){
                    var newType = parts[0];
                    if (currentType != newType){
                        if (currentType != null){
                            doEndLine = true;
                        }
                    }
                }
            }

            if (splittingStrategy(gpsData[i-1],gpsData[i])){
                doEndLine = true;
                disjointSplit = true;
            }

            if (doEndLine)
                endLineSection(disjointSplit);

            currentType = newType;
        }

        endLineSection();

        addToGPSBounds(map, new google.maps.LatLng(minLat,minLng));
        addToGPSBounds(map, new google.maps.LatLng(maxLat,maxLng));
        refreshMarkerPosition(map);
    }

    function addToGPSBounds(map, point){
        if (map.gpsBounds == null){
            map.gpsBounds = new google.maps.LatLngBounds(point,point);
            return;
        }

        var maxLng = Math.max(point.lng(),map.gpsBounds.getNorthEast().lng());
        var minLng = Math.min(point.lng(),map.gpsBounds.getSouthWest().lng());
        var minLat = Math.min(point.lat(),map.gpsBounds.getSouthWest().lat());
        var maxLat = Math.max(point.lat(),map.gpsBounds.getNorthEast().lat());

        map.gpsBounds = new google.maps.LatLngBounds(new google.maps.LatLng(minLat,minLng),new google.maps.LatLng(maxLat,maxLng));

    }

    function filterGPSData(gpsData){//also sorts it
        var filtered = 0;
        var avg = 0;
        for (var i = 0; i < gpsData.length; i++){
            if (gpsData[i].accuracy > config.flatAccuracyCutoff || gpsData[i].accuracy == 0){
                filtered++
                continue;
            }
            avg += gpsData[i].accuracy;
        }
        var cutoff = 1000000000;
        if (filtered != gpsData.length){
            avg /= gpsData.length - filtered;
            var std = 0;
            for (var i = 0; i < gpsData.length; i++){
                if (gpsData[i].accuracy > config.flatAccuracyCutoff || gpsData[i].accuracy == 0)
                    continue;
                std += Math.pow(gpsData[i].accuracy - avg,2);
            }
            std /= gpsData.length - filtered;
            std = Math.sqrt(std);
            cutoff = avg + std * config.stdAccuracyCutoff;
        }
        var newDataSet = [];
        var sub = 0;
        for (var i = 0, li = gpsData.length; i < li; i++){
            if (gpsData[i].accuracy > cutoff  || (filtered != gpsData.length && gpsData[i].accuracy == 0)){
                continue;
            }

            var min = 0;
            var max = newDataSet.length - 1;
            if (min > max){
                newDataSet[0] = gpsData[i];
            }
            else{
                var startTime = gpsData[i].start;
                if (startTime < newDataSet[min].start)
                    newDataSet.splice(min,0,gpsData[i])
                else if (startTime > newDataSet[max].start){
                    newDataSet.push(gpsData[i]);
                }
                else{
                    while (Math.abs(min-max) > 1){
                        var mid = Math.floor((min + max) / 2);
                        var midTime = newDataSet[mid].start;
                        if (startTime > midTime)
                            min = mid;
                        else if (startTime < midTime)
                            max = mid;
                        else{
                            min = max = mid;
                            if (min > 0)
                                min--;
                            else
                                max++;
                        }
                    }
                    newDataSet.splice(max,0,gpsData[i]);
                }
            }

        }
        return newDataSet;
    }

    function addData(map, connectorData, connectorInfoId, clickable){
        if (!isDisplayable(connectorInfoId))
            return false;
        map.markers[connectorInfoId] = addItemsToMap(map,connectorData,clickable);
        return map.markers[connectorInfoId] != null;
    }

    function addAddresses(map,addressesData,clickable){
        for (var type in addressesData){
            for (var i = 0; i < addressesData[type].length; i++)
                addAddress(map,addressesData[type][i],clickable);
        }
    }

    function addAddress(map,address,clickable){
        var icon = "/" + FLX_RELEASE_NUMBER + "/images/mapicons/";
        switch (address.type){
            default:
            case "ADDRESS_HOME":
                icon += "home.png";
                break;
            case "ADDRESS_WORK":
                icon += "workoffice.png"
                break;
        }
        var marker = new google.maps.Marker({map:map, position:new google.maps.LatLng(address.latitude,address.longitude), icon:icon});
        map.addressMarkerList.push(marker);
        marker.showCircle = function(){
            if (marker.circle != null)
                return;
            marker.circle = new google.maps.Circle({center:marker.getPosition(),
                map:map,
                radius:address.radius,
                fillColor:"green",
                fillOpacity:0.5,
                strokeOpacity:0,
                clickable:false});
        }
        marker.hideCircle = function(){
            if (marker.circle == null)
                return;
            marker.circle.setMap(null);
            marker.circle = null;
        }
        if (!clickable)
            return marker;
        google.maps.event.addListener(marker, "click", function(){
            map.connectorSelected = null;
            if (map.selectedMarker != null)
                map.selectedMarker.hideCircle();
            map.selectedMarker = marker;
            map.infoWindow.setContent(address.getDetails());
            map.infoWindow.open(map,marker);
            marker.showCircle();
        });

        marker._oldSetMap = marker.setMap;
        marker.setMap = function(newMap){
            if (marker.circle != null)
                marker.circle.setMap(newMap);
            marker._oldSetMap(newMap);
        }
    }

    function isDisplayable(itemType){
        var config = App.getFacetConfig(itemType);
        return config.map && !config.gps;
    }

    function addItemsToMap(map,items,clickable){
        var markerArray = new Array();
        for (var i = 0; i < items.length; i++){
            var marker = addItemToMap(map,items[i],clickable);
            if (marker != null){
                markerArray[markerArray.length] = marker;
            }
            else {
                console.log("null marker for item " + i + " in addItemsToMap");
            }
        }
        if (markerArray.length == 0)
            markerArray = null;
        return markerArray;
    }



    function getPointForItemOnLine(map,gpsData,item,allowNull){
        if (allowNull == null)
            allowNull = true;
        var startTime = item.start;
        var endTime = item.end == null ? startTime : item.end;
        var time = (startTime + endTime) / 2;

        if (gpsData.gpsTimestamps.length == 0)
            return null;
        if (time <= gpsData.gpsTimestamps[0]){
            return (!allowNull || endTime >= gpsData.gpsTimestamps[0]) ? gpsData.gpsPositions[0] : null;
        }
        if (time >= gpsData.gpsTimestamps[gpsData.gpsTimestamps.length - 1]){
            return (!allowNull || endTime <= gpsData.gpsTimestamps[gpsData.gpsTimestamps.length - 1]) ? gpsData.gpsPositions[gpsData.gpsPositions.length-1] : null;
        }
        var min = 0;
        var max = gpsData.gpsTimestamps.length - 1;

        while (Math.abs(min - max) > 1){//when we have two points next to each other then we know we have a mach and can proceed to the next step
            var mid = Math.floor((min + max) / 2);
            var midTime = gpsData.gpsTimestamps[mid];
            if (time < midTime){
                max = mid;
            }
            else if (time > midTime){
                min = mid;
            }
            else{//we have an exact match!
                min = max = mid;
                if (min > 0)
                    min--;
                else
                    max++;
            }

        }

        //var endIndex;
        //for (endIndex = 1; endIndex < gpsData.gpsTimestamps.length && gpsData.gpsTimestamps[endIndex] < time; endIndex++);
        // var startIndex = endIndex - 1;
        var percentThrough = (time - gpsData.gpsTimestamps[min]) / (gpsData.gpsTimestamps[max] - gpsData.gpsTimestamps[min]);

        var projection = map.getProjection();
        var startPoint = projection.fromLatLngToPoint(gpsData.gpsPositions[min]);
        var endPoint = projection.fromLatLngToPoint(gpsData.gpsPositions[max]);

        var x = (endPoint.x - startPoint.x) * percentThrough + startPoint.x;
        var y = (endPoint.y - startPoint.y) * percentThrough + startPoint.y;
        var latlng = projection.fromPointToLatLng(new google.maps.Point(x,y));
        return latlng;

    }

    function getPointForTimeOnLine(map,gpsData,time,allowNull){
        if (gpsData == null)
            gpsData = map.primaryGPSData;
        return getPointForItemOnLine(map,gpsData,{start:time},allowNull);
    }

    //creates a marker with extended functionality
    function addItemToMap(map,item,clickable){
        if (item == null){
            console.log("ignoring null item in addItemToMap");
            return null;
        }
        var itemConfig = App.getFacetConfig(item.type);
        var start = item.start;
        var end = item.end;

        if (item.position == null){
            var gpsDataToUse = null;
            var matchingGPSData = null;
            var point = null;
            for (var objectType in map.gpsData){
                if (App.getFacetConnector(item.type) == App.getFacetConnector(objectType)){
                    matchingGPSData = map.gpsData[objectType];
                    break;
                }
            }
            if (matchingGPSData != null){
                point = getPointForItemOnLine(map,matchingGPSData,item);
                if (point != null){
                    gpsDataToUse = matchingGPSData;
                }
            }
            if (point == null && map.primaryGPSData != null){
                point = getPointForItemOnLine(map,map.primaryGPSData,item);
                if (point != null){
                    gpsDataToUse = map.primaryGPSData;
                }
            }
            if (point == null){
                for (var objectType in map.gpsData){
                    point = getPointForItemOnLine(map,map.gpsData[objectType],item);
                    if (point != null){
                        gpsDataToUse = map.gpsData[objectType];
                        break;
                    }
                }
            }
            if (point == null && map.primaryGPSData != null){
                point = getPointForItemOnLine(map,map.primaryGPSData,item,false);
                if (point != null){
                    gpsDataToUse = map.primaryGPSData;
                }

            }
            if (point != null){
                var marker = new google.maps.Marker({
                    map:map,
                    position:point,
                    icon:itemConfig.mapicon,
                    shadow:itemConfig.mapshadow,
                    clickable:clickable
                });
                marker.gpsData = gpsDataToUse;
                map.enhanceMarkerWithItem(marker,item);
                var bounds = marker.getBounds();
                addToGPSBounds(map, bounds.getNorthEast());
                addToGPSBounds(map, bounds.getSouthWest());
                return marker;

            }
            return null;

        }
        else{
            var point = new google.maps.LatLng(item.position[0],item.position[1]);
            var marker = new google.maps.Marker({
                map:map,
                position:point,
                icon:itemConfig.mapicon,
                shadow:itemConfig.mapshadow,
                clickable:clickable
            });
            map.enhanceMarkerWithItem(marker,item);
            var bounds = marker.getBounds();
            addToGPSBounds(map, bounds.getNorthEast());
            addToGPSBounds(map, bounds.getSouthWest());
            return marker;
        }
    }

    function decorateMarker(marker, item) {
        if (item != null && item.type==="moves-place") {
            if (item.placeType==="foursquare") {
                $.ajax({
                    url: "/api/metadata/foursquare/venue/" + item.foursquareId,
                    success: function(response) {
                        marker.config.mapicon = {
                            url: response.categoryIconUrlPrefix + "bg_32" + response.categoryIconUrlSuffix,
                            size: new google.maps.Size(32,32)
                        };
                        marker.resyncIcons(true);
                        marker.setIcon(marker.config.mapicon);
                    }
                });
            } else {
                marker.config.mapicon = {
                    url: "/images/moves/" + item.placeType + ".png",
                    size: new google.maps.Size(32,37)
                };
                marker.resyncIcons(true);
                marker.setIcon(marker.config.mapicon);
            }
        }
    }

    function addClickListenerForMarker(map,marker,item){
        google.maps.event.addListener(marker, "click", function(){
            map.connectorSelected = item.type;
            if (map.selectedMarker != null)
                map.selectedMarker.hideCircle();
            map.selectedMarker = marker;
            var details = $(item.getDetails(true));
            var details = item.getDetails(true);
            details.on("contentchange",function(event, content){
                map.infoWindow.setContent(details[0]);
                App.apps.calendar.rebindDetailsControls(details,[item]);
            });
            details.find(".mapLink").remove();
            details.css("width","300px");
            details.css("overflow-y","auto");
            details.css("max-height","200px");
            details.trigger("contentchange",details[0]);
            map.infoWindow.open(map,marker);
            marker.doHighlighting();
            marker.showCircle();
            if (map.infoWindowShown != null){
                map.infoWindowShown();
            }
            moveDateAxisCursor(map,marker.time);
        });

    }

    function enhanceMarkerWithItem(map,marker,item){
        if (marker._oldSetMap != null)
            return;
        marker.item = item;
        map.enhanceMarker(marker);
    }

    function enhanceMarker(map, marker, start, end){
        if (marker._oldSetMap != null)
            return;
        marker._oldSetMap = marker.setMap;
        marker.targetMap = null;
        marker.circle = null;
        var gpsDataToUse = marker.gpsData == null ? map.primaryGPSData : marker.gpsData;
        var config = marker.item != null ? App.getFacetConfig(marker.item.type) : {gps:false};
        var startTime = marker.item != null ? marker.item.start : start;
        var endTime = marker.item != null && marker.item.end != null ? marker.item.end : end;
        if (endTime == null)
            endTime =  startTime;
        var time = (startTime + endTime) / 2;
        var accuracy = (marker.item != null  && marker.item.position != null) ? marker.item.accuracy : getGPSAccuracy(gpsDataToUse,time);
        if (accuracy == null)
            accuracy = 0;
        marker.accuracy = accuracy;
        marker.showCircle = function(){
            // Only set a circle for this item if it has its own idea
            // of accuracy.  Otherwise rely on the time marker circles.
            if (marker.circle != null || marker.item == null || !("accuracy" in marker.item))
                return;

            // Generate a fill color for this kind of item
            var fillColor = "#ff0000";
            // Get the config for this type of item
            if (marker.item && marker.item.type) {
                var cfg = App.getFacetConfig(marker.item.type);
                if(cfg!=null && cfg.color !=null) {
                    fillColor = cfg.color;
                }
            }

            marker.circle = new google.maps.Circle({center:marker.getPosition(),
                map:map,
                radius:accuracy,
                fillColor:fillColor,
                fillOpacity:0.4,
                strokeOpacity:0,
                clickable:false});
        }
        marker.getBounds = function(){
            if (accuracy > 0)
                return new google.maps.Circle({center:marker.getPosition(),radius:accuracy}).getBounds();
            else
                return new google.maps.LatLngBounds(marker.getPosition(),marker.getPosition());
        }
        marker.hideCircle = function(){
            if (marker.circle == null)
                return;
            marker.circle.setMap(null);
            marker.circle = null;
        }
        marker.setMap = function(newMap){
            if (marker.circle != null)
                marker.circle.setMap(newMap);
            if (marker.line != null && marker.line === map.currentHighlightedLine){
                if (newMap == null){
                    map.currentHighlightedLine.setMap(null);
                }
                else if(map == newMap){
                    map.currentHighlightedLine.setMap(map);
                }
            }
            else{
                marker.line = null;
            }
            marker.targetMap = newMap;
            if (!marker.hidden)
                marker._oldSetMap(newMap);
        }
        marker.hidden = false;
        marker.hideMarker = function(){
            if (!marker.hidden){
                marker.hidden = true;
                marker._oldSetMap(null);
            }
        }
        marker.showMarker = function(){
            if (marker.hidden){
                marker.hidden = false;
                marker._oldSetMap(marker.targetMap);
            }
        }
        marker.doHighlighting = function(){
            if (map.currentHighlightedLine != null){
                map.currentHighlightedLine.setMap(null);
                map.currentHighlightedLine = null
            }
            if (this.gpsData == null)
                return;
            if ((this.item != null && this.item.end != null) || end != null){
                var strokeColor = App.getFacetConfig(this.item.type).color;
                if (strokeColor == this.gpsData.color){
                    strokeColor = $.xcolor.darken(strokeColor,2).getCSS();
                }
                map.currentHighlightedLine = map.createPolyLineSegment(this.gpsData, this.item != null ? this.item.start : start, this.item != null ? this.item.end : end, {strokeColor:strokeColor, zIndex: 100});
                marker.line = map.currentHighlightedLine;
                if (this.gpsData.gpsLines[0].line.getMap() == null)
                    map.currentHighlightedLine.setMap(null);
            }
        }
        var itemConfig;
        if (marker.item != null)
            itemConfig = App.getFacetConfig(marker.item.type);
        else
            itemConfig = {};


        marker.resyncIcons = function(force){
            if (itemConfig.highlightmapicon == null || force){
                if (this.config.mapicon != null && this.config.mapicon.url != null && this.config.mapicon.size != null){
                    var newSize = new google.maps.Size(this.config.mapicon.size.width * 1.5,this.config.mapicon.size.height * 1.5);
                    this.config.highlightmapicon = {
                        url: this.config.mapicon.url,
                        size: newSize,
                        scaledSize: newSize
                    }
                }

            }
        }

        if (marker.clickable && marker.item != null){
            addClickListenerForMarker(map,marker,marker.item);
        }

        marker.time = time;
        if (marker.item != null){

            marker.config =  {
                mapicon: itemConfig.mapicon,
                highlightmapicon: itemConfig.highlightmapicon != null ? itemConfig.highlightmapicon : itemConfig.mapicon,
                greymapicon: itemConfig.greymapicon != null ? itemConfig.greymapicon : emptyCircle
            };
            marker.resyncIcons();
        }
        else{
            marker.config = {};
        }


        decorateMarker(marker, marker.item);

        //add to marker list in order

        var min = 0;
        var max = map.markerList.length - 1;
        if (min > max){
            map.markerList.push(marker);
        }
        else if (marker.time <= map.markerList[min].time){
            map.markerList.splice(min,0,marker);
        }
        else if (marker.time >= map.markerList[max].time){
            map.markerList.push(marker);
        }
        else{
            while (Math.abs(max - min) > 1){
                var mid = Math.floor((max + min) / 2);
                var midTime = map.markerList[mid].time;
                if (marker.time < midTime){
                    max = mid;
                }
                else if (marker.time > midTime){
                    min = mid;
                }
                else{
                    min = max = mid;
                    if (min > 0)
                        min--;
                    else
                        max++;
                }
            }
            map.markerList.splice(max,0,marker);
        }
    }

    var emptyCircle = {
        url: "/" + FLX_RELEASE_NUMBER + "/images/mapicons/transparentdot.png",
        anchor: google.maps.Point(5,5)
    };

    function highlightTimespan(map, start,end,gpsDataSet){
        function highlight(map,gpsDataSet,start,end){
            if (gpsDataSet.oldHighlightSection != null && gpsDataSet.oldHighlightSection.start == start && gpsDataSet.oldHighlightSection.end == end)
                return;
            if (gpsDataSet.gpsTimestamps.length == 0)
                return;
            var startIndex = map.getFirstIndexAfter(gpsDataSet,start);
            var endIndex = map.getFirstIndexBefore(gpsDataSet,end);

            for (var i = 0; i < gpsDataSet.gpsLines.length; i++){
                var gpsLine = gpsDataSet.gpsLines[i];
                if (gpsLine.highlight != null){
                    gpsLine.highlight.setMap(null);
                    gpsLine.highlight = null;
                }

                if (startIndex <= gpsLine.start && endIndex >= gpsLine.end){
                    gpsLine.line.setOptions({strokeColor: gpsLine.color});
                    continue;
                }
                gpsLine.line.setOptions({strokeColor: $.xcolor.opacity(gpsLine.color, 'lightgrey', 0.69).getCSS()});

                if (startIndex >= gpsLine.end || endIndex <= gpsLine.start)
                    continue;
                var startTime = startIndex <= gpsLine.start ? gpsDataSet.gpsTimestamps[gpsLine.start] : start;
                var endTime = endIndex >= gpsLine.end ? gpsDataSet.gpsTimestamps[gpsLine.end] : end;
                gpsLine.highlight = map.createPolyLineSegment(gpsDataSet, startTime, endTime, {map: gpsLine.line.getMap(), strokeColor:gpsLine.color, zIndex: 99});
            }
            gpsDataSet.oldHighlightSection = {start:start,end:end};
        }
        if (gpsDataSet != null){
            highlight(map, gpsDataSet,start,end);
        }
        else{
            for (var objectType in map.gpsData){
                highlight(map, map.gpsData[objectType],start,end);
            }
        }

        if ((map.oldHighlightSection != null && map.oldHighlightSection.start == start && map.oldHighlightSection.end == end) || map.markerList.length == 0)
            return;

        function binarySearchOnMarkerList(time,below){
            var min = 0;
            var max = map.markerList.length - 1;
            if (time <= map.markerList[min].time)
                return min;
            if (time >= map.markerList[max].time)
                return max;

            while (Math.abs(max - min) > 1){
                var mid = Math.floor((min + max) / 2);
                var midTime = map.markerList[mid].time;
                if (midTime < time){
                    min = mid;
                }
                else if (midTime > time){
                    max = mid;
                }
                else{
                    min = max = mid;
                }
            }
            if (below)
                return min;
            else
                return max;
        }

        var oldMin = 0;
        var oldMax = map.markerList.length - 1;
        if (map.oldHighlightSection != null){
            oldMin = map.oldHighlightSection.min;
            oldMax = map.oldHighlightSection.max;
        }

        var newMin = binarySearchOnMarkerList(start,false);
        var newMax = binarySearchOnMarkerList(end,true);

        for (var i = Math.min(oldMin,newMin), li = Math.max(oldMin,newMin); i <= li; i++){
            var marker = map.markerList[i];
            var wasHighlighted = oldMin <= i && i <= oldMax;
            var shouldBeHighlighted = newMin <= i && i <= newMax;
            if (wasHighlighted != shouldBeHighlighted){
                marker.grey = !shouldBeHighlighted;
                if (shouldBeHighlighted){
                    if (marker.highlight)
                        marker.setIcon(marker.config.highlightmapicon);
                    else
                        marker.setIcon(marker.config.mapicon);
                }
                else{
                    if (!marker.highlight)
                        marker.setIcon(marker.config.greymapicon);
                }
            }
        }
        for (var i = Math.min(oldMax,newMax), li = Math.max(oldMax,newMax); i <= li; i++){
            var marker = map.markerList[i];
            var wasHighlighted = oldMin <= i && i <= oldMax;
            var shouldBeHighlighted = newMin <= i && i <= newMax;
            if (wasHighlighted != shouldBeHighlighted){
                marker.grey = !shouldBeHighlighted;
                if (shouldBeHighlighted){
                    if (marker.highlight)
                        marker.setIcon(marker.config.highlightmapicon);
                    else
                        marker.setIcon(marker.config.mapicon);
                }
                else{
                    if (!marker.highlight)
                        marker.setIcon(marker.config.greymapicon);
                }
            }
        }
        map.oldHighlightSection = {start:start,end:end,min:newMin,max:newMax};
    }

    function getFirstIndexAfter(gpsDataSet, time){
        var endIndex;
        for (endIndex = 0; endIndex < gpsDataSet.gpsTimestamps.length && gpsDataSet.gpsTimestamps[endIndex] < time; endIndex++);
        return endIndex;
    }

    function getFirstIndexBefore(gpsDataSet, time){
        if (time <= gpsDataSet.gpsTimestamps[0])
            return -1;
        var endIndex;
        for (endIndex = 1; endIndex < gpsDataSet.gpsTimestamps.length && gpsDataSet.gpsTimestamps[endIndex] < time; endIndex++);
        return endIndex - 1;
    }

    function createPolyLineSegment(map, gpsDataSet, start, end, options){
        if (typeof options.map === "undefined")
            options.map = map;
        var newPoints = new Array();
        newPoints[0] = map.getLatLngOnGPSLine(start,gpsDataSet);
        if (newPoints[0] == null)
            return null;
        var startIndex = map.getFirstIndexAfter(gpsDataSet,start);
        var endIndex = map.getFirstIndexBefore(gpsDataSet,end);
        for (var i = 0; i + startIndex <= endIndex; i++){
            newPoints[i+1] = gpsDataSet.gpsPositions[i+startIndex];
        }
        newPoints[newPoints.length] = map.getLatLngOnGPSLine(end,gpsDataSet);
        options.path = newPoints;
        options.clickable = false;
        return new google.maps.Polyline(options);
    }

    function getGPSAccuracy(gpsData,time){
        if (gpsData == null)
            return -1;
        if (gpsData.gpsTimestamps.length == 0)
            return -1;
        if (time <= gpsData.gpsTimestamps[0])
            return gpsData.gpsAccuracies[0];
        if (time >= gpsData.gpsTimestamps[gpsData.gpsTimestamps.length - 1])
            return gpsData.gpsAccuracies[gpsData.gpsAccuracies.length - 1];

        var min = 0;
        var max = gpsData.gpsTimestamps.length - 1;

        while (Math.abs(min - max) > 1){//when we have two points next to each other then we know we have a mach and can proceed to the next step
            var mid = Math.floor((min + max) / 2);
            var midTime = gpsData.gpsTimestamps[mid];
            if (time < midTime){
                max = mid;
            }
            else if (time > midTime){
                min = mid;
            }
            else{//we have an exact match!
                min = max = mid;
                if (min > 0)
                    min--;
                else
                    max++;
            }

        }

        /*var endIndex;
         for (endIndex = 1; endIndex < gpsData.gpsTimestamps.length && gpsData.gpsTsimestamps[endIndex] < time; endIndex++);
         var startIndex = endIndex - 1;*/
        var percentThrough = (time - gpsData.gpsTimestamps[min]) / (gpsData.gpsTimestamps[max] - gpsData.gpsTimestamps[min]);
        if (isNaN(percentThrough))
            return gpsData.gpsAccuracies[min];
        return (gpsData.gpsAccuracies[max] - gpsData.gpsAccuracies[min]) * percentThrough + gpsData.gpsAccuracies[min];
    }

    function zoomOnTimespan(map, start,end){
        var minLat, maxLat, minLng, maxLng;
        function zoom(map,gpsDataSet,start,end){
            if (start > gpsDataSet.gpsTimestamps[gpsDataSet.gpsTimestamps.length - 1] || end < gpsDataSet.gpsTimestamps[0])
                return;
            var startPoint = map.getLatLngOnGPSLine(start,gpsDataSet);
            var endPoint = map.getLatLngOnGPSLine(end,gpsDataSet);
            if (startPoint == null || endPoint == null)
                return;
            if (minLat == null){
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
                    maxLng = startPoint.lng();
                }
            }
            else{
                minLat = Math.min(Math.min(minLat,startPoint.lat()),endPoint.lat());
                maxLat = Math.max(Math.max(maxLat,startPoint.lat()),endPoint.lat());
                minLng = Math.min(Math.min(minLng,startPoint.lng()),endPoint.lng());
                maxLng = Math.max(Math.max(maxLng,startPoint.lng()),endPoint.lng());
            }

            var startIndex = map.getFirstIndexAfter(gpsDataSet,start);
            var endIndex = map.getFirstIndexBefore(gpsDataSet,end);

            for (var i = startIndex; i < endIndex; i++){
                if (gpsDataSet.gpsPositions[i].lat() < minLat)
                    minLat = gpsDataSet.gpsPositions[i].lat();
                else if (gpsDataSet.gpsPositions[i].lat() > maxLat)
                    maxLat = gpsDataSet.gpsPositions[i].lat();
                if (gpsDataSet.gpsPositions[i].lng() < minLng)
                    minLng = gpsDataSet.gpsPositions[i].lng();
                else if (gpsDataSet.gpsPositions[i].lng() > maxLng)
                    maxLng = gpsDataSet.gpsPositions[i].lng();
            }

        }
        for (var objectTypeId in map.gpsData){
            zoom(map,map.gpsData[objectTypeId],start,end);
        }

        if (minLat == null)
            return;

        map.fitBounds(new google.maps.LatLngBounds(new google.maps.LatLng(minLat,minLng), new google.maps.LatLng(maxLat,maxLng)));
    }

    function zoomOnPoint(map, point){
        map.setCenter(point);
        map.setZoom(15);
    }

    function zoomOnMarker(map,marker){
        if (marker.accuracy == 0 || marker.circle == null)
            zoomOnPoint(map,marker.getPosition());
        else
            map.fitBounds(marker.circle.getBounds());
    }

    function hideData(map,connectorId){
        if (!map.hasData(connectorId))
            return;
        if (map.connectorSelected == connectorId){
            map.infoWindow.close();
            map.connectorSelected = null;
        }
        for (var i = 0; i < map.markers[connectorId].length; i++){
            map.markers[connectorId][i].setMap(null);
        }
        if (map.gpsData[connectorId] != null){
            for (var i = 0, li = map.gpsData[connectorId].gpsLines.length; i < li; i++){
                map.gpsData[connectorId].gpsLines[i].line.setMap(null);
                if (map.gpsData[connectorId].gpsLines[i].highlight != null)
                    map.gpsData[connectorId].gpsLines[i].highlight.setMap(null);
            }
            if (map.gpsData[connectorId].dateMarker != null){
                map.gpsData[connectorId].dateMarker.setMap(null);
                map.gpsData[connectorId].dateMarker.circle.setMap(null);
            }
        }

    }

    function showData(map,connectorId){
        if (!map.hasData(connectorId))
            return;
        for (var i = 0; i < map.markers[connectorId].length; i++){
            map.markers[connectorId][i].setMap(map);
            if (map.selectedMarker == map.markers[connectorId][i])
                map.selectedMarker.showCircle();
        }
        if (map.gpsData[connectorId] != null){
            for (var i = 0, li = map.gpsData[connectorId].gpsLines.length; i < li; i++){
                map.gpsData[connectorId].gpsLines[i].line.setMap(map);
                if (map.gpsData[connectorId].gpsLines[i].highlight != null)
                    map.gpsData[connectorId].gpsLines[i].highlight.setMap(map);
            }
            if (map.gpsData[connectorId].dateMarker != null){
                map.gpsData[connectorId].dateMarker.setMap(map);
                map.gpsData[connectorId].dateMarker.circle.setMap(map);
            }
        }
    }

    function hasData(map,connectorId){
        if (map.markers[connectorId] != null)
            return true;
        for (var objectType in map.gpsData){
            if (App.getFacetConnector(objectType) == connectorId)
                return true;
        }
        return false;
    }

    function createMapPositionControls(map){
        var control = $("<div></div>");
        control.css("background","white");
        control.css("margin","0px 5px");
        control.css("border","1px solid rgb(113, 123, 135)");
        control.css("padding","5px");
        control.css("box-shadow","rgba(0, 0, 0, 0.398438) 0px 2px 4px");
        control.css("-webkit-box-shadow","rgba(0, 0, 0, 0.398438) 0px 2px 4px");
        control.css("width","84px");
        var fitButton = $('<button class="btnList btn btnListChecked enabled">Fit to View</button>');
        fitButton.css("margin-bottom","0.5em");
        var container = $("<label style='cursor:pointer;'></label>")
        var preserveView = $('<input type="checkbox">');
        preserveView.css("margin-right","0.5em");
        preserveView.css("float","left");

        control.append(fitButton);
        container.append(preserveView);
        container.append("Preserve View");
        control.append(container);
        fitButton.click(function(){
            map.fitBounds(map.gpsBounds);
        });
        map.preserveViewCheckbox = preserveView;
        map.controls[google.maps.ControlPosition.RIGHT_TOP].push(control[0]);
        map._preserveViewBtn = preserveView[0];

        preserveView.click(function(){
            if (map.preserveViewCheckboxChanged != null)
                map.preserveViewCheckboxChanged();
        });

        map.isPreserveViewChecked = function(){
            return map._preserveViewBtn.checked;
        }

        map.setPreserveView = function(isSet){
            map._preserveViewBtn.checked = isSet;
        }
    }

    /*

     */

    function createTimelineControls(map,maxBounds){
        App.loadMustacheTemplate("applications/calendar/tabs/map/mapTemplates.html","timeControls",function(template){
            var control = $(template.render());
            map.controls[google.maps.ControlPosition.TOP].push(control[0]);

            map.setMaxTimeBounds(maxBounds);

            createDateAxis(map,"mapDateAxis");

            control.find("#map_zoomIn_button").click(function(event){
                var zoom = map.dateAxis.getMax() - map.dateAxis.getMin();
                var zoomChange = ((zoom / 1.4) - zoom) / 2;
                map.dateAxis.setRange(map.dateAxis.getMin() - zoomChange, map.dateAxis.getMax() + zoomChange);
                event.preventDefault();
            });
            control.find("#map_zoomOut_button").click(function(event){
                var zoom = map.dateAxis.getMax() - map.dateAxis.getMin();
                var zoomChange = ((zoom * 1.4) - zoom) / 2;
                map.dateAxis.setRange(map.dateAxis.getMin() - zoomChange, map.dateAxis.getMax() + zoomChange);
                event.preventDefault();
            });
            control.find("#map_gotoBeginning_button").click(function(event){
                var panAmount = map.maxBounds.min - map.dateAxis.getMin();
                map.dateAxis.setRange(map.dateAxis.getMin() + panAmount, map.dateAxis.getMax() + panAmount);
                event.preventDefault();
            });
            control.find("#map_gotoBack_button").click(function(event){
                var panAmount = map.dateAxis.getMin() - map.dateAxis.getMax();
                var maxPanAmount = map.maxBounds.min - map.dateAxis.getMin();
                if (panAmount < maxPanAmount)
                    panAmount = maxPanAmount;
                map.dateAxis.setRange(map.dateAxis.getMin() + panAmount, map.dateAxis.getMax() + panAmount);
                event.preventDefault();
            });
            control.find("#map_gotoForward_button").click(function(event){
                var panAmount = map.dateAxis.getMax() - map.dateAxis.getMin();
                var maxPanAmount = map.maxBounds.max - map.dateAxis.getMax();
                if (panAmount > maxPanAmount)
                    panAmount = maxPanAmount;
                map.dateAxis.setRange(map.dateAxis.getMin() + panAmount, map.dateAxis.getMax() + panAmount);
                event.preventDefault();
            });
            control.find("#map_gotoEnd_button").click(function(event){
                var panAmount = map.maxBounds.max - map.dateAxis.getMax();
                map.dateAxis.setRange(map.dateAxis.getMin() + panAmount, map.dateAxis.getMax() + panAmount);
                event.preventDefault();
            });

        });


    }

    function createDateAxis(map,id){
        if ($("#" + id).length == 0){
            setTimeout(function(){
                createDateAxis(map,id);
            },10);
            return;
        }

        map.dateAxis = new DateAxis(id, "horizontal", {
            "min" : map.maxBounds.min,
            "max" : map.maxBounds.max
        });
        map.dateAxis.setCursorPosition(map.maxBounds.min);
        map.dateAxisContainer =  $("#" + id);
        $(window).resize(function(){
            map.dateAxis.setSize(map.dateAxisContainer.width(),map.dateAxisContainer.height(), SequenceNumber.getNext());
        });
        map.dateAxis.setMaxRange(map.maxBounds.min,map.maxBounds.max);
        $(window).resize();

        var oldPosition = null;
        map.dateAxis.addAxisChangeListener(function(event){
            if (oldPosition != event.cursorPosition)
                oldPosition = App.apps.calendar.dateAxisCursorPosition = event.cursorPosition;
            updateCursorMarkerPosition(map,event.cursorPosition);
            updateMarkerHighlighting(map,event.cursorPosition);
            if (map.markerList.length > 5500){//we should delay so the axis doesn't become locked up
                $.doTimeout("MapDateAxisChange");//cancel previous doTimeout
                $.doTimeout('MapDateAxisChange', 500, function() {//schedule a new one
                    updateDateAxisHighlighting(map,event.min,event.max);
                });
            }
            else{
                updateDateAxisHighlighting(map,event.min,event.max);

            }

        });
        refreshMarkerPosition(map);
    }

    function updateCursorMarkerPosition(map,time){
        for (var member in map.gpsData){
            var dataSource = map.gpsData[member];
            if (dataSource.cursorPos == time)
                return;
            dataSource.cursorPos = time;
            var newPosition = getPointForTimeOnLine(map,dataSource,time*1000,false);
            var newAccuracy = getGPSAccuracy(dataSource,time*1000);
            if (dataSource.dateMarker == null){
                // Setup defaults for stroke and fill color to be red/red
                var strokeColor = "#ff0000";
                var fillColor = "#ff0000";

                // Get the config for this type of item
                var cfg = App.getFacetConfig(member);
                if(cfg!=null) {
                    // Check for strokeColor, and if set use it for the stroke
                    if(cfg.strokeColor !=null) {
                        strokeColor = cfg.strokeColor;
                    }
                    // Check for color, and if set use it for the fill
                    if(cfg.color !=null) {
                        fillColor = cfg.color;
                    }
                }

                // Create a marker that's an inverted triangle with configurable colors
                dataSource.dateMarker = new google.maps.Marker({
                    map: dataSource.gpsLines[0].line.getMap(),
                    position: newPosition,
                    clickable:true,
                    zIndex: google.maps.Marker.MAX_ZINDEX,
                    icon: {
                        path: "M 0 0 L -8 -16 L 8 -16 z",
                        fillColor: fillColor,
                        strokeColor: strokeColor,
                        strokeWeight: 2,
                        fillOpacity: 1
                    }
                });
                dataSource.dateMarker.circle = new google.maps.Circle({center:newPosition,
                    map:dataSource.gpsLines[0].line.getMap(),
                    radius:newAccuracy,
                    fillColor:fillColor,
                    fillOpacity:0.4,
                    strokeOpacity:0,
                    clickable:false});
                google.maps.event.addListener(dataSource.dateMarker, "click", function(){
                    centerDateAxisOnCursor(map);
                });

            }
            else{
                dataSource.dateMarker.setPosition(newPosition);
                dataSource.dateMarker.circle.setCenter(newPosition);
                dataSource.dateMarker.circle.setRadius(newAccuracy);
            }
        }
    }

    function updateMarkerHighlighting(map,time){
        var start = (time - map.dateAxis.getScale() * 5) * 1000;
        var end = (time + map.dateAxis.getScale() * 5) * 1000;
        function binarySearchOnMarkerList(time,below){
            if (map.markerList.length == 0){
                return null;
            }
            var min = 0;
            var max = map.markerList.length - 1;
            if (time <= map.markerList[min].time)
                return min;
            if (time >= map.markerList[max].time)
                return max;

            while (Math.abs(max - min) > 1){
                var mid = Math.floor((min + max) / 2);
                var midTime = map.markerList[mid].time;
                if (midTime < time){
                    min = mid;
                }
                else if (midTime > time){
                    max = mid;
                }
                else{
                    min = max = mid;
                }
            }
            if (below)
                return min;
            else
                return max;
        }

        var oldMin = 0;
        var oldMax = map.markerList.length - 1;
        if (map.oldMarkerHighlight != null){
            oldMin = map.oldMarkerHighlight.min;
            oldMax = map.oldMarkerHighlight.max;
        }

        var newMin = binarySearchOnMarkerList(start,false);
        var newMax = binarySearchOnMarkerList(end,true);
        if (newMin == null){
            map.oldMarkerHighlight = null;
            return;
        }

        for (var i = Math.min(oldMin,newMin), li = Math.max(oldMin,newMin); i <= li; i++){
            var marker = map.markerList[i];
            var wasHighlighted = oldMin <= i && i <= oldMax;
            var shouldBeHighlighted = newMin <= i && i <= newMax;
            if (wasHighlighted != shouldBeHighlighted){
                marker.highlight = shouldBeHighlighted;
                if (shouldBeHighlighted){
                    marker.setIcon(marker.config.highlightmapicon);
                }
                else{
                    if (!marker.grey)
                        marker.setIcon(marker.config.mapicon);
                    else
                        marker.setIcon(marker.config.greymapicon);

                }
            }
        }
        for (var i = Math.min(oldMax,newMax), li = Math.max(oldMax,newMax); i <= li; i++){
            var marker = map.markerList[i];
            var wasHighlighted = oldMin <= i && i <= oldMax;
            var shouldBeHighlighted = newMin <= i && i <= newMax;
            if (wasHighlighted != shouldBeHighlighted){
                marker.highlight = shouldBeHighlighted;
                if (shouldBeHighlighted){
                    marker.setIcon(marker.config.highlightmapicon);
                }
                else{
                    if (!marker.grey)
                        marker.setIcon(marker.config.mapicon);
                    else
                        marker.setIcon(marker.config.greymapicon);

                }
            }
        }
        map.oldMarkerHighlight = {start:start,end:end,min:newMin,max:newMax};

    }

    function moveDateAxisCursor(map,time){
        if (map.dateAxis != null){
            var targetTime = time/1000;
            map.dateAxis.setCursorPosition(targetTime);
            var minTime = map.dateAxis.getMin();
            var maxTime = map.dateAxis.getMax();
            if (targetTime < minTime || targetTime > maxTime){
                centerDateAxisOnCursor(map);
            }
        }
    }

    function centerDateAxisOnCursor(map){
        if (map.dateAxis != null){
            var targetTime = map.dateAxis.getCursorPosition();
            if (targetTime != null){
                var minTime = map.dateAxis.getMin();
                var maxTime = map.dateAxis.getMax();
                var offset = (maxTime - minTime) / 2;
                map.dateAxis.setRange(targetTime - offset,targetTime + offset);
            }
        }

    }


    function updateDateAxisHighlighting(map,min,max){
        map.highlightTimespan(min*1000,max*1000);
    }

    function refreshMarkerPosition(map){
        if (map.dateAxis == null)
            return;
        updateCursorMarkerPosition(map,map.dateAxis.getCursorPosition());
        updateMarkerHighlighting(map,map.dateAxis.getCursorPosition());
    }

    function fixZooming(map,zoomLevel,isPreserved){
        if (!map.isFullyInitialized()){
            $.doTimeout(100,function(){
                fixZooming(map,zoomLevel,isPreserved);
            });
            return;
        }
        if (map.getZoom() > 16 && !isPreserved)
            map.setZoom(16);
        if (isPreserved)
            map.setZoom(map.getZoom()+1);
    }

    function zoomOnItemAndClick(map,item,zoom){
        var targetMarker = null;
        for (var i = 0, li = map.markerList.length; i < li && targetMarker == null; i++){
            if (map.markerList[i].item != null && map.markerList[i].item.id == item.id)
                targetMarker = map.markerList[i];
        }
        if (targetMarker != null && zoom){
            map.zoomOnMarker(targetMarker);
        }
        google.maps.event.trigger(targetMarker,"click");
    }

    return {
        isDisplayable: isDisplayable,
        filterGPSData: filterGPSData,
        newMap: function(center,zoom,divId,hideControls,maxBounds ){ //creates and returns a google map with extended functionality
            var options = {
                zoom : zoom,
                center: center,
                scrollwheel : true,
                streetViewControl : true,
                mapTypeId : google.maps.MapTypeId.ROADMAP
            };
            if (hideControls){
                options.disableDefaultUI = true;
            }
            var map = new google.maps.Map(document.getElementById(divId),options);
            map.reset = function(){
                if (map.infoWindow == null){//brand new map, initialize
                    map.infoWindow = new google.maps.InfoWindow();
                    google.maps.event.addListener(map.infoWindow,"closeclick",function(){
                        if (map.selectedMarker != null){
                            map.selectedMarker.hideCircle();
                            if (map.currentHighlightedLine != null){
                                map.currentHighlightedLine.setMap(null);
                                map.currentHighlightedLine = null
                            }
                            this.selectedMarker = null;
                        }
                    });
                }
                else{//old map, remove everything!
                    for (var i = 0, li = this.markerList.length; i < li; i++){
                        this.markerList[i].setMap(null);
                    }
                    for (var i = 0, li = this.addressMarkerList.length; i < li; i++){
                        this.addressMarkerList[i].setMap(null);
                    }
                    for (var dataset in this.gpsData){
                        for (var i = 0, li = this.gpsData[dataset].gpsLines.length; i < li; i++){
                            this.gpsData[dataset].gpsLines[i].line.setMap(null);
                            if (this.gpsData[dataset].gpsLines[i].highlight != null)
                                this.gpsData[dataset].gpsLines[i].highlight.setMap(null);
                            if (this.gpsData[dataset].dateMarker != null){
                                this.gpsData[dataset].dateMarker.setMap(null);
                                this.gpsData[dataset].dateMarker.circle.setMap(null);

                            }
                        }
                    }
                }
                this.currentHighlightedLine = null;
                this.connectorSelected = null;
                this.selectedMarker = null;
                this.oldMarkerHighlight = null;
                this.executionQueue = [];

                this.markers = {};
                this.markerList = [];
                this.addressMarkerList = [];

                this.gpsBounds = null;

                this.gpsData = {};
                this.primaryGPSData = null;
                this.oldHighlightSection = null;

            }

            map.reset();

            map.setDigest = function(digest){
                this.digest = digest;
            }

            map.addGPSData = function(gpsData,config,clickable){addGPSData(map,gpsData, config,clickable)};
            map.addData = function(connectorData, connectorInfoId,clickable){return addData(map,connectorData, connectorInfoId,clickable)};
            map.addAddresses = function(addresses,clickable){addAddresses(map,addresses,clickable)}
            map.getLatLngOnGPSLine = function(time,gpsDataSet){return getPointForTimeOnLine(map,gpsDataSet,time,false);};
            map.createPolyLineSegment = function(gpsDataSet, start,end,options){return createPolyLineSegment(map, gpsDataSet,start,end,options)};
            map.getFirstIndexAfter = function(gpsDataSet, time){return getFirstIndexAfter(gpsDataSet,time)};
            map.getFirstIndexBefore = function(gpsDataSet, time){return getFirstIndexBefore(gpsDataSet,time)};
            map.zoomOnTimespan = function(start,end){zoomOnTimespan(map,start,end)};
            map.highlightTimespan = function(start,end){highlightTimespan(map,start,end)};
            map.showData = function(connectorId){showData(map,connectorId)};
            map.hideData = function(connectorId){hideData(map,connectorId)};
            map.hasData = function(connectorId){return hasData(map,connectorId)};
            map.addItem = function(item,clickable){return addItemToMap(map,item,clickable)};
            map.zoomOnPoint = function(point){zoomOnPoint(map,point)};
            map.zoomOnMarker = function(marker){zoomOnMarker(map,marker)};
            map.enhanceMarker = function(marker,start,end){enhanceMarker(map,marker,start,end)};
            map.enhanceMarkerWithItem = function(marker,item){enhanceMarkerWithItem(map,marker,item)};
            map.isFullyInitialized = function(){
                if (map.getProjection() == null)
                    return false;
                if (!hideControls && map.dateAxis == null)
                    return false;
                return true;
            };
            map.executeAfterReady = function(afterready){
                if (this.executionQueue.length == 0 && this.isFullyInitialized())
                    afterready();
                else
                    this.executionQueue.push(afterready);
            }
            map.isPreserveViewChecked = function(){return false;}
            map.zoomOnItemAndClick = function(item){
                zoomOnItemAndClick(map,item,true);
            }
            map.clickOnItem = function(item){
                zoomOnItemAndClick(map,item,false);
            }
            map._oldFitBounds = map.fitBounds;
            map.fitBounds = function(bounds,isPreservedView){
                if (bounds == null)
                    return;
                this._oldFitBounds(bounds);
                var zoomLevel = map.getZoom();
                fixZooming(map,zoomLevel,isPreservedView);
            }
            map.setMaxTimeBounds = function(maxBounds){
                this.maxBounds = maxBounds;
                if (this.dateAxis != null){
                    this.dateAxis.setMaxRange(maxBounds.min,maxBounds.max);
                    this.dateAxis.setRange(maxBounds.min,maxBounds.max);
                    this.dateAxis.setCursorPosition(maxBounds.min);
                }
            }

            map.setCursorPosition = function(position){
                this.dateAxis.setCursorPosition(position);
            }
            map.hasAnyData = function(){
                return this.markerList.length > 0;
            }

            if (!hideControls){
                createMapPositionControls(map);
                createTimelineControls(map,maxBounds);
            }

            //wait for map to be completely ready and then execute all queued code
            $.doTimeout(100,function(){
                if (map.isFullyInitialized()){
                    var executionQueue = map.executionQueue;
                    map.executionQueue = [];
                    for (var i = 0, li = executionQueue.length; i < li; i++){
                        executionQueue[i]();
                    }
                    return false;
                }
                return true;
            });
            return map;
        }
    }
});