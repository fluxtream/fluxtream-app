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
                    that.populateTemplate({dashboards : dashboards,
                                              release : window.FLX_RELEASE_NUMBER});
                }
               }
        );
        App.fullHeight();
    }

    function populateTemplate(dashboardsTemplateData) {
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
                             fetchWidgets(getActiveWidgets(dashboardsTemplateData.dashboards));
                         },
                         dashboardsTemplateData
        );
    };

    function fetchWidgets(activeWidgets) {
        var rows = [];
        var row = {widgets:[]};
        for (var i=0; i<activeWidgets.length; i++) {
            if(i%3==0) {
                row = {widgets:[]};
                rows.push(row);
            }
            row.widgets.push(activeWidgets[i]);
        }
        console.log({rows: rows});
        App.loadMustacheTemplate("applications/calendar/tabs/dashboards/manageDashboardsTemplate.html","widgetsGrid", function(template){
            var html = template.render({rows: rows});
            $("#dashboardsTab .tab-content").empty();
            $("#dashboardsTab .tab-content").append(html);
            fireWidgets(activeWidgets)
        });
    }

    function fireWidgets(activeWidgets) {
        for (var i=0; i<activeWidgets.length; i++) {
            require([activeWidgets[i].WidgetRepositoryURL + "/"
                                       + activeWidgets[i].WidgetName + "/"
                                       + activeWidgets[i].WidgetName + ".js"], function(WidgetModule) {
                WidgetModule.load(digest);
            });
        }
    }

    function getActiveWidgets(dashboards) {
        var activeDashboard = getActiveDashboard(dashboards);
        return activeDashboard.widgets;
    }

    function getActiveDashboard(dashboards) {
        for (var i=0; i<dashboards.length; i++) {
            console.log("dashboard id: " + dashboards[i].id);
            if (dashboards[i].id===dashboardsTab.activeDashboard)
                return dashboards[i];
        }
        return null;
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
