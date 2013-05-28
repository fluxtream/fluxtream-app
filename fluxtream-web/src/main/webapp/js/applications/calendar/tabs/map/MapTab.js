define(["core/Tab",
        "applications/calendar/App",
       "applications/calendar/tabs/map/MapUtils",
       "applications/calendar/tabs/photos/PhotoUtils"], function(Tab, Calendar, MapUtils, PhotoUtils) {

	var map = null;
    var digestData = null;
    var preserveView = false;

    var lastTimestamp = null;
    var photoCarouselHTML;

    function render(params) {
        params.setTabParam(null);
        this.getTemplate("text!applications/calendar/tabs/map/map.html", "map", function(){
            if (lastTimestamp == params.digest.generationTimestamp && !params.forceReload){
                params.doneLoading();
                return;
            }
            else
                lastTimestamp = params.digest.generationTimestamp;
            setup(params.digest,params.calendarState,params.connectorEnabled,params.doneLoading);
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

    function setup(digest, calendarState,connectorEnabled,doneLoading) {
        digestData  = digest;
        App.fullHeight();
        $("#mapFit").unbind("click");

        var bounds = null;
        var addressToUse = {latitude:0,longitude:0};
        if (digest.addresses.ADDRESS_HOME != null && digest.addresses.ADDRESS_HOME.length != 0)
            addressToUse = digest.addresses.ADDRESS_HOME[0];

        if (map == null){//make new map
            map = MapUtils.newMap(new google.maps.LatLng(addressToUse.latitude,addressToUse.longitude),16,"the_map",false);
            map.infoWindowShown = function(){
                $("#the_map").find(".flx-photo").click(function(event){
                    App.makeModal(photoCarouselHTML);
                    App.carousel($(event.delegateTarget).attr("photoId"));
                });
            }
        }
        else{//recycle old map
            if (map.isPreserveViewChecked()){
                bounds = map.getBounds();
            }
            map.reset();
        }
        $("#mapFit").click(function(){
            map.fitBounds(map.gpsBounds);
        });

        showData(connectorEnabled,bounds,function(bounds){
            if (bounds != null){
                map.fitBounds(bounds,map.isPreserveViewChecked());
            }
            else{
                map.setCenter(new google.maps.LatLng(addressToUse.latitude,addressToUse.longitude));
                map.setZoom(16);
            }
            map.preserveViewCheckboxChanged = function(){
                preserveView = map.isPreserveViewChecked();
            }
            doneLoading();

        });

        photoCarouselHTML = PhotoUtils.getCarouselHTML(digest);


	}

    function showData(connectorEnabled,bounds,doneLoading){
        if (!map.isFullyInitialized()){
            $.doTimeout(100,function(){showData(connectorEnabled,bounds,doneLoading)});
            return;
        }
        var digest = digestData;
        if (digest!=null && digest.cachedData!=null &&
            typeof(digest.cachedData["google_latitude-location"])!="undefined"
                && digest.cachedData["google_latitude-location"] !=null &&
            digest.cachedData["google_latitude-location"].length>0) { //make sure gps data is available before trying to display it
            map.addGPSData(digest.cachedData["google_latitude-location"],App.getFacetConfig("google_latitude-location"),true);
        }

        for (var objectType in digest.cachedData){
            if (objectType == "google_latitude-location")
                continue;//we already showed google latitude data if it existed
            map.addGPSData(digest.cachedData[objectType],App.getFacetConfig(objectType),true)
        }

        map.addAddresses(digest.addresses,true);
        for(var objectTypeName in digest.cachedData) {
            if (digest.cachedData[objectTypeName]==null||typeof(digest.cachedData[objectTypeName])=="undefined")
                continue;
            map.addData(digest.cachedData[objectTypeName], objectTypeName, true);
        }
        for (var i = 0; i < digest.selectedConnectors.length; i++){
            if (!connectorEnabled[digest.selectedConnectors[i].connectorName])
                for (var j = 0; j < digest.selectedConnectors[i].facetTypes.length; j++){
                    map.hideData(digest.selectedConnectors[i].facetTypes[j]);
                }
        }

        doneLoading(map.isPreserveViewChecked() ? bounds : map.gpsBounds);

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
