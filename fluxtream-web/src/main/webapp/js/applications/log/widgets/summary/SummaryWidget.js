define([], function() {
	
	function render(digest, timeUnit) {
		console.log("timeUnit: " + timeUnit);
		require(["text!applications/log/widgets/summary/" + timeUnit.toLowerCase() + "Summary.html"], function(template) {
			$("#widgets").append(template);
		});
	}
	
	var summaryWidget = {};
	summaryWidget.render = render;
	return summaryWidget;
	
});
