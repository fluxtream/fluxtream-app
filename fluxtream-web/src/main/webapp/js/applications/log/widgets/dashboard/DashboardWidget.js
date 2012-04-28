define(["applications/log/widgets/Widget",
        "applications/log/App"], function(Widget, Log, CaloriesBurned) {
	
	var digest, dashboardData;

	function render(digestInfo, timeUnit) {
        console.log("rendering widget...");
		digest = digestInfo;
        var that = this;
        $.ajax({
            url : "/widgets/dashboard.json",
            success: function(widgetData) {
                console.log("haha some data...");
                dashboardData = widgetData;
                that.getUrl("/widgets/dashboard", "dashboard-" + timeUnit, populateTemplate, true, widgetData);
            }
        });
	}
	
	function populateTemplate(html) {
        console.log("populating with data...");
        for(var i=0; i<dashboardData.required.length; i++) {
            require([dashboardData.required[i]], function(dashboardWidgetModule) {
                dashboardWidgetModule.render(digest, dashboardData);
            });
        }
    }
	
	var dashboardWidget = new Widget("dashboard", "Candide Kemmler", "icon-chart");
	dashboardWidget.render = render;
	return dashboardWidget;
	
});
