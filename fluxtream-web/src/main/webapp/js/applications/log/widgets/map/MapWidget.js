define(["applications/log/widgets/Widget",
        "applications/log/App"], function(Widget, Log) {

	var map = null;
	
	function render(digest, timeUnit) {
		require(["text!applications/log/widgets/map/map.html"], function(template) {
			console.log("rendering map widget");
			$("#widgets").append(template);
			Log.fullHeight();
			var myOptions = {
				zoom : 11,
				scrollwheel : false,
				streetViewControl : false,
				mapTypeId : google.maps.MapTypeId.ROADMAP
			}
			map = new google.maps.Map(document.getElementById("the_map"),
					myOptions);
			setMapPosition(50.858519,4.484482, 9);
		});
	}
	
	function setMapPosition(pos_x, pos_y, zoomLevel) {
		var center = new google.maps.LatLng(pos_x, pos_y);
		map.setCenter(center);
		map.setZoom(zoomLevel);
	}

	var mapWidget = new Widget("clock", "Candide Kemmler", "icon-map-marker");
	mapWidget.render = render;
	return mapWidget;

});
