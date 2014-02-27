define(["core/Application", "core/FlxState", "core/TabInterface"], function(Application, FlxState, TabInterface) {

    var BodyTrack = new Application("bodytrack", "Candide Kemmler", "icon-bookmark", "BodyTrack App");


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
        var params = this.getParams();
        for (var member in params)
            obj[member] = params[member];
        obj.stateParts = splits;
        obj.tabName = splits.shift();
        if (!isValidTabName(obj.tabName)) {
            return null;
        }
        obj.tabState = splits.join("/");
        return obj;
    };

    function getStateString(state){
        if(state!=null && state.tabName!=null) {
            return state.tabName + "/" + state.stateParts.join("/");
        }
        else {
            // I'm not sure why state is sometimes null, but return "grapher" in that case
            // so we go to the default state for the BodyTrack app
            return "grapher";
        }
    }

    BodyTrack.renderDefaultState = function() {
        BodyTrack.navigateState(tabNames[0]);
    };

    BodyTrack.renderState = function(state) {
        if (typeof state == "string") {
            state = BodyTrack.parseState(state);
        }
        if(state == null) {
            // Default to "grapher" if state is null
            state = BodyTrack.parseState("grapher");
        }
        tabInterface.setRenderParamsFunction(function(){
            return state;
        });
        document.title = "Fluxtream BodyTrack App";
        App.state.saveState("bodytrack",getStateString(state));
        tabInterface.setActiveTab(state.tabName);
    };

    return BodyTrack;
});