define(["core/Application", "core/FlxState", "core/TabInterface"], function(Application, FlxState, TabInterface) {

    var BodyTrack = new Application("bodytrack", "Candide Kemmler", "icon-bookmark");


    var grapher;
    var tabInterface;

    var tabPaths = ["applications/bodytrack/tabs/grapher/GrapherTab", "applications/bodytrack/tabs/views/ViewsTab"];
    var tabNames = ["grapher","views"]

    BodyTrack.initialize = function () {
        var bt = this;
        FlxState.router.route(/^app\/bodytrack(\/?)(.*?)$/, "", function() {
            var parse_url = /^(?:([A-Za-z]+):)?(\/{0,3})([0-9.\-A-Za-z]+)(?::(\d+))?(?:\/([^?#]*))?(?:\?([^#]*))?(?:#(.*))?$/;
            var result = parse_url.exec(window.location.href);
            var names = [ 'url', 'scheme', 'slash', 'host', 'port', 'path' ];
            var i;
            var parts = {};
            for (i = 0; i < names.length; i += 1)
                parts[names[i]] = result[i];
            var pathElements = parts.path.split("/");
            if (pathElements.length<3){
                App.invalidPath();
                return;
            }
            var found = false;
            for (var i = 0; i < tabNames.length && !found; i++){
                found = pathElements[2] == tabNames[i];
            }
            if (!found){
                App.invalidPath();
                return;
            }

            var state = "";
            for (var i = 2; i < pathElements.length; i++){
                state += "/" + pathElements[i];
            }

            bt.render(state);
        });
    };

    BodyTrack.saveState = function() {
    };

    BodyTrack.renderState = function(state) {
        if (state===null) state = "";
        else if (state.indexOf("/") != 0) state = "/" + state;

        var splits = state.split("/");

        var tabName = splits[1];
        if (tabName == "" || tabName == null){
            this.renderState(tabNames[0]);
            return;
        }
        tabInterface.setRenderParamsFunction(function(){
            return {stateParts:splits};
        });
        tabInterface.setActiveTab(tabName);

        document.title = "Fluxtream BodyTrack" + " (" + tabName + ")";
        FlxState.router.navigate("app/bodytrack" + state);
        FlxState.saveState("bodytrack", state);
    };

    BodyTrack.setup = function() {
        tabInterface = new TabInterface(tabPaths);
        $("#bodytrackTabs").replaceWith(tabInterface.getNav());
        tabInterface.setTabVisibility(tabNames,true);
        var bt = this;
        tabInterface.getNav().addClickListener(function(tabName){
            bt.renderState(tabName);
        });
    };

    return BodyTrack;
});