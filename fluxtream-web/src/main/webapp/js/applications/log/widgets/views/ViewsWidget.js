define([], function() {
	
	function render(digest, timeUnit) {
		console.log("timeUnit: " + timeUnit);
		require(["text!applications/log/widgets/views/views.html"], function(template) {
			$("#widgets").append(template);
		});
	}
	
	var widget = {};
	widget.render = render;
	return widget;
	
});
