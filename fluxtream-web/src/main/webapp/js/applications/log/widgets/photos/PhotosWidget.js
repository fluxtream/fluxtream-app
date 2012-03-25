define(["applications/log/widgets/Widget"], function(Widget) {

	function render(digest, timeUnit) {
		require(["text!applications/log/widgets/photos/photos.html"], function(template) {
			$("#widgets").append(template);
		});
	}
	
	var photosWidget = new Widget("clock", "Candide Kemmler", "icon-camera");
	photosWidget.render = render;
	return photosWidget;

});
