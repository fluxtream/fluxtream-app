define([], function() {
	
	function render(digest, timeUnit) {
		require(["text!applications/log/widgets/stats/" + timeUnit.toLowerCase() + "Stats.html"], function(template) {
			$("#widgets").append(template);
		});
	}
	
	var statsWidget = {};
	statsWidget.render = render;
	return statsWidget;
	
});
