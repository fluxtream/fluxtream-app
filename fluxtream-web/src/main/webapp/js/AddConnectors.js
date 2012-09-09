define(function() {

    function show(){
        $.ajax("/api/connectors/uninstalled",{
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
            App.makeModal(html);
            bindDialog();
        });
    }

    function bindDialog(){
    }

    var AddConnectors = {};
    AddConnectors.show = show;
    return AddConnectors;

});