define(["core/Tab","core/Grapher/grapher"], function(Tab,Grapher) {
	
	var grapherTab = new Tab("bodytrack", "grapher", "Candide Kemmler", "icon-film", true);

    var grapher = null;

    grapherTab.render = function(params){
        this.getTemplate("text!applications/bodytrack/tabs/grapher/grapher.html", "grapher", function() {
            setup();
        });
    }

    function setup(){
        if (grapher == null)
            grapher = new Grapher($("#grapherContainer"),{showFullControls:true,showDeleteBtn:true});
    }

	return grapherTab;
});
