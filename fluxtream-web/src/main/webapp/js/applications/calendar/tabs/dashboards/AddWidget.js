define(function() {

    var activeDashboardId;

    function show(adid){
        activeDashboardId = adid;
        $.ajax("/api/widgets",{
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
        App.loadMustacheTemplate("applications/calendar/tabs/dashboards/addWidgetTemplate.html","mainDialog",function(template){
            var html = template.render(data);
            App.makeModal(html);
            bindDialog();
        });
    }

    function bindDialog(){
    }

    var AddWidget = {};
    AddWidget.show = show;
    return AddWidget;
});