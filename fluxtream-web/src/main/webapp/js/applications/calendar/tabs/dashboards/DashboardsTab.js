define(["applications/calendar/tabs/Tab",
        "applications/calendar/App"], function(Tab, Calendar) {
	
	var digest, dashboardData;

	function render(digestInfo, timeUnit) {
		digest = digestInfo;
        var that = this;
        console.log("loading dashboards...");
        $.ajax({
            url : "/tabs/dashboard.json",
            success: function(tabData) {
                dashboardData = tabData;
                that.getUrl("/tabs/dashboard", "dashboard-" + timeUnit, populateTemplate, true, tabData);
            }
        });
	}
	
	function populateTemplate(html) {
        for(var i=0; i<dashboardData.required.length; i++) {
            require([dashboardData.required[i]], function(dashboardTabModule) {
                dashboardTabModule.render(digest, dashboardData);
            });
        }
    }
	
	var dashboardsTab = new Tab("dashboards", "Candide Kemmler", "icon-chart", true);
	dashboardsTab.render = render;
	return dashboardsTab;
	
});
