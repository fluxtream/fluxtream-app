define(["core/Application", "core/FlxState"], function(Application, FlxState) {

    var Pinboard = new Application("pinboard", "Candide Kemmler", "icon-pushpin");

    Pinboard.initialize = function () {
        FlxState.router.route("app/pinboard", "", function() {
            Pinboard.render("");
        });
    };

    Pinboard.saveState = function() {
    };

    Pinboard.renderState = function(state) {
    };

    Pinboard.setup = function() {
    };

    return Pinboard;
});