define(function() {

    function show(){
        $.ajax("/api/v1/connectors/uninstalled",{
            success: function(data, textStatus, jqXHR){
                var rows = [];
                var row = {connectors:[]};
                for (var i=0; i<data.length; i++) {
                    if(i%3==0) {
                        row = {connectors:[]};
                        rows.push(row);
                    }
                    row.connectors.push(data[i]);
                }
                dataLoaded({rows: rows});
            }
        });
    }

    function dataLoaded(data){
        App.loadMustacheTemplate("connectorMgmtTemplates.html","addConnectors",function(template){
            var html = template.render(data);
            $("#connectorsDropdownToggle").popover("destroy");
            App.makeModal(html);
            bindDialog();
        });
    }

    function bindDialog(){
        $("#modal").on("hide",function(){
            App.activeApp.renderState(App.state.getState(App.activeApp.name),true);//force refresh of the current app state
        });
    }

    var AddConnectors = {};
    AddConnectors.show = show;
    return AddConnectors;

});