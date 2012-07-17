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
                if (!data[i].manageable)
                    continue;
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
            $.ajax("/api/sync/all",{
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
            $.ajax("/api/sync/" + connector.connectorName,{
                type:"POST"
            });
        });
        var viewDataBtn = $("#viewUpdates-" + connector.connectorName);
        viewDataBtn.click(function(event){
            event.preventDefault();
            App.loadMustacheTemplate("connectorMgmtTemplates.html","viewUpdates",function(template){
                viewUpdates(template, connector);
            });
        });
    }

    function viewUpdates(template, connector) {
        var connectorName = connector.connectorName.charAt(0).toUpperCase() + connector.connectorName.slice(1);
        $.ajax({
            url:"/api/updates/" + connector.connectorName + "?page=0&pageSize=50",
            success: function(updates) {
                for (var i=0; i<updates.length; i++)
                    updates[i].time = App.formatDate(updates[i].ts, true);
                var html = template.render({connectorName : connectorName,
                                            updates : updates});

                App.makeModal(html);
            }
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
                     "<img src=\"/images/syncing.gif\" alt=\"load\">" +
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
        App.loadMustacheTemplate("connectorMgmtTemplates.html","deleteConnectorConfirm",function(template){
            var html = template.render(connectors[index]);

            $("body").append(html);
            $("#deleteConnectorConfirm").modal();

            $("#deleteConnectorConfirm").css("zIndex","1052");

            $("#deleteConnectorConfirm").on("hidden",function(){
                $("#deleteConnectorConfirm").remove();
            });

            var backdrops = $(".modal-backdrop");
            $(backdrops[backdrops.length - 1]).css("zIndex","1051");

            var confirmDelete = $("#confirmRemoveConnectorBtn");
            var cancelDelete = $("#cancelRemoveConnectorBtn");

            cancelDelete.click(function() {
                $("#deleteConnectorConfirm").modal("hide");
            });

            confirmDelete.click(function(){
                $.ajax({
                    url: "/api/connectors/" + connectors[index].connectorName,
                    type:"DELETE",
                    success: function() {
                        updateContents();
                        $("#deleteConnectorConfirm").modal("hide");
                    },
                    error: function() {
                        $("#deleteConnectorConfirm").modal("hide");
                    }
                });
            });
        });

    }

    var ManageConnectors = {};
    ManageConnectors.show = show;
    return ManageConnectors;
});