define(function() {

    var dashboardsTab;

    function show(dt){
        dashboardsTab = dt;
        $.ajax("/api/v1/dashboards/" + dashboardsTab.activeDashboard + "/availableWidgets",{
            success: function(data, textStatus, jqXHR){
                var rows = [];
                var row = {widgets:[]};
                for (var i=0; i<data.length; i++) {
                    if(i%3==0) {
                        row = {widgets:[]};
                        rows.push(row);
                    }
                    data[i].arrayIndex = i;
                    row.widgets.push(data[i]);
                }
                dataLoaded(data,{rows: rows});
            }
        });
    }

    function dataLoaded(originalData,data){
        App.loadMustacheTemplate("applications/calendar/tabs/dashboards/dashboardsTabTemplates.html","addWidgetDialog",function(template){
            var html = template.render(data);
            App.makeModal(html);
            bindDialog(originalData);
        });
    }

    function bindDialog(data) {
        $("#availableWidgets a").click(function() {
            var widgetName = $(this).attr("name");
            var widgetInfo = data[$(this).attr("arrayIndex")];
            if (widgetInfo.RequiredConnectors != null && widgetInfo.RequiredConnectors.length != 0 && dashboardsTab.shouldSandboxWidget({manifest: widgetInfo})) {
                App.loadMustacheTemplate("applications/calendar/tabs/dashboards/dashboardsTabTemplates.html","chooseConnectors",function(template){
                    //sometimes there's trailing/leading spaces
                    for (var i = 0; i < widgetInfo.RequiredConnectors.length; i++){
                        widgetInfo.RequiredConnectors[i] = widgetInfo.RequiredConnectors[i].trim();
                    }
                    var html = template.render(widgetInfo);
                    var modal = App.makeModal(html);
                    modal.find(".confirm").click(function(){
                        var allowedConnectors = [];
                        modal.find("input").each(function(){
                            if (this.checked) {
                                allowedConnectors.push(this.getAttribute("name"));

                            }
                        });
                        addWidget(widgetName,allowedConnectors,widgetInfo.fullAccess);
                        modal.modal("hide");
                    });
                });
            }
            else{
                addWidget(widgetName,[],!dashboardsTab.shouldSandboxWidget({manifest: widgetInfo}));
            }
        });
    }

    function addWidget(widgetName,allowedConnectors,fullAccess) {
        $.ajax({
            url: "/api/v1/dashboards/" + dashboardsTab.activeDashboard + "/widgets",
            type: "POST",
            data: { widget : widgetName, allowedConnectors: JSON.stringify(allowedConnectors), fullAccess: fullAccess },
            success: function(dashboards) {
                dashboardsTab.populateTemplate({dashboards : dashboards,
                    release : window.FLX_RELEASE_NUMBER});
                App.closeModal();
            }
        });
    }
    var AddWidget = {};
    AddWidget.show = show;
    return AddWidget;
});