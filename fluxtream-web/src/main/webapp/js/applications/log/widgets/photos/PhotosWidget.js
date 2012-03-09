define([], function() {

	function render(digest, timeUnit) {
		require(["text!applications/log/widgets/photos/photos.html"], function(template) {
			$("#widgets").append(template);
		});
	}
	
	var photosWidget = {};
	photosWidget.render = render;
	return photosWidget;

});
