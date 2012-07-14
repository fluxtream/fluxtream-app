define(["App"], function(App) {

    var dashboardsTab;

    function show(dt){
        dashboardsTab = dt;
        update();
    }

    function update() {
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
        $(".dashboards-list").sortable({
            axis:"y",
            containment:".dashboards-list",
            stop: function(event,ui){
                dashboardsTab.reorderDone();
            }
        });
        $(".dashboard-list-item .remove").unbind("click");
        $(".dashboard-list-header button").unbind("click");


        $(".dashboard-list-item .remove").click(function(evt) {
            removeDashboard(evt);
        });
        $(".dashboard-list-header button").click(function(evt) {
            var dashboardName = $(".dashboard-list-header input").val();
            createDashboard(dashboardName);
        });
        $(".dashboard-list-item-name").editable(function(value, settings) {
            var dashboardId = $(this).parent().attr("id").substring("dashboard-".length);
            $.ajax({
                url: "/api/dashboards/" + dashboardId + "/name?name=" + value,
                type: "PUT",
                success: function(dashboards) {
                    dashboardsTab.updateDashboardTabs({ dashboards : dashboards });
                }
            });
            return value;
        },{
            style: "display: inline",
            width: 200
        });
    }

    function getDashboardId(evt) {
        return $(evt.target).parent().parent().parent().attr("id").substring("dashboard-".length)
    }

    function removeDashboard(evt) {
        var dashboardId = getDashboardId(evt);
        dashboardsTab.removeDashboard(dashboardId);
    }

    function createDashboard(dashboardName) {
        dashboardsTab.createDashboard(dashboardName);
    }

    function promoteDashboard(evt) {
        dashboardsTab.promoteDashboard(getDashboardId(evt));
    }

    function demoteDashboard(evt) {
        dashboardsTab.demoteDashboard(getDashboardId(evt));
    }

    var ManageDashboards = {};
    ManageDashboards.show = show;
    ManageDashboards.update = update;
    return ManageDashboards;
});