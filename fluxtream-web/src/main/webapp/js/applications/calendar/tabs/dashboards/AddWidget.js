define(function() {

    var dashboardsTab;

    function show(dt){
        dashboardsTab = dt;
        $.ajax("/api/dashboards/" + dashboardsTab.activeDashboard + "/availableWidgets",{
            success: function(data, textStatus, jqXHR){
                var rows = [];
                var row = {widgets:[]};
                for (var i=0; i<data.length; i++) {
                    if(i%3==0) {
                        row = {widgets:[]};
                        rows.push(row);
                    }
                    row.widgets.push(data[i]);
                }
                dataLoaded({rows: rows});
            }
        });
    }

    function dataLoaded(data){
        App.loadMustacheTemplate("applications/calendar/tabs/dashboards/dashboardsTabTemplates.html","addWidgetDialog",function(template){
            var html = template.render(data);
            App.makeModal(html);
            bindDialog();
        });
    }

    function bindDialog() {
        $("#availableWidgets a").click(function() {
            var widgetName = $(this).attr("name");
            $.ajax({
                url: "/api/dashboards/" + dashboardsTab.activeDashboard + "/widgets",
                type: "POST",
                data: { widget : widgetName },
                success: function(dashboards) {
                    dashboardsTab.populateTemplate({dashboards : dashboards,
                                                    release : window.FLX_RELEASE_NUMBER});
                    App.closeModal();
                }
           })
        });
    }

    var AddWidget = {};
    AddWidget.show = show;
    return AddWidget;
});