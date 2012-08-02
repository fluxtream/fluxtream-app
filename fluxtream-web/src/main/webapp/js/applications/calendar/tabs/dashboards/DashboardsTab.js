define(["core/Tab",
        "applications/calendar/App", "applications/calendar/tabs/dashboards/ManageDashboards",
        "applications/calendar/tabs/dashboards/AddWidget"],
       function(Tab, Calendar, ManageDashboards, AddWidget) {
	
	var digest, dashboardData;

    var setTabParam;

	function render(params) {
        setTabParam = params.setTabParam;
        if (params.tabParam != null)
            dashboardsTab.activeDashboard = parseInt(params.tabParam);
        _.bindAll(this);
		digest = params.digest;
        $.ajax({
                url: "/api/dashboards",
                success: function(dashboards) {
                    if (dashboardsTab.activeDashboard == null){
                        for (var i=0; i<dashboards.length; i++) {
                            if (dashboards[i].active)
                                dashboardsTab.activeDashboard = dashboards[i].id;
                        }
                    }
                    dashboardsTab.populateTemplate({dashboards : dashboards});
                }
            }
        );
        App.fullHeight();
    }

   function populateTemplate(dashboardsTemplateData) {
       setTabParam(dashboardsTab.activeDashboard);
        this.getTemplate("text!applications/calendar/tabs/dashboards/dashboards.html", "dashboards",
                         function() {
                             makeDashboardTabs(dashboardsTemplateData);
                         },
                         dashboardsTemplateData
        );
   };

   function makeDashboardTabs(dashboardsTemplateData) {
        App.loadMustacheTemplate("applications/calendar/tabs/dashboards/dashboardsTabTemplates.html","dashboardTabs", function(template){
            var html = template.render(dashboardsTemplateData);
            $("#dashboardTabs").replaceWith(html);
            $("#dashboard-" + dashboardsTab.activeDashboard).children().tab("show");
            $("#addWidgetButton").unbind();
            $("#manageDashboardsButton").unbind();
            $(".dashboardName").unbind();
            $("#addWidgetButton").click(addWidget);
            $("#manageDashboardsButton").click(manageDashboards);
            $(".dashboardName").click(function(evt) {
                var dashboardId = Number($(evt.target).parent().attr("id").substring("dashboard-".length));
                setActiveDashboard(dashboardId);
            });
            fetchWidgets(getActiveWidgetInfos(dashboardsTemplateData.dashboards));
        });
   }

   function setActiveDashboard(dashboardId) {
       dashboardsTab.activeDashboard = dashboardId;
       $.ajax({
                  url: "/api/dashboards/" + dashboardId + "/active",
                  type: "PUT",
                  success: function(dashboards) {
                      dashboardsTab.populateTemplate({dashboards : dashboards});
                  }
              });
   }

   function fetchWidgets(activeWidgetInfos) {
        var rows = [];
        var row = {widgets:[]};
        for (var i=0; i<activeWidgetInfos.length; i++) {
            if(i%3==0) {
                row = {widgets:[]};
                rows.push(row);
            }
            row.widgets.push(activeWidgetInfos[i].manifest);
        }
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
            loadWidgets(activeWidgetInfos)
        });
   }

    function removeWidget(widgetName) {
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

    function loadWidgets(activeWidgetInfos) {
        for (var i=0; i<activeWidgetInfos.length; i++) {
            var widgetInfo = activeWidgetInfos[i];
            loadWidget(widgetInfo);
        };
    }

   function loadWidget(widgetInfo) {
       var that = this;
       require([widgetInfo.manifest.WidgetRepositoryURL + "/"
                    + widgetInfo.manifest.WidgetName + "/"
                    + widgetInfo.manifest.WidgetName + ".js"],
               function(WidgetModule) {
                   WidgetModule.load(widgetInfo, digest, dashboardsTab.activeDashboard);
               });
   }

    function getActiveWidgetInfos(dashboards) {
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
                type: "DELETE",
                success: function(dashboards) {
                    setActiveDashboard(dashboards[0].id);
                    ManageDashboards.update();
                }
            });
        }
    }

    function createDashboard(dashboardName) {
        $.ajax({
            url: "/api/dashboards",
            type: "post",
            data: { dashboardName : dashboardName },
            success: function(dashboards) {
                makeDashboardTabs({ dashboards : dashboards })
                ManageDashboards.update();
            }
        });
    }

    function demoteDashboard(dashboardId) {
        ManageDashboards.update();
    }

    function promoteDashboard(dashboardId) {
        ManageDashboards.update();
    }

    function reorderDone(){
        var ordering = "";
        var children = $(".dashboards-list").children();
        for (var i = 0; i < children.length; i++){
            if (i != 0)
                ordering += ",";
            var tabId = $(children[i]).attr('dashboardid');
            ordering += tabId;
            var tab = $("#dashboard-" + tabId);
            tab.remove();
            $("#dashboardTabs").append(tab);
        }
        $.ajax({
            url: "/api/dashboards/reorder",
            type:"post",
            data: {dashboardIds:ordering}
        })
    }


    var dashboardsTab = new Tab("calendar", "dashboards", "Candide Kemmler", "icon-dashboard", true);
    dashboardsTab.activeDashboard = null;
	dashboardsTab.render = render;
    dashboardsTab.connectorDisplayable = function(connector) { return false; }
    dashboardsTab.populateTemplate = populateTemplate;
    dashboardsTab.createDashboard = createDashboard;
    dashboardsTab.removeDashboard = removeDashboard;
    dashboardsTab.demoteDashboard = demoteDashboard;
    dashboardsTab.promoteDashboard = promoteDashboard;
    dashboardsTab.updateDashboardTabs = makeDashboardTabs;
    dashboardsTab.reorderDone = reorderDone;
	return dashboardsTab;
	
});
