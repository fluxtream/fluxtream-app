define(["applications/log/widgets/Widget",
        "applications/log/App"], function(Widget, Log) {

	var map = null;
	
	function render(digest, timeUnit) {
		this.getTemplate("text!applications/log/widgets/map/map.html", "map", function(){setup(digest);});
	}
	
	function setup(digest) {
		App.fullHeight();
		var myOptions = {
			zoom : 11,
			scrollwheel : false,
			streetViewControl : false,
			mapTypeId : google.maps.MapTypeId.ROADMAP
		};
		map = new google.maps.Map(document.getElementById("the_map"),
				myOptions);
		setMapPosition(digest.cachedData.google_latitude[0].position[0],digest.cachedData.google_latitude[0].position[1], 9);
        var i = 0;
        for (i = 0; i < digest.cachedData.google_latitude.length; i++){
            var myLatlng = new google.maps.LatLng(digest.cachedData.google_latitude[i].position[0],digest.cachedData.google_latitude[i].position[1]);
            new google.maps.Marker({
                position: myLatlng,
                map: map,
                title:"latitude point: " + i
            });
        }
	}
	
	function setMapPosition(pos_x, pos_y, zoomLevel) {
		var center = new google.maps.LatLng(pos_x, pos_y);
		map.setCenter(center);
		map.setZoom(zoomLevel);
	}

	var mapWidget = new Widget("map", "Candide Kemmler", "icon-map-marker");
	mapWidget.render = render;
	return mapWidget;

});
