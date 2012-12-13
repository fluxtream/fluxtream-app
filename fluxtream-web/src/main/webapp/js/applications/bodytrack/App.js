define(["core/Application", "core/FlxState", "core/TabInterface"], function(Application, FlxState, TabInterface) {

    var BodyTrack = new Application("bodytrack", "Candide Kemmler", "icon-bookmark");


    var grapher;
    var tabInterface;

    var tabPaths = ["applications/bodytrack/tabs/grapher/GrapherTab", "applications/bodytrack/tabs/views/ViewsTab"];
    var tabNames = ["grapher","views"]

    BodyTrack.initialize = function () {
        _.bindAll(this);
    };

    BodyTrack.setup = function() {
        tabInterface = new TabInterface(tabPaths);
        $("#bodytrackTabs").replaceWith(tabInterface.getNav());
        tabInterface.setTabVisibility(tabNames,true);
        tabInterface.getNav().addClickListener(function(tabName) {
            BodyTrack.navigateState(tabName);
        });
    };

    function isValidTabName(tabName) {
        return _.include(tabNames, tabName);
    }

    BodyTrack.parseState = function(state) {
        var splits = state.split("/");
        var obj = {};
        obj.stateParts = splits;
        obj.tabName = splits.shift();
        if (!isValidTabName(obj.tabName)) {
            return null;
        }
        obj.tabState = splits.join("/");
        return obj;
    };

    BodyTrack.renderDefaultState = function() {
        BodyTrack.navigateState(tabNames[0]);
    };

    BodyTrack.renderState = function(state) {
        tabInterface.setRenderParamsFunction(function(){
            return state;
        });
        document.title = "Fluxtream BodyTrack" + " (" + state.tabName + ")";
        tabInterface.setActiveTab(state.tabName);
    };

    return BodyTrack;
});