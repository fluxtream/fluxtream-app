define([], function() {
	
	function render(digest, timeUnit) {
		require(["text!applications/log/widgets/summary/summary.html"], function(template) {
			$("#widgets").append(template);
		});
	}
	
	var summaryWidget = {};
	summaryWidget.render = render;
	return summaryWidget;
	
});
