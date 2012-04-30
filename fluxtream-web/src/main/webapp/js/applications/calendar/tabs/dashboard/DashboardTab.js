define(["applications/calendar/tabs/Tab",
        "applications/calendar/App"], function(Tab, Calendar, CaloriesBurned) {
	
	var digest, dashboardData;

	function render(digestInfo, timeUnit) {
		digest = digestInfo;
        var that = this;
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
	
	var dashboardTab = new Tab("dashboard", "Candide Kemmler", "icon-chart", true);
	dashboardTab.render = render;
	return dashboardTab;
	
});
