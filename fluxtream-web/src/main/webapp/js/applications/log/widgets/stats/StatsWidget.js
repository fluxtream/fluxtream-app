define(["applications/log/widgets/Widget",
        "applications/log/App"], function(Widget, Log) {
	
	function render(digest, timeUnit) {
		$.ajax({
			url : "/widgets/stats",
			success: populateTemplate
		});
	}
	
	function populateTemplate(html) {
		$.ajax({
			url : "/widgets/stats.json",
			success: function(json) {
				html = $.mustache(html, json);
				$("#widgets").append(html);
			}
		});
	}
	
	var statsWidget = new Widget("stats", "Candide Kemmler", "icon-chart");
	statsWidget.render = render;
	return statsWidget;
	
});
