define(["applications/calendar/tabs/Tab",
        "applications/calendar/App", "applications/calendar/tabs/dashboards/ManageDashboards",
        "applications/calendar/tabs/dashboards/AddWidget"],
       function(Tab, Calendar, ManageDashboards, AddWidget) {
	
	var digest, dashboardData;

	function render(digestInfo, timeUnit) {
        _.bindAll(this);
		digest = digestInfo;
        var that = this;
        $.ajax({
                url: "/api/dashboards",
                success: function(dashboards) {
                    for (var i=0; i<dashboards.length; i++) {
                        if (dashboards[i].active)
                            dashboardsTab.activeDashboard = dashboards[i].id;
                    }
                    that.populateTemplate({"dashboards" : dashboards});
                }
               }
        );
        App.fullHeight();
    }

    function populateTemplate(dashboards) {
        this.getTemplate("text!applications/calendar/tabs/dashboards/dashboards.html", "dashboards",
                         function() {
                             $("#addWidgetButton").click(addWidget);
                             $("#manageDashboardsButton").click(manageDashboards);
                             $(".dashboardName").click(function(evt) {
                                 var dashboardId = $(evt.target).parent().attr("id").substring("dashboard-".length);
                                 dashboardsTab.activeDashboard = dashboardId;
                                 $.ajax({
                                     url: "/api/dashboards/" + dashboardsTab.activeDashboard + "/active",
                                     type: "PUT"
                                 });
                             });
                         },
                         dashboards
        );
    }

    function addWidget() {
        AddWidget.show(dashboardsTab.activeDashboard);
    }

    function manageDashboards() {
        ManageDashboards.show();
    }

    function setup(digest, timeUnit) {
        status.handleComments();
    }

    var dashboardsTab = new Tab("dashboards", "Candide Kemmler", "icon-chart", true);
	dashboardsTab.render = render;
    dashboardsTab.populateTemplate = populateTemplate;
	return dashboardsTab;
	
});
