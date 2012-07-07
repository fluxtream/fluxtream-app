define(["applications/calendar/tabs/Tab",
        "applications/calendar/App", "applications/calendar/tabs/dashboards/ManageDashboards",
        "applications/calendar/tabs/dashboards/AddWidget"],
       function(Tab, Calendar, ManageDashboards, AddWidget) {
	
	var digest, dashboardData;

	function render(digestInfo, timeUnit) {
        _.bindAll(this);
		digest = digestInfo;
        $.ajax({
                url: "/api/dashboards",
                success: function(dashboards) {
                    for (var i=0; i<dashboards.length; i++) {
                        if (dashboards[i].active)
                            dashboardsTab.activeDashboard = dashboards[i].id;
                    }
                    dashboardsTab.populateTemplate({dashboards : dashboards,
                                              release : window.FLX_RELEASE_NUMBER});
                }
            }
        );
        App.fullHeight();
    }

   function populateTemplate(dashboardsTemplateData) {
        this.getTemplate("text!applications/calendar/tabs/dashboards/dashboards.html", "dashboards",
                         function() {
                             $("#addWidgetButton").unbind();
                             $("#manageDashboardsButton").unbind();
                             $(".dashboardName").unbind();

                             $("#addWidgetButton").click(addWidget);
                             $("#manageDashboardsButton").click(manageDashboards);
                             $(".dashboardName").click(function(evt) {
                                 var dashboardId = Number($(evt.target).parent().attr("id").substring("dashboard-".length));
                                 dashboardsTab.activeDashboard = dashboardId;
                                 $.ajax({
                                     url: "/api/dashboards/" + dashboardsTab.activeDashboard + "/active",
                                     type: "PUT",
                                     success: function(dashboards) {
                                         dashboardsTab.populateTemplate({dashboards : dashboards,
                                                                         release : window.FLX_RELEASE_NUMBER});
                                     }
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
        App.loadMustacheTemplate("applications/calendar/tabs/dashboards/dashboardsTabTemplates.html","widgetsGrid", function(template){
            var html = template.render({rows: rows});
            $("#dashboardsTab .tab-content").empty();
            $("#dashboardsTab .tab-content").append(html);
            $(".flx-remove-widget").click(function() {
                var widgetId = $(this).parent().parent().parent().attr("id");
                var i = widgetId.indexOf("-widget");
                var widgetName = widgetId.substring(0, i);
                removeWidget(widgetName);
            })
            loadWidgets(activeWidgets)
        });
   }

    function removeWidget(widgetName) {
        console.log("removing widget " + widgetName);
        var that = this;
        $.ajax({
               url: "/api/dashboards/" + dashboardsTab.activeDashboard + "/widgets/" + widgetName,
               type: "DELETE",
               success: function(dashboards) {
                   dashboardsTab.populateTemplate({dashboards : dashboards,
                                             release : window.FLX_RELEASE_NUMBER});
               }
           }
        );
    }

    function loadWidgets(activeWidgets) {
        for (var i=0; i<activeWidgets.length; i++) {
            var manifest = activeWidgets[i];
            loadWidget(manifest);
        };
    }

   function loadWidget(manifest) {
       require([manifest.WidgetRepositoryURL + "/"
                    + manifest.WidgetName + "/"
                    + manifest.WidgetName + ".js"],
               function(WidgetModule) {
                   WidgetModule.load(manifest, digest);
               });
   }

    function getActiveWidgets(dashboards) {
        var activeDashboard = getActiveDashboard(dashboards);
        return activeDashboard.widgets;
    }

    function getActiveDashboard(dashboards) {
        for (var i=0; i<dashboards.length; i++) {
            if (dashboards[i].id===dashboardsTab.activeDashboard) {
                return dashboards[i];
            }
        }
        return null;
    }

    function addWidget() {
        AddWidget.show(dashboardsTab);
    }

    function manageDashboards() {
        ManageDashboards.show(dashboardsTab);
    }

    function removeDashboard(dashboardId) {
        var confirmed = confirm ("Are you sure?");
        if (confirmed) {
            $.ajax({
                url: "/api/dashboards/" + dashboardId,
                type: "DELETE"
                   });
        }
    }

    function demoteDashboard(dashboardId) {
        console.log("demoting a dashboard: " + dashboardId);
    }

    function promoteDashboard(dashboardId) {
        console.log("promoting a dashboard: " + dashboardId);
    }

    var dashboardsTab = new Tab("dashboards", "Candide Kemmler", "icon-chart", true);
	dashboardsTab.render = render;
    dashboardsTab.connectorDisplayable = function(connector) { return false; }
    dashboardsTab.populateTemplate = populateTemplate;
    dashboardsTab.demoteDashboard = demoteDashboard;
   dashboardsTab.removeDashboard = removeDashboard;
   dashboardsTab.promoteDashboard = promoteDashboard;
	return dashboardsTab;
	
});
