define(["core/Tab","core/grapher/Grapher"], function(Tab,Grapher) {
	
	var grapherTab = new Tab("bodytrack", "grapher", "Candide Kemmler", "icon-film", true);

    var grapher = null;

    grapherTab.render = function(params){
        tbounds = params.tbounds;
        this.getTemplate("text!applications/bodytrack/tabs/grapher/grapher.html", "grapher", function() {
            var sourceName = null;
            if (params.stateParts != null && params.stateParts.length == 4 && params.stateParts[2] == "source") {
                srcLoad = params.stateParts[3];
            }
            if (params.stateParts != null && params.stateParts.length == 2 && params.stateParts[0] == "view"){
                viewLoad = params.stateParts[1];
            }
            setup();
        });
    }

    var srcLoad = null;
    var viewLoad = null;
    var tbounds;

    function setup(){
        if (grapher == null)
            grapher = new Grapher($("#grapherContainer"),
                {showFullControls: true, showDeleteBtn: true, onLoadActions: [onGrapherLoad]});
        else
            onGrapherLoad();
    }

    function onGrapherLoad(){
        grapher.updateViews();
        if (srcLoad != null){
            var source = srcLoad;
            srcLoad = null;
            grapher.loadSource(source,onSourceLoad);
        }
        else{
            onSourceLoad();
        }

    }

    function onSourceLoad(){
        if (viewLoad != null){
            var view = viewLoad;
            viewLoad = null;
            grapher.loadView(view);
        }
        if (tbounds != null){
            grapher.setRange(tbounds.start/1000,tbounds.end/1000);
            tbounds = null;
        }

    }

	return grapherTab;
});
