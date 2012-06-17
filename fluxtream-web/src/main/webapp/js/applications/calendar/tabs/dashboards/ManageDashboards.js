define(function() {

    function show(){
        $.ajax("/api/dashboards",{
            success: function(data, textStatus, jqXHR){
                dataLoaded(data);
            }
        });
    }

    function dataLoaded(data){
        App.loadMustacheTemplate("applications/calendar/tabs/dashboards/manageDashboardsTemplate.html","mainDialog",function(template){
            var html = template.render(data);
            App.makeModal(html);
            bindDialog();
        });
    }

    function bindDialog(){
    }

    var ManageDashboards = {};
    ManageDashboards.show = show;
    return ManageDashboards;
});