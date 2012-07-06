define(["App"], function(App) {

    function show(){
        $.ajax("/api/dashboards",{
            success: function(data, textStatus, jqXHR){
                dataLoaded(data);
            }
        });
    }

    function dataLoaded(data){
        App.loadMustacheTemplate("applications/calendar/tabs/dashboards/dashboardsTabTemplates.html","manageDashboardsDropdown",function(template){
            var html = template.render( { dashboards : data } );
            App.addHideTooltipListener(function() {
                $("#manageDashboardsDropdown").hide();
            });
            $("#manageDashboardsDropdown").empty();
            $("#manageDashboardsDropdown").show();
            $("#manageDashboardsDropdown").append(html);
            bindPanel();
        });
    }

    function bindPanel() {
        $(".dashboard-list-item .up").click(function(evt) {
            console.log("up!");
        });
        $(".dashboard-list-item .down").click(function(evt) {
            console.log("down!");
        });
    }

    var ManageDashboards = {};
    ManageDashboards.show = show;
    return ManageDashboards;
});