define(["core/Tab","core/grapher/Grapher","core/FlxState"], function(Tab,Grapher,FlxState) {
	
	var grapherTab = new Tab("bodytrack", "grapher", "Candide Kemmler", "icon-film", true);
    var currentView = null;

    var grapher = null;
    var pointLoad = null;
    var currentPointLoad = null;

    grapherTab.render = function(params){
        tbounds = params.tbounds;
        this.getTemplate("text!applications/bodytrack/tabs/grapher/grapher.html", "grapher", function() {
            var sourceName = null;
            if (params.stateParts != null && params.stateParts.length == 2 && params.stateParts[0] == "source") {
                srcLoad = params.stateParts[1];
            }
            if (params.stateParts != null && params.stateParts.length == 2 && params.stateParts[0] == "view"){
                viewLoad = params.stateParts[1];
            }
            if (params.stateParts != null && params.stateParts.length == 4 && params.stateParts[0] == "point"){
                pointLoad = {
                    device: params.stateParts[1],
                    channel: params.stateParts[2],
                    time: parseInt(params.stateParts[3]) / 1000
                }

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
                {showFullControls: true, showDeleteBtn: true, onLoadActions: [onGrapherLoad], loadViewOverride: loadView});
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
        //trigger the resize handler for the grapher. This forces the grapher objects to resize to their proper sizes
        $(window).resize();
    }

    function onSourceLoad(){
        if (pointLoad != null){
            if (currentPointLoad !== pointLoad){
                currentPointLoad = pointLoad;
                currentView = null;
                grapher.newView(pointLoad.time - 60 * 60 * 12, pointLoad.time + 60 * 60 * 12);
                grapher.addChannel(pointLoad.device + "." + pointLoad.channel);
                grapher.setTimeCursorPosition(pointLoad.time);
                pointLoad = null;
                tbounds = null;
            }
        }
        onPointLoad();
    }

    function onPointLoad(){
        if (viewLoad != null){
            if (currentView != viewLoad){
                var view = viewLoad;

                currentView = view;
                grapher.loadView(view);
            }
            viewLoad = null;
        }
        if (tbounds != null){
            grapher.setRange(tbounds.start/1000,tbounds.end/1000);
            tbounds = null;
        }

        if (grapher.getTimeCursorPosition() == null)
            grapher.setTimeCursorPosition(grapher.getCenter());

    }

    function loadView(viewId){
        if (currentView == viewId)
            return false;
        if (viewId == null){
            App.renderApp("bodytrack","grapher");
            return false;
        }
        else{
            App.renderApp("bodytrack","grapher/view/" + viewId);
            return true;
        }
    }

	return grapherTab;
});
