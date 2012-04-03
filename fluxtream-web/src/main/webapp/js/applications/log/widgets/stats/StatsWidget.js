define(["applications/log/widgets/Widget",
        "applications/log/App"], function(Widget, Log) {
	
	function render(digest, timeUnit) {
		require(["text!applications/log/widgets/stats/" + timeUnit.toLowerCase() + "Stats.html"], function(template) {
			$("#widgets").append(template);
			Log.fullHeight();
		});
	}
	
	var statsWidget = new Widget("clock", "Candide Kemmler", "icon-chart");
	statsWidget.render = render;
	return statsWidget;
	
});
