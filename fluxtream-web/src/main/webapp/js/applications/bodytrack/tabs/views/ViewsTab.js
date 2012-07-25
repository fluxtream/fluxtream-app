define(["core/Tab"], function(Tab) {
	
	var viewsTab = new Tab("bodytrack", "views", "Candide Kemmler", "icon-facetime-video", true);

    viewsTab.render = function(params){
        this.getTemplate("text!applications/bodytrack/tabs/views/views.html", "views", function() {
            setup();
        });
    }

    function setup(){

    }

	return viewsTab;
});
