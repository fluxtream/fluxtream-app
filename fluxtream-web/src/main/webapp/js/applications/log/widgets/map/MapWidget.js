define(["applications/log/widgets/Widget",
        "applications/log/App"], function(Widget, Log) {

	var mapWidget = new Widget("map", "Candide Kemmler", "icon-map-marker");
	var map = null;
	
	function render(digest, timeUnit) {
		mapWidget.getTemplate("text!applications/log/widgets/map/map.html", setup);
	}
	
	function setup() {
		App.fullHeight();
		var myOptions = {
			zoom : 11,
			scrollwheel : false,
			streetViewControl : false,
			mapTypeId : google.maps.MapTypeId.ROADMAP
		};
		map = new google.maps.Map(document.getElementById("the_map"),
				myOptions);
		setMapPosition(50.858519,4.484482, 9);
	}
	
	function setMapPosition(pos_x, pos_y, zoomLevel) {
		var center = new google.maps.LatLng(pos_x, pos_y);
		map.setCenter(center);
		map.setZoom(zoomLevel);
	}

	mapWidget.render = render;
	return mapWidget;

});
