define(["core/Tab","core/Grapher/BTCore"], function(Tab,BTCore) {
	
	var viewsTab = new Tab("bodytrack", "views", "Candide Kemmler", "icon-facetime-video", true);

    viewsTab.render = function(params){
        this.getTemplate("text!applications/bodytrack/tabs/views/views.html", "views", function() {
            setup();
        });
    }

    function setup(){
        $("#viewsTabContents").empty();
        $("#viewsTabContents").append("loading views...");
        BTCore.VIEWS.getAvailableList(function (availableList){
            App.loadMustacheTemplate("applications/bodytrack/tabs/views/viewsTemplates.html","viewsList",function(template){
                $("#viewsTabContents").empty();
                $("#viewsTabContents").append(template.render({list:availableList.map(function(view){
                    return {id:view.id,
                            name:view.name,
                            last_used:App.formatDate(view.last_used,true)};
                })}));
                for (var i = 0; i < availableList.length; i++){
                    $("#delete-view-" + availableList[i].id).click({id:availableList[i].id},function(event){
                        var tr = $(event.target).parent().parent();
                        BTCore.VIEWS.delete(event.data.id,function(success){
                            if (success)
                                tr.remove();
                        });
                    });
                }
            })
        });
    }

	return viewsTab;
});
