define(["applications/calendar/tabs/Tab",
        "applications/calendar/App",
       "applications/calendar/tabs/map/MapUtils"], function(Tab, Calendar, MapUtils) {

	var map = null;
    var digestData = null;

    function render(digest, timeUnit, calendarState, connectorEnabled) {
        $("#filtersContainer").show();
        this.getTemplate("text!applications/calendar/tabs/map/map.html", "map", function(){setup(digest,calendarState,connectorEnabled);});
    }

    function setup(digest, calendarState,connectorEnabled) {
        digestData  = digest;
        App.fullHeight();
        $("#the_map").empty();
        $("#mapFit").unbind("click");

        var bounds = null;
        if (map != null)
            bounds = map.getBounds();

        var addressToUse = {latitude:0,longitude:0};
        if (digest.addresses.ADDRESS_HOME != null && digest.addresses.ADDRESS_HOME.length != 0)
            addressToUse = digest.addresses.ADDRESS_HOME[0];

        map = MapUtils.newMap(new google.maps.LatLng(addressToUse.latitude,addressToUse.longitude),17,"the_map",false);
        if (!document.getElementById("perserveViewCheckBox").checked)
            bounds = map.getBounds();

        if (digest!=null && digest.cachedData!=null &&
            typeof(digest.cachedData.google_latitude)!="undefined"
                && digest.cachedData.google_latitude !=null &&
            digest.cachedData.google_latitude.length>0) { //make sure gps data is available before trying to display it
            map.addGPSData(digest.cachedData.google_latitude);

            if (!document.getElementById("perserveViewCheckBox").checked)
                bounds = map.gpsBounds;

            showData();
            for (var i = 0; i < digest.selectedConnectors.length; i++){
                if (!connectorEnabled[digest.selectedConnectors[i].connectorName])
                    for (var j = 0; j < digest.selectedConnectors[i].facetTypes.length; j++){
                        map.hideData(digest.selectedConnectors[i].facetTypes[j]);
                    }
            }

            $("#mapFit").show();
            $("#mapFit").click(function(){
                map.fitBounds(map.gpsBounds);
            });

        } else {
            $("#mapFit").hide();
        }
        if (bounds != null)
            map.fitBounds(bounds);
	}

    function showData(functionName){
        if (functionName != null)
            delete window[functionName];
        if (!map.isFullyInitialized()){
            var functionName = "mapTimingFunction" + new Date().getUTCMilliseconds();
            window[functionName] = showData;
            setTimeout("window." + functionName + "(\"" + functionName + "\");",100);
            return;
        }
        var digest = digestData;
        for(var objectTypeName in digest.cachedData) {
            if (digest.cachedData[objectTypeName]==null||typeof(digest.cachedData[objectTypeName])=="undefined")
                continue;
            map.addData(digest.cachedData[objectTypeName], objectTypeName, true);
        }

    }

    function connectorToggled(connectorName,objectTypeNames,enabled){
        for (var i = 0; i < objectTypeNames.length; i++){
            if (enabled)
                map.showData(objectTypeNames[i]);
            else
                map.hideData(objectTypeNames[i]);
        }
    }

    var mapTab = new Tab("map", "Candide Kemmler", "icon-map-marker", true);
    mapTab.render = render;
    mapTab.connectorToggled = connectorToggled;
    return mapTab;

});
