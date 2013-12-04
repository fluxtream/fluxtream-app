define(["core/Tab","core/grapher/Grapher","core/FlxState"], function(Tab,Grapher,FlxState) {
	
	var grapherTab = new Tab("bodytrack", "grapher", "Candide Kemmler", "icon-film", true);
    var currentView = null;

    var grapher = null;
    var pointLoad = null;
    var currentPointLoad = null;
    var cursorPositionToSet = null;
    var channelToAdd = null;

    grapherTab.render = function(params){
        if (params.rebuildURL){
            params.rebuildURL = false;
            var append = "";
            if (currentView != null){
                append = "/view/" + currentView;
            }
            App.renderApp("bodytrack","grapher" + append,params);
            return;
        }
        if (params.facetToShow != null){
            cursorPositionToSet = (params.facetToShow.start + (params.facetToShow.end != null ? params.facetToShow.end : params.facetToShow.start)) / 2;
        }
        tbounds = params.tbounds;
        this.getTemplate("text!applications/bodytrack/tabs/grapher/grapher.html", "grapher", function() {
            var sourceName = null;
            if (params.stateParts != null && params.stateParts.length == 2 && params.stateParts[0] == "source") {
                srcLoad = params.stateParts[1];
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
        if (channelToAdd != null)
            if (!grapher.hasChannel(channelToAdd))
                grapher.addChannel(channelToAdd);
        if (tbounds != null)
            grapher.setRange(tbounds.start/1000,tbounds.end/1000);
        if (cursorPositionToSet != null){
            grapher.setTimeCursorPosition(cursorPositionToSet);
        }
        if (channelToAdd != null)
            grapher.doCursorClick(channelToAdd);
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
        else{
            currentView = null;
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
