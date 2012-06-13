define(["applications/calendar/tabs/Tab",
        "applications/calendar/App"], function(Tab, Calendar) {
	
	var digest, dashboardData;

	function render(digestInfo, timeUnit) {
		digest = digestInfo;
        var that = this;
        this.getTemplate("text!applications/calendar/tabs/dashboards/dashboards.html", "dashboards",
            function() {

            }
        );
	}

	var dashboardsTab = new Tab("dashboards", "Candide Kemmler", "icon-chart", true);
	dashboardsTab.render = render;
	return dashboardsTab;
	
});
