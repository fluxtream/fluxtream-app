define([], function() {
	
	function render(digest, timeUnit) {
		console.log("timeUnit: " + timeUnit);
		require(["text!applications/log/widgets/timeline/timeline.html"], function(template) {
			$("#widgets").append(template);
			callAFunction();
		});
	}
	
	function callAFunction() {
		
	}
	
	var widget = {};
	widget.render = render;
	return widget;
	
});
