define(function() {

    function show(){
        $.ajax("/api/dashboards",{
            success: function(data, textStatus, jqXHR){
                dataLoaded(data,false);
            }
        });
    }

    function dataLoaded(data,update){
        App.loadMustacheTemplate("applications/calendar/tabs/dashboards/manageDashboardsTemplate.html","mainDialog",function(template){
            var html = template.render(data);
            App.makeModal(html);
            bindDialog();
        });
    }

    function bindDialog(){
        //for (var i = 0; i < connectors.length; i++){
        //    bindConnector(connectors[i]);
        //}
        //var syncAllBtn = $("#sync-all");
        //syncAllBtn.click(function(){
        //    setAllToSyncing();
        //    event.preventDefault();
        //    $.ajax("/api/guest/" + App.getUsername() + "/connector/all/sync",{
        //        type:"POST"
        //    });
        //});
        //$.doTimeout("manageConnectorsUpdater", 10000, function(){
        //    updateContents();
        //    return true;
        //});
        //$("#modal").on("hide",function(){
        //    $.doTimeout("manageConnectorsUpdater");
        //})
    }

    var ManageDashboards = {};
    ManageDashboards.show = show;
    return ManageDashboards;
});