define(["applications/calendar/tabs/Tab",
        "applications/calendar/App"], function(Tab, Calendar) {

	function render(digest, timeUnit) {
		this.getUrl("/tabs/photos", "photos", null, true);
	}
	
	var photosTab = new Tab("photos", "Candide Kemmler", "icon-camera");
	photosTab.render = render;
	return photosTab;

});
