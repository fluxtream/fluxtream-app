define(["core/Application", "core/FlxState"], function(Application, FlxState) {

    var Pinboard = new Application("pinboard", "Candide Kemmler", "icon-bookmark");

    Pinboard.initialize = function () {
        FlxState.router.route("app/pinboard", "", function() {
            Pinboard.render("");
        });
    };

    Pinboard.saveState = function() {
    };

    Pinboard.renderState = function(state) {
        if (state===null) state = "";
        else state = "/" + state;
        FlxState.router.navigate("app/pinboard" + state);
        FlxState.saveState("pinboard", state);
    };

    Pinboard.setup = function() {
    };

    return Pinboard;
});