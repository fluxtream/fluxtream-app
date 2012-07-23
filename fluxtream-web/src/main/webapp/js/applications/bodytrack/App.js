define(["core/Application", "core/FlxState"], function(Application, FlxState) {

    var Pinboard = new Application("bodytrack", "Candide Kemmler", "icon-bookmark");

    Pinboard.initialize = function () {
        FlxState.router.route(/^app\/bodytrack(\/?)(.*?)$/, "", function() {
            Pinboard.render("");
        });
    };

    Pinboard.saveState = function() {
    };

    Pinboard.renderState = function(state) {
        if (state===null) state = "";
        else state = "/" + state;
        FlxState.router.navigate("app/bodytrack" + state);
        FlxState.saveState("pinboard", state);
    };

    Pinboard.setup = function() {
    };

    return Pinboard;
});