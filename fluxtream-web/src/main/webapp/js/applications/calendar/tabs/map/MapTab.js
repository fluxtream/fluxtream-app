define(["core/Tab",
        "applications/calendar/App",
       "applications/calendar/tabs/map/MapUtils",
       "applications/calendar/tabs/photos/PhotoUtils"], function(Tab, Calendar, MapUtils, PhotoUtils) {

	var map = null;
    var digestData = null;
    var preserveView = false;

    var lastTimestamp = null;

    var itemToShow = null;

    function render(params) {
        itemToShow = params.facetToShow;
        if (params.digest.delta && map != null && map.selectedMarker != null && map.selectedMarker.item != null){
            itemToShow = map.selectedMarker.item;
        }
        params.setTabParam(null);
        this.getTemplate("text!applications/calendar/tabs/map/map.html", "map", function(){
            if (lastTimestamp == params.digest.generationTimestamp && !params.forceReload){
                if (App.apps.calendar.dateAxisCursorPosition != null)
                    map.setCursorPosition(App.apps.calendar.dateAxisCursorPosition);
                $.doTimeout(250,function(){
                    if (itemToShow != null)
                        map.zoomOnItemAndClick(itemToShow);

                });

                params.doneLoading();
                return;
            }
            else
                lastTimestamp = params.digest.generationTimestamp;
            setup(params.digest,params.calendarState,params.connectorEnabled,params.doneLoading,params.digest.tbounds);
        });
        $(window).resize();
    }

    $(window).resize(function(){
        if (map != null){
            $.doTimeout("mapResizeHandler");//cancel original
            $.doTimeout("mapResizeHandler",100,function(){
                if (map != null)
                    google.maps.event.trigger(map,"resize");
            });
        }
    });

    function setup(digest, calendarState, connectorEnabled, doneLoading, tbounds) {
        digestData  = digest;
        App.fullHeight();
        $("#mapFit").unbind("click");

        var cursorPos = App.apps.calendar.dateAxisCursorPosition;

        var bounds = null;

        var maxTimeBounds = {min:tbounds.start / 1000, max:tbounds.end / 1000};
        
        if (map == null){//make new map
            map = MapUtils.newMap(new google.maps.LatLng(digest.metadata.mainCity.latitude,digest.metadata.mainCity.longitude),14,"the_map",false,maxTimeBounds);
            map.infoWindowShown = function(){
                $("#the_map").find(".flx-photo").click(function(event){
                    var dTarget = $(event.delegateTarget);
                    PhotoUtils.showPhotoDialog(dTarget.attr("data-deviceName"), dTarget.attr("data-channelName"),
                        dTarget.attr("data-id"), dTarget.attr("data-timestamp"),{minTime:digestData.tbounds.start,maxTime:digestData.tbounds.end});
                });
            }
        }
        else{//recycle old map
            if (map.isPreserveViewChecked() || digest.delta){
                bounds = map.getBounds();
            }
            map.reset();
            map.setMaxTimeBounds(maxTimeBounds);
        }
        if (typeof(digest.locationFetched)=="undefined"){
            Calendar.startLoading();
            $.ajax({
                url: "/api/calendar/location/" + Calendar.tabState,
                success: function(locationDigest) {
                    for (name in locationDigest.facets) {
                        Calendar.processFacets(locationDigest.facets[name]);
                        digest.facets[name] = locationDigest.facets[name];
                    }
                    digest.locationFetched = true;
                    renderDigest();
                },

                error: function(status) {
                    Calendar.handleError(status.message);
                }
            });
        }
        else{
            renderDigest();
        }
        function renderDigest(){
            map.setDigest(digest);
            $("#mapFit").unbind("click").click(function(){
                map.fitBounds(map.gpsBounds);
            });

            map.executeAfterReady(function(){
                showData(connectorEnabled,bounds,function(bounds){
                    if (bounds != null){
                        map.fitBounds(bounds,map.isPreserveViewChecked() || digestData.delta);
                    }
                    else{
                        map.setCenter(new google.maps.LatLng(digest.metadata.mainCity.latitude,digest.metadata.mainCity.longitude));
                        map.setZoom(14);
                    }
                    map.preserveViewCheckboxChanged = function(){
                        preserveView = map.isPreserveViewChecked();
                    }
                    if (itemToShow != null){
                        if (!digestData.delta)
                            map.zoomOnItemAndClick(itemToShow);
                        else
                            map.clickOnItem(itemToShow);
                    }

                    if (cursorPos != null)
                        map.setCursorPosition(cursorPos);

                    $("#mapwrapper .noDataOverlay").css("display", map.hasAnyData() ? "none" : "block");
                    doneLoading();

                });
            });
        }
	}

    function showData(connectorEnabled,bounds,doneLoading){
        var digest = digestData;
        if (digest!=null && digest.facets!=null &&
            typeof(digest.facets["google_latitude-location"])!="undefined"
                && digest.facets["google_latitude-location"] !=null &&
            digest.facets["google_latitude-location"].length>0) { //make sure gps data is available before trying to display it
            map.addGPSData(digest.facets["google_latitude-location"],App.getFacetConfig("google_latitude-location"),true);
        }

        for (var objectType in digest.facets){
            if (objectType == "google_latitude-location")
                continue;//we already showed google latitude data if it existed
            map.addGPSData(digest.facets[objectType],App.getFacetConfig(objectType),true)
        }

        map.addAddresses(digest.addresses,true);
        for(var objectTypeName in digest.facets) {
            if (digest.facets[objectTypeName]==null||typeof(digest.facets[objectTypeName])=="undefined")
                continue;
            map.addData(digest.facets[objectTypeName], objectTypeName, true);
        }
        for (var i = 0; i < digest.selectedConnectors.length; i++){
            if (!connectorEnabled[digest.selectedConnectors[i].connectorName])
                for (var j = 0; j < digest.selectedConnectors[i].facetTypes.length; j++){
                    map.hideData(digest.selectedConnectors[i].facetTypes[j]);
                }
        }

        doneLoading((map.isPreserveViewChecked() || digest.delta) ? bounds : map.gpsBounds);

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
