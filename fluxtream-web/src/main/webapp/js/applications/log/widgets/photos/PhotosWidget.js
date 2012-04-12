define(["applications/log/widgets/Widget",
        "applications/log/App"], function(Widget, Log) {

	function render(digest, timeUnit) {
		$.ajax({
			url : "/widgets/photos",
			success: function(html) {
				$("#widgets").append(html);
			}
		});
	}
	
	var photosWidget = new Widget("photos", "Candide Kemmler", "icon-camera");
	photosWidget.render = render;
	return photosWidget;

});
