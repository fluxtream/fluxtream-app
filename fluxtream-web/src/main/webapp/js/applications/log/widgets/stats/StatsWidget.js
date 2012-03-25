define(["applications/log/widgets/Widget"], function(Widget) {
	
	function render(digest, timeUnit) {
		require(["text!applications/log/widgets/stats/" + timeUnit.toLowerCase() + "Stats.html"], function(template) {
			$("#widgets").append(template);
		});
	}
	
	var statsWidget = new Widget("clock", "Candide Kemmler", "icon-chart");
	statsWidget.render = render;
	return statsWidget;
	
});
