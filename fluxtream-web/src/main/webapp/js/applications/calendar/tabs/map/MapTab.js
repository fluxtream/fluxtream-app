define(["applications/calendar/tabs/Tab",
        "applications/calendar/App"], function(Tab, Calendar) {

	var map = null;
	
	function render(digest, timeUnit) {
		this.getTemplate("text!applications/calendar/tabs/map/map.html", "map", function(){setup(digest);});
	}
	
	function setup(digest) {
		App.fullHeight();
        if (digest!=null && digest.cachedData!=null &&
            typeof(digest.cachedData.google_latitude)!="undefined"
            && digest.cachedData.google_latitude !=null &&
            digest.cachedData.google_latitude.length>0) {
            if ($("#the_map > .emptyList").length>0)
                $("#the_map").empty();
            var myOptions = {
                zoom : 11,
                scrollwheel : true,
                streetViewControl : false,
                mapTypeId : google.maps.MapTypeId.ROADMAP
            };
            map = new google.maps.Map(document.getElementById("the_map"),
                myOptions);
            setMapPosition(digest.cachedData.google_latitude[0].position[0],digest.cachedData.google_latitude[0].position[1], 9);
            var myLatLngs=new Array();
            var i;
            for (i = 0; i < digest.cachedData.google_latitude.length; i++){
                myLatLngs[i] = new google.maps.LatLng(digest.cachedData.google_latitude[i].position[0],digest.cachedData.google_latitude[i].position[1]);
            }
            new google.maps.Polyline({map:map, path:myLatLngs});

        } else {
            $("#the_map").empty();
            $("#the_map").removeAttr("style");
            $("#the_map").append("<div class=\"emptyList\">(no location data)</div>");
        }
	}
	
	function setMapPosition(pos_x, pos_y, zoomLevel) {
		var center = new google.maps.LatLng(pos_x, pos_y);
		map.setCenter(center);
		map.setZoom(zoomLevel);
	}

	var mapTab = new Tab("map", "Candide Kemmler", "icon-map-marker", true);
	mapTab.render = render;
	return mapTab;

});
