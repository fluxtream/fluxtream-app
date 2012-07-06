define(function() {

    var connectors;

    function show(){
        $.ajax("/api/connectors/installed",{
            success: function(data, textStatus, jqXHR){
                dataLoaded(data,false);
            }
        });
    }

    function updateContents(){
        return;
        $.ajax("/api/connectors/installed",{
            success: function(data, textStatus, jqXHR){
                if (hidden)
                    return;
                dataLoaded(data,true);
            }
        })
    }


    function dataLoaded(data,update){
        connectors = data;
        App.loadMustacheTemplate("connectorMgmtTemplates.html","manageConnectors",function(template){
            var params = [];
            for (var i = 0; i < data.length; i++){
                params[i] = {};
                for (var member in data[i]){
                    switch (member){
                        default:
                            params[i][member] = data[i][member];
                            break;
                        case "latestData":
                        case "lastSync":
                            var formatted = App.formatDate(data[i][member],true);
                            if (formatted == "Present")
                                formatted = member == "lastSync" ? "Never" : "No Data";
                            params[i][member] = formatted;
                            break;
                    }
                }
            }
            var html = template.render({connectors:params});
            if (update){
                var scrollTop = $(".modal-body").scrollTop();
                $("#modal").html($(html).html());
                $(".modal-body").scrollTop(scrollTop);
            }
            else
                App.makeModal(html);
            bindDialog();
        });
    }

    var hidden;

    function bindDialog(){
        hidden = false;
         for (var i = 0; i < connectors.length; i++){
             bindConnector(connectors[i], i);
         }
        var syncAllBtn = $("#sync-all");
        syncAllBtn.click(function(){
            setAllToSyncing();
            event.preventDefault();
            $.ajax("/api/connectors/sync",{
                type:"POST"
            });
        });
        $.doTimeout("manageConnectorsUpdater", 10000, function(){
            updateContents();
            return true;
        });
        $("#modal").on("hide",function(){
            hidden = true;
            $.doTimeout("manageConnectorsUpdater");
        })
    }

    function bindConnector(connector, i){
        var deleteBtn = $("#remove-" + connector.connectorName);
        deleteBtn.click({index:i}, function(event){
            event.preventDefault();
            confirmDelete(event.data.index);
        });
        var syncNowBtn = $("#syncNow-" + connector.connectorName);
        syncNowBtn.click(function(event){
            event.preventDefault();
            setToSyncing(connector.connectorName)
            $.ajax("/api/connectors/" + connector.connectorName + "/sync",{
                type:"POST"
            });
        });

    }

    function setToSyncing(connectorName){
        var row = $("#connector-" + connectorName);
        if (row.hasClass("nowSynchro"))
            return;
        row.addClass("nowSynchro");
        var syncLED = $("#syncLED-" + connectorName);
        syncLED.removeClass("syncLED-yes");
        syncLED.removeClass("syncLED-no");
        syncLED.addClass("syncLED-waiting");
        syncLED.html("<span class=\"syncLED-waiting\">" +
                     "<img src=\"/css/devicesPictures/load.gif\" alt=\"load\">" +
                     "</span>");
        var lastSync = $("#lastSync-" + connectorName);
        lastSync.html("Now synchronizing");
        var syncNowBtn = $("#syncNow-" + connectorName);
        var disabledBtn = $("<span>" + syncNowBtn.html() + "</span>");
        syncNowBtn.replaceWith(disabledBtn);
    }

    function setAllToSyncing(){
        for (var i = 0; i < connectors.length; i++)
            setToSyncing(connectors[i].connectorName);
    }



    function confirmDelete(index){
        App.closeModal();
        $("#modal").on("hidden",function(){
            App.loadMustacheTemplate("connectorMgmtTemplates.html","deleteConnectorConfirm",function(template){
                App.makeModal(template.render(connectors[index]));
                var confirmDelete = $("#confirmDeleteBtn");

                confirmDelete.click(function(){
                    $.ajax("/api/connectors/" + connectors[index].connectorName,{
                        type:"DELETE",
                        success: App.closeModal,
                        error: App.closeModal
                    });

                });

                $("#modal").on("hidden",show);

            });


        });

    }


    var ManageConnectors = {};
    ManageConnectors.show = show;
    return ManageConnectors;
});