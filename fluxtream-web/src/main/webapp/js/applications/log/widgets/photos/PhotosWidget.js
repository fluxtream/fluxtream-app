define(["applications/log/widgets/Widget",
        "applications/log/App"], function(Widget, Log) {

	function render(digest, timeUnit) {
		var url =  "/widgets/photos/" + Log.widgetState;
		console.log("rendering photos widgets: " + url);
		$.ajax({
			url : url,
			success: function(html) {
				$("#widgets").append(html);
			}
		});
	}
	
	var photosWidget = new Widget("photos", "Candide Kemmler", "icon-camera");
	photosWidget.render = render;
	return photosWidget;

});
