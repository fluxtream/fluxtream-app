define(["applications/log/widgets/Widget",
        "applications/log/App"], function(Widget, Log) {
	
	function render(digest, timeUnit) {
		$.ajax({
			url : "/widgets/dashboard",
			success: populateTemplate
		});
	}
	
	function populateTemplate(html) {
		$.ajax({
			url : "/widgets/dashboard.json",
			success: function(json) {
				html = $.mustache(html, json);
				$("#widgets").append(html);
			}
		});
	}
	
	var dashboardWidget = new Widget("dashboard", "Candide Kemmler", "icon-chart");
	dashboardWidget.render = render;
	return dashboardWidget;
	
});
