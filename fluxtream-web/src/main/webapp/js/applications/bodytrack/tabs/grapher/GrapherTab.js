define(["core/Tab","core/grapher/Grapher","core/FlxState"], function(Tab,Grapher,FlxState) {
	
	var grapherTab = new Tab("bodytrack", "grapher", "Candide Kemmler", "icon-film", true);
    var currentView = null;
    var lastGuestId;

    var grapher = null;
    var grapherLoaded = false;
    var pointLoad = null;
    var currentPointLoad = null;
    var cursorPositionToSet = null;
    var channelsToAdd = [];

    grapherTab.render = function(params){
        if (App.buddyToAccess == null)
            return;
        if (lastGuestId!=null&&App.buddyToAccess.id!==lastGuestId) {
            $("._timeline_sources_list").empty();
            $("._timeline_channel").remove();
            grapher.reload();
        }
        lastGuestId = App.buddyToAccess.id;
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
            //NOTE: finding the right channels to load is done via a hack! TODO: implement this more ellegantly
            var connectorName = params.facetToShow.type.split("-")[0];
            var selectedConnectors = App.apps.calendar.digest.selectedConnectors;

            for (var i = 0, li = selectedConnectors.length; i < li; i++){
                if (selectedConnectors[i].connectorName == connectorName){
                    for (var j = 0, lj = selectedConnectors[i].channelNames.length; j < lj; j++){
                        channelsToAdd.push(selectedConnectors[i].channelNames[j]);
                    }
                    break;
                }
            }

            //have to divide by 1000 because times for the grapher are specified in seconds and times for the rest of fluxtream are specified in milliseconds
            cursorPositionToSet = (params.facetToShow.start + (params.facetToShow.end != null ? params.facetToShow.end : params.facetToShow.start)) / 2 / 1000;
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
        else if (grapherLoaded)
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
        if (navigator.userAgent.indexOf("Chrome")!=-1) {
            $("._timeline_channels").unbind().bind("DOMSubtreeModified", function() {
                $("._timeline_channelsArea").hide().show(0);
            }).on("click", function() {
                $("._timeline_channelsArea").hide().show(0);
            });
        }
        //trigger the resize handler for the grapher. This forces the grapher objects to resize to their proper sizes
        $(window).resize();
        grapherLoaded = true;
    }

    function onSourceLoad(){
        //add all queued channels
        for (var i = 0, li = channelsToAdd.length; i < li; i++){
            if (!grapher.hasChannel(channelsToAdd[i]))
                grapher.addChannel(channelsToAdd[i]);
        }

        if (tbounds != null)
            grapher.setRange(tbounds.start/1000,tbounds.end/1000);
        if (cursorPositionToSet != null){
            var range = grapher.getRange();
            range = range.max - range.min;
            grapher.setTimeCursorPosition(cursorPositionToSet);
            grapher.setRange(cursorPositionToSet - range / 2, cursorPositionToSet + range/2);
        }
        for (var i = 0, li = channelsToAdd.length; i < li; i++){
            grapher.doCursorClick(channelsToAdd[i]);
        }
        channelsToAdd = [];
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
