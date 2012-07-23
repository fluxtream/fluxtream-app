define(["core/Application", "core/FlxState"], function(Application, FlxState) {

    var BodyTrack = new Application("bodytrack", "Candide Kemmler", "icon-bookmark");

    BodyTrack.initialize = function () {
        FlxState.router.route(/^app\/bodytrack(\/?)(.*?)$/, "", function() {
            BodyTrack.render("");
        });
    };

    BodyTrack.saveState = function() {
    };

    BodyTrack.renderState = function(state) {
        if (state===null) state = "";
        else state = "/" + state;
        FlxState.router.navigate("app/bodytrack" + state);
        FlxState.saveState("pinboard", state);
    };

    BodyTrack.setup = function() {
    };

    return BodyTrack;
});