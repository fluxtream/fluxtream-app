define(["applications/calendar/tabs/Tab",
        "applications/calendar/App",
       "applications/calendar/tabs/map/MapUtils"], function(Tab, Calendar, MapUtils) {

	var map = null;
    var digestData = null;

    function render(digest, timeUnit, calendarState) {
        this.getTemplate("text!applications/calendar/tabs/map/map.html", "map", function(){setup(digest,calendarState);});
    }

    function setup(digest, calendarState) {
        digestData  = digest;
        $("#tooltips").load("/calendar/tooltips");
        App.fullHeight();
        $("#the_map").empty();
        $("#selectedConnectors").empty();
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

            $.ajax("/api/guest/" + App.getUsername() + "/photo/" + calendarState,{
                success: function(data, textStatus, jqXHR){
                    if (data != null && data.result == null && data.length != 0)
                        map.addData(data,data[0].type,true);
                }

            });

            if (!document.getElementById("perserveViewCheckBox").checked)
                bounds = map.gpsBounds;

            showData();

            $("#mapFit").show();
            $("#mapFit").click(function(){
                map.fitBounds(map.gpsBounds);
            });

        } else {
            $("#mapFit").hide();
            $("#selectedConnectors").append("<div class=\"emptyList\">(no location data)</div>");
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
        var checkedContainer = $("#selectedConnectors");
        for(var objectTypeName in digest.cachedData) {
            if (digest.cachedData[objectTypeName]==null||typeof(digest.cachedData[objectTypeName])=="undefined")
                continue;
            map.addData(digest.cachedData[objectTypeName], objectTypeName, true);
        }

        for (var i = 0; i < digest.selectedConnectors.length; i++){
            var displayable = false;
            for (var j = 0; j < digest.selectedConnectors[i].facetTypes.length && !displayable; j++){
                displayable = MapUtils.isDisplayable(digest.selectedConnectors[i].facetTypes[j]);
            }
            var isGoogleLatitude = digest.selectedConnectors[i].connectorName == "google_latitude";
            if (displayable || isGoogleLatitude){
                var enabled = false;
                for (var j = 0; j < digest.selectedConnectors[i].facetTypes.length && !enabled; j++){
                    enabled = map.hasData(digest.selectedConnectors[i].facetTypes[j]);
                }
                enabled = (enabled || isGoogleLatitude) ? "enabled" : "disabled";
                var button = $('<button class="btnList btn btnListChecked ' + enabled + '">' + digest.selectedConnectors[i].prettyName + '</button>');
                button.click({button:button,objectTypeNames:digest.selectedConnectors[i].facetTypes,isGoogleLatitude:isGoogleLatitude}, function(event){
                    buttonClicked(event.data.button,event.data.objectTypeNames,event.data.isGoogleLatitude);
                })
                checkedContainer.append(button);
                checkedContainer.append("&nbsp;");
            }
        }

    }

    function buttonClicked(button,connectorNames,isGoogleLatitude){
        if (button.hasClass("disabled"))
            return;
        button.removeClass("enabled");
        button.addClass("disabled");
        if (button.hasClass("btnListChecked")){
            button.removeClass("btnListChecked");
            button.addClass("btn-inverse");
            if (isGoogleLatitude){
                map.hideGPSData();
            }
            else{
                for (var i = 0; i < connectorNames.length; i++){
                    map.hideData(connectorNames[i]);
                }
            }
        }
        else{
            button.removeClass("btn-inverse");
            button.addClass("btnListChecked");
            if (isGoogleLatitude){
                map.showGPSData();
            }
            else{
                for (var i = 0; i < connectorNames.length; i++){
                    map.showData(connectorNames[i]);
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
