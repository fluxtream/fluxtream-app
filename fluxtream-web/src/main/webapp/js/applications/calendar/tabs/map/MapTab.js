define(["applications/calendar/tabs/Tab",
        "applications/calendar/App"], function(Tab, Calendar) {

	var map = null;
	
	function render(digest, timeUnit) {
		this.getTemplate("text!applications/calendar/tabs/map/map.html", "map", setup);
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

	var mapTab = new Tab("map", "Candide Kemmler", "icon-map-marker");
	mapTab.render = render;
	return mapTab;

});
