define(["applications/log/widgets/Widget",
        "applications/log/App"], function(Widget, Log, CaloriesBurned) {
	
	var digest;

	function render(digestInfo, timeUnit) {
		digest = digestInfo;
		this.getUrl("/widgets/dashboard", "dashboard-" + timeUnit, populateTemplate);
	}
	
	function populateTemplate(html) {
		$.ajax({
			url : "/widgets/dashboard.json",
			success: function(dashboardData) {
				for(var i=0; i<dashboardData.required.length; i++) {
					require([dashboardData.required[i]], function(dashboardWidgetModule) {
						dashboardWidgetModule.render(digest, dashboardData);
					});
				}
			}
		});
	}
	
	var dashboardWidget = new Widget("dashboard", "Candide Kemmler", "icon-chart");
	dashboardWidget.render = render;
	return dashboardWidget;
	
});
