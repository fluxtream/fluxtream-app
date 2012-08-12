define(["core/Tab",
        "applications/calendar/App",
       "applications/calendar/tabs/map/MapUtils"], function(Tab, Calendar, MapUtils) {

	var map = null;
    var digestData = null;
    var preserveView = false;

    var oldState = null;

    function render(params) {
        params.setTabParam(null);
        this.getTemplate("text!applications/calendar/tabs/map/map.html", "map", function(){
            if (params.calendarState == oldState)
                return;
            else
                oldState = params.calendarState;
            setup(params.digest,params.calendarState,params.connectorEnabled);
        });
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

        map = MapUtils.newMap(new google.maps.LatLng(addressToUse.latitude,addressToUse.longitude),16,"the_map",false);
        map.setPreserveView(preserveView);
        if (!map.isPreserveViewChecked())
            bounds = map.getBounds();

        if (digest!=null && digest.cachedData!=null &&
            typeof(digest.cachedData["google_latitude-location"])!="undefined"
                && digest.cachedData["google_latitude-location"] !=null &&
            digest.cachedData["google_latitude-location"].length>0) { //make sure gps data is available before trying to display it
            map.addGPSData(digest.cachedData["google_latitude-location"],true);

            if (!map.isPreserveViewChecked())
                bounds = map.gpsBounds;
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
        showData();
        if (bounds != null){
            map.fitBounds(bounds,map.isPreserveViewChecked());
        }
        map.preserveViewCheckboxChanged = function(){
            preserveView = map.isPreserveViewChecked();
        }
	}

    function showData(){
        if (!map.isFullyInitialized()){
            $.doTimeout(100,showData);
            return;
        }
        var digest = digestData;
        map.addAddresses(digest.addresses,true);
        if (digest!=null && digest.cachedData!=null &&
            typeof(digest.cachedData["google_latitude-location"])!="undefined"
                && digest.cachedData["google_latitude-location"] !=null &&
            digest.cachedData["google_latitude-location"].length>0){
            for(var objectTypeName in digest.cachedData) {
                if (digest.cachedData[objectTypeName]==null||typeof(digest.cachedData[objectTypeName])=="undefined")
                    continue;
                map.addData(digest.cachedData[objectTypeName], objectTypeName, true);
            }
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

    function connectorDisplayable(connector){
        for (var i = 0; i < connector.facetTypes.length; i++){
            var config = App.getFacetConfig(connector.facetTypes[i]);
            if (config.map)
                return true;
        }
        return false;
    }

    var mapTab = new Tab("calendar", "map", "Candide Kemmler", "icon-map-marker", true);
    mapTab.render = render;
    mapTab.connectorToggled = connectorToggled;
    mapTab.connectorDisplayable = connectorDisplayable;
    return mapTab;

});
