define(["applications/calendar/tabs/Tab",
        "applications/calendar/App",
       "applications/calendar/tabs/map/MapUtils"], function(Tab, Calendar, MapUtils) {

	var map = null;

    function render(digest, timeUnit) {
        this.getTemplate("text!applications/calendar/tabs/map/map.html", "map", function(){setup(digest);});
    }

    function setup(digest) {
        $("#tooltips").load("/calendar/tooltips");
        App.fullHeight();
        $("#the_map").empty();
        $("#selectedConnectors").empty();
        $("#mapFit").unbind("click");

        var bounds = null;
        if (map != null)
            bounds = map.getBounds();

        if (digest.homeAddress != null && digest.homeAddress.isSet){
            map = MapUtils.newMap(new google.maps.LatLng(digest.homeAddress.latitude,digest.homeAddress.longitude),8,"the_map",false);
        }
        else{
            map = MapUtils.newMap(new google.maps.LatLng(0,0),8,"the_map",false);
        }

        if (digest!=null && digest.cachedData!=null &&
            typeof(digest.cachedData.google_latitude)!="undefined"
                && digest.cachedData.google_latitude !=null &&
            digest.cachedData.google_latitude.length>0) { //make sure gps data is available before trying to display it
            map.addGPSData(digest.cachedData.google_latitude);
            if (!document.getElementById("perserveViewCheckBox").checked)
                bounds = map.gpsBounds;

            var checkedContainer = $("#selectedConnectors");
            for(var objectTypeName in digest.cachedData) {
                if (digest.cachedData[objectTypeName]==null||typeof(digest.cachedData[objectTypeName])=="undefined")
                    continue;
                var dataAdded = map.addData(digest.cachedData[objectTypeName], objectTypeName, true);
                if (dataAdded || objectTypeName == "google_latitude"){
                    var button = $('<button class="btnList btn btnListChecked enabled">' + objectTypeName + '</button>');
                    button.click({button:button,objectTypeName:objectTypeName,isGoogleLatitude:objectTypeName == "google_latitude"},function(event){
                        buttonClicked(event.data.button,event.data.objectTypeName,event.data.isGoogleLatitude);
                    });
                    checkedContainer.append(button);
                    checkedContainer.append("&nbsp;");
                }
            }

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

    function buttonClicked(button,connectorName,isGoogleLatitude){
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
                map.hideData(connectorName);
            }
        }
        else{
            button.removeClass("btn-inverse");
            button.addClass("btnListChecked");
            if (isGoogleLatitude){
                map.showGPSData();
            }
            else{
                map.showData(connectorName);
            }
        }

        button.removeClass("disabled");
        button.addClass("enabled");
    }

    var mapTab = new Tab("map", "Candide Kemmler", "icon-map-marker", true);
    mapTab.render = render;
    return mapTab;

});
