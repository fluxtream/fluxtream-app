define(["applications/calendar/tabs/map/MapConfig"], function(Config) {
    var config = Config.getConfig();
    google.maps.visualRefresh = true;

    function addGPSData(map,gpsData, config, clickable){
        if (!(config.map && config.gps))
            return;
        var newGPSDataSet = {};
        map.gpsData[gpsData[0].type] = newGPSDataSet;
        if (map.primaryGPSData == null){
            map.primaryGPSData = newGPSDataSet;
        }
        newGPSDataSet.color = config.color;

        newGPSDataSet.gpsPositions = [];
        newGPSDataSet.gpsTimestamps = [];
        newGPSDataSet.gpsAccuracies = [];
        if (gpsData.length == 0)
            return;
        map.markers[gpsData[0].type] = [];
        var minLat = 90; //initialized to the largest valid latitude
        var maxLat = 0; //initialized to the smallest valid latitude
        var minLng = 180; //initialized to the largest valid longitude
        var maxLng = -180; //initialized to the smallest valid longitude
        gpsData = filterGPSData(gpsData);
        for (var i = 0; i < gpsData.length; i++){
            var lat = gpsData[i].position[0];
            var lng = gpsData[i].position[1];
            newGPSDataSet.gpsPositions.push(new google.maps.LatLng(lat,lng));
            try {
            map.markers[gpsData[i].type].push(new google.maps.Marker({map:map,
                                                                    position:newGPSDataSet.gpsPositions[newGPSDataSet.gpsPositions.length-1],
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
        }
        newGPSDataSet.gpsLine = new google.maps.Polyline({
            map:map,
            path:newGPSDataSet.gpsPositions,
            clickable:false,
            strokeColor: newGPSDataSet.color});
        addToGPSBounds(map, new google.maps.LatLng(minLat,minLng));
        addToGPSBounds(map, new google.maps.LatLng(maxLat,maxLng));

        map.noGPSDiv.css("display","none");
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
            var j = 0;
            for (j = 0; j < newDataSet.length && newDataSet[j].start < gpsData[i].start; j++);
            if (j < newDataSet.length){
                for (var k = newDataSet.length; k > j; k--){
                    newDataSet[k] = newDataSet[k-1];
                }
                newDataSet[j] = gpsData[i];
            }
            else{
                newDataSet.push(gpsData[i]);
            }

        }
        return newDataSet;
    }

    function addAlternativeGPSData(map, gpsData,connectorInfoId,clickable){
        var config = App.getFacetConfig(connectorInfoId);
        if (!(config.map && config.gps))
            return;
        map.markers[connectorInfoId] = [];
        gpsData = filterGPSData(gpsData);
        var dataPoints = [];
        for (var i = 0, li = gpsData.length; i < li; i++){
            dataPoints.push(new google.maps.LatLng(gpsData[i].position[0],gpsData[i].position[1]));
            map.markers[connectorInfoId][i] = new google.maps.Marker({
                map:map,
                position:dataPoints[i],
                icon:config.mapicon,
                shadow:config.mapshadow,
                clickable:clickable
            });
            map.enhanceMarkerWithItem(map.markers[connectorInfoId][i],gpsData[i]);
        }
        map.polylines[connectorInfoId] = new google.maps.Polyline({
            map:map,
            path:dataPoints,
            clickable:false,
            strokeColor:config.color
        })
    }

    function addData(map,connectorData, connectorInfoId, clickable){
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
        map.markerList.push(marker);
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
            if (marker != null)
                markerArray[markerArray.length] = marker;
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
        var endIndex;
        for (endIndex = 1; endIndex < gpsData.gpsTimestamps.length && gpsData.gpsTimestamps[endIndex] < time; endIndex++);
        var startIndex = endIndex - 1;
        var percentThrough = (time - gpsData.gpsTimestamps[startIndex]) / (gpsData.gpsTimestamps[endIndex] - gpsData.gpsTimestamps[startIndex]);

        var projection = map.getProjection();
        var startPoint = projection.fromLatLngToPoint(gpsData.gpsPositions[startIndex]);
        var endPoint = projection.fromLatLngToPoint(gpsData.gpsPositions[endIndex]);

        var x = (endPoint.x - startPoint.x) * percentThrough + startPoint.x;
        var y = (endPoint.y - startPoint.y) * percentThrough + startPoint.y;
        var latlng = projection.fromPointToLatLng(new google.maps.Point(x,y));
        return latlng;

    }

    //creates a marker with extended functionality
    function addItemToMap(map,item,clickable){
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
            map.noGPSDiv.css("display","none");
            return marker;
        }
    }

    function addClickListenerForMarker(map,marker,item){
        google.maps.event.addListener(marker, "click", function(){
            map.connectorSelected = item.type;
            if (map.selectedMarker != null)
                map.selectedMarker.hideCircle();
            map.selectedMarker = marker;
            updateFacetDetailsHTML(map,item.getDetails(true));
            map.infoWindow.open(map,marker);


            marker.doHighlighting();
            marker.showCircle();
            if (map.infoWindowShown != null){
                map.infoWindowShown();
            }
        });

    }

    function updateFacetDetailsHTML(map,html){
        var details = $(html);
        details.find(".mapLink").remove();
        details.css("width","300px");
        details.find(".facet-edit").unbind("click");
        details.find(".facet-edit").click(function(event){
            event.updateHTML = function(html){
                updateFacetDetailsHTML(map,html);
            }
            App.apps.calendar.commentEdit(event);
        });
        map.infoWindow.setContent(details[0]);
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
        marker.showCircle = function(){
            if (marker.circle != null)
                return;
            marker.circle = new google.maps.Circle({center:marker.getPosition(),
                                                   map:map,
                                                   radius:accuracy,
                                                   fillColor:"red",
                                                   fillOpacity:0.5,
                                                   strokeOpacity:0,
                                                   clickable:false});
        }
        marker.getBounds = function(){
            return new google.maps.Circle({center:marker.getPosition(),radius:accuracy}).getBounds();
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
                if (this.gpsData.gpsLine.getMap() == null)
                    map.currentHighlightedLine.setMap(null);
            }
        }
        if (marker.clickable && marker.item != null){
            addClickListenerForMarker(map,marker,marker.item);
        }
        map.markerList.push(marker);
    }

    function highlightTimespan(map, start,end,gpsDataSet){
        function highlight(map,gpsDataSet,start,end){
            if (gpsDataSet.gpsTimestamps.length == 0)
                return;
            if (gpsDataSet.highlightSection != null){
                gpsDataSet.highlightSection.setMap(null);
                gpsDataSet.highlightSection = null;
            }
            if (start <= gpsDataSet.gpsTimestamps[0] && end >= gpsDataSet.gpsTimestamps[gpsDataSet.gpsTimestamps.length - 1]){
                gpsDataSet.gpsLine.setOptions({strokeColor: gpsDataSet.color});
                return;
            }
            gpsDataSet.gpsLine.setOptions({strokeColor: $.xcolor.opacity(gpsDataSet.color, 'lightgrey', 0.69).getCSS()});
            gpsDataSet.highlightSection = map.createPolyLineSegment(gpsDataSet, start, end, {strokeColor:gpsDataSet.color, zIndex: 99});
        }
        if (gpsDataSet != null){
            highlight(map, gpsDataSet,start,end);
        }
        else{
            for (var objectType in map.gpsData){
                highlight(map, map.gpsData[objectType],start,end);
            }
        }
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

        var endIndex;
        for (endIndex = 1; endIndex < gpsData.gpsTimestamps.length && gpsData.gpsTimestamps[endIndex] < time; endIndex++);
        var startIndex = endIndex - 1;
        var percentThrough = (time - gpsData.gpsTimestamps[startIndex]) / (gpsData.gpsTimestamps[endIndex] - gpsData.gpsTimestamps[startIndex]);
        if (isNaN(percentThrough))
            return gpsData.gpsAccuracies[startIndex];
        return (gpsData.gpsAccuracies[endIndex] - gpsData.gpsAccuracies[startIndex]) * percentThrough + gpsData.gpsAccuracies[startIndex];
    }

    function getLatLngOnGPSLine(map, time, gpsDataSet){
        if (gpsDataSet == null)
            gpsDataSet = map.primaryGPSData;
        if (gpsDataSet == null)
            return null;

        if (gpsDataSet.gpsTimestamps.length == 0)
            return null;
        if (time <= gpsDataSet.gpsTimestamps[0])
            return gpsDataSet.gpsPositions[0];
        if (time >= gpsDataSet.gpsTimestamps[gpsDataSet.gpsTimestamps.length - 1])
            return gpsDataSet.gpsPositions[gpsDataSet.gpsPositions.length-1];
        var endIndex;
        for (endIndex = 1; endIndex < gpsDataSet.gpsTimestamps.length && gpsDataSet.gpsTimestamps[endIndex] < time; endIndex++);
        var startIndex = endIndex - 1;
        var percentThrough = (time - gpsDataSet.gpsTimestamps[startIndex]) / (gpsDataSet.gpsTimestamps[endIndex] - gpsDataSet.gpsTimestamps[startIndex]);

        var projection = map.getProjection();
        var startPoint = projection.fromLatLngToPoint(gpsDataSet.gpsPositions[startIndex]);
        var endPoint = projection.fromLatLngToPoint(gpsDataSet.gpsPositions[endIndex]);

        var x = (endPoint.x - startPoint.x) * percentThrough + startPoint.x;
        var y = (endPoint.y - startPoint.y) * percentThrough + startPoint.y;
        var latlng = projection.fromPointToLatLng(new google.maps.Point(x,y));
        return new google.maps.LatLng(latlng.lat(),latlng.lng());

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
        if (marker.circle == null)
            zoomOnPoint(map,marker.getPosition());
        else
            map.fitBounds(marker.circle.getBounds());
    }

    function hideData(map,connectorId){
        if (connectorId == "google_latitude"){
            map.hideGPSData();
        }
        if (!map.hasData(connectorId))
            return;
        if (map.connectorSelected == connectorId){
            map.infoWindow.close();
            map.connectorSelected = null;
        }
        for (var i = 0; i < map.markers[connectorId].length; i++){
            map.markers[connectorId][i].setMap(null);
        }
        if (map.gpsData[connectorId] != null)
            map.gpsData[connectorId].gpsLine.setMap(null);
    }

    function showData(map,connectorId){
        if (connectorId == "google_latitude"){
            map.showGPSData();
        }
        if (!map.hasData(connectorId))
            return;
        for (var i = 0; i < map.markers[connectorId].length; i++){
            map.markers[connectorId][i].setMap(map);
            if (map.selectedMarker == map.markers[connectorId][i])
                map.selectedMarker.showCircle();
        }
        if (map.gpsData[connectorId] != null)
            map.gpsData[connectorId].gpsLine.setMap(map);
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

    function isFullyInitialized(map){
        return map.getProjection() != null;
    }

    function hideGPSData(map){
        map.gpsLine.setMap(null);
        if (map.currentHighlightedLine != null)
            map.currentHighlightedLine.setMap(null);
        if (map.highlightSection != null)
            map.highlightSection.setMap(null);
    }

    function showGPSData(map){
        map.gpsLine.setMap(map);
        if (map.currentHighlightedLine != null)
            map.currentHighlightedLine.setMap(map);
        if (map.highlightSection != null)
            map.highlightSection.setMap(map);
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

    function showNoGPSDisplay(map){
        if (map.noGPSDiv == null){
            map.noGPSDiv = $("<div id='nogeolocation' style='background:white;'>No Geolocation Data Available</div>");
            map.controls[google.maps.ControlPosition.TOP].push(map.noGPSDiv[0]);
        }
        else{
            map.noGPSDiv.css("display","block");
        }
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

    function zoomOnItemAndClick(map,itemId){
        var targetMarker = null;
        for (var i = 0, li = map.markerList.length; i < li && targetMarker == null; i++){
            if (map.markerList[i].item != null && map.markerList[i].item.id == itemId)
                targetMarker = map.markerList[i];
        }
        if (targetMarker != null){
            map.zoomOnMarker(targetMarker);
            google.maps.event.trigger(targetMarker,"click");
        }
    }

    return {
        isDisplayable: isDisplayable,
        filterGPSData: filterGPSData,
        newMap: function(center,zoom,divId,hideControls,mapTypeId ){ //creates and returns a google map with extended functionality
            var options = {
                zoom : zoom,
                center: center,
                scrollwheel : true,
                streetViewControl : false,
                mapTypeId : mapTypeId != null ? mapTypeId : google.maps.MapTypeId.ROADMAP
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
                        }
                    })
                }
                else{//old map, remove everything!
                    for (var i = 0, li = map.markerList.length; i < li; i++){
                        map.markerList[i].setMap(null);
                    }
                    for (var dataset in map.gpsData){
                        map.gpsData[dataset].gpsLine.setMap(null);
                    }
                }
                showNoGPSDisplay(map);
                map.currentHighlightedLine = null;
                map.highlightSection = null;
                map.connectorSelected = null;
                map.selectedMarker = null;

                map.markers = {};
                map.markerList = [];

                map.gpsBounds = null;

                map.gpsData = {};
                map.primaryGPSData = null;

            }

            map.reset();


            map.addGPSData = function(gpsData,config,clickable){addGPSData(map,gpsData, config,clickable)};
            map.addData = function(connectorData, connectorInfoId,clickable){return addData(map,connectorData, connectorInfoId,clickable)};
            map.addAddresses = function(addresses,clickable){addAddresses(map,addresses,clickable)}
            map.getLatLngOnGPSLine = function(time,gpsDataSet){return getLatLngOnGPSLine(map,time,gpsDataSet)};
            map.createPolyLineSegment = function(gpsDataSet, start,end,options){return createPolyLineSegment(map, gpsDataSet,start,end,options)};
            map.getFirstIndexAfter = function(gpsDataSet, time){return getFirstIndexAfter(gpsDataSet,time)};
            map.getFirstIndexBefore = function(gpsDataSet, time){return getFirstIndexBefore(gpsDataSet,time)};
            map.zoomOnTimespan = function(start,end){zoomOnTimespan(map,start,end)};
            map.highlightTimespan = function(start,end){highlightTimespan(map,start,end)};
            map.showData = function(connectorId){showData(map,connectorId)};
            map.hideData = function(connectorId){hideData(map,connectorId)};
            map.hasData = function(connectorId){return hasData(map,connectorId)};
            map.showGPSData = function(){showGPSData(map)};
            map.hideGPSData = function(){hideGPSData(map)};
            map.addItem = function(item,clickable){return addItemToMap(map,item,clickable)};
            map.zoomOnPoint = function(point){zoomOnPoint(map,point)};
            map.zoomOnMarker = function(marker){zoomOnMarker(map,marker)};
            map.enhanceMarker = function(marker,start,end){enhanceMarker(map,marker,start,end)};
            map.enhanceMarkerWithItem = function(marker,item){enhanceMarkerWithItem(map,marker,item)};
            map.isFullyInitialized = function(){return isFullyInitialized(map)};
            map.isPreserveViewChecked = function(){return false;}
            map.zoomOnItemAndClick = function(itemId){
                zoomOnItemAndClick(map,itemId);
            }
            map._oldFitBounds = map.fitBounds;
            map.fitBounds = function(bounds,isPreservedView){
                if (bounds == null)
                    return;
                map._oldFitBounds(bounds);
                var zoomLevel = map.getZoom();
                fixZooming(map,zoomLevel,isPreservedView);
            }
            if (!hideControls){
                createMapPositionControls(map);
            }
            return map;
        }
    }
});