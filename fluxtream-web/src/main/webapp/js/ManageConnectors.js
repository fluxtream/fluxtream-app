define(function() {

    var connectors;

    function show(){
        $.ajax("/api/guest/" + App.getUsername() + "/connector/all",{
            success: function(data, textStatus, jqXHR){
                dataLoaded(data,false);
            }
        });
    }

    function updateContents(){
        $.ajax("/api/guest/" + App.getUsername() + "/connector/all",{
            success: function(data, textStatus, jqXHR){
                dataLoaded(data,true);
            }
        })
    }


    function dataLoaded(data,update){
        connectors = data;
        App.loadMustacheTemplate("manageConnectorsTemplate.html","mainDialog",function(template){
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

    function bindDialog(){
         for (var i = 0; i < connectors.length; i++){
             var deleteBtn = $("#remove-" + connectors[i].connectorName);
             deleteBtn.click({index:i}, function(event){
                 event.preventDefault();
                 confirmDelete(event.data.index);
             });
             var syncNowBtn = $("#syncNow-" + connectors[i].connectorName);
             syncNowBtn.click({index:i}, function(event){
                 event.preventDefault();
                 $.ajax("/api/guest/" + App.getUsername() + "/connector/" + connectors[event.data.index].connectorName + "/sync",{
                     type:"POST",
                     success:updateContents
                 });
             });
         }
        var syncAllBtn = $("#sync-all");
        syncAllBtn.click(function(){
            event.preventDefault();
            $.ajax("/api/guest/" + App.getUsername() + "/connector/all/sync",{
                type:"POST",
                success:updateContents
            });
        });
    }

    function confirmDelete(index){
        App.closeModal();
        $("#modal").on("hidden",function(){
            App.loadMustacheTemplate("manageConnectorsTemplate.html","deleteConfirm",function(template){
                App.makeModal(template.render(connectors[index]));
                var confirmDelete = $("#confirmDeleteBtn");

                confirmDelete.click(function(){
                    $.ajax("/api/guest/" + App.getUsername() + "/connector/" + connectors[index].connectorName,{
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