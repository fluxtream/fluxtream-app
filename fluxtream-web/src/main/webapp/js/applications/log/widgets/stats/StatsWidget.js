define(["applications/log/widgets/Widget",
        "applications/log/App"], function(Widget, Log) {
	
	function render(digest, timeUnit) {
		var url =  "/widgets/stats/" + Log.widgetState;
		$.ajax({
			url : url,
			success: function(html) {
				$("#widgets").append(html);
			}
		});
	}
	
	var statsWidget = new Widget("stats", "Candide Kemmler", "icon-chart");
	statsWidget.render = render;
	return statsWidget;
	
});
