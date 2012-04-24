define(["applications/log/widgets/Widget",
        "applications/log/App"], function(Widget, Log) {

	function render(digest, timeUnit) {
		this.getUrl("/widgets/photos", "photos", null, true);
	}
	
	var photosWidget = new Widget("photos", "Candide Kemmler", "icon-camera");
	photosWidget.render = render;
	return photosWidget;

});
