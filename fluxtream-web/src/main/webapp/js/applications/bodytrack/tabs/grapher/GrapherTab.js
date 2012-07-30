define(["core/Tab","core/grapher/Grapher"], function(Tab,Grapher) {
	
	var grapherTab = new Tab("bodytrack", "grapher", "Candide Kemmler", "icon-film", true);

    var grapher = null;

    grapherTab.render = function(params){
        this.getTemplate("text!applications/bodytrack/tabs/grapher/grapher.html", "grapher", function() {
            var sourceName = null;
            if (params.stateParts != null && params.stateParts.length == 4 && params.stateParts[2] == "source")
                sourceName = params.stateParts[3];
            setup(sourceName);
        });
    }

    var srcLoad = null;

    $(window).resize(function(){
        if (srcLoad == null)
            return;
        var source = srcLoad;
        srcLoad = null;
        grapher.loadSource(source);

    });

    function setup(sourceToLoad){
        srcLoad = sourceToLoad;
        if (grapher == null)
            grapher = new Grapher($("#grapherContainer"),{showFullControls:true,showDeleteBtn:true});
        else{
            grapher.updateViews();
            $(window).resize();
        }
    }

	return grapherTab;
});
