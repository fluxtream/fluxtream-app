define(["core/Application", "core/FlxState", "core/Grapher/grapher"], function(Application, FlxState, Grapher) {

    var BodyTrack = new Application("bodytrack", "Candide Kemmler", "icon-bookmark");


    var grapher;

    BodyTrack.initialize = function () {
        FlxState.router.route(/^app\/bodytrack(\/?)(.*?)$/, "", function() {
            BodyTrack.render("");
        });
    };

    BodyTrack.saveState = function() {
    };

    BodyTrack.renderState = function(state) {
        if (state===null) state = "";
        else if (state.indexOf("/") != 0) state = "/" + state;
        FlxState.router.navigate("app/bodytrack" + state);
        FlxState.saveState("bodytrack", state);
        $(window).resize();
    };

    BodyTrack.setup = function() {
        grapher = new Grapher($("#bodyTrackAppContainer"),{showDeleteBtn:true,showFullControls:true});
    };

    return BodyTrack;
});