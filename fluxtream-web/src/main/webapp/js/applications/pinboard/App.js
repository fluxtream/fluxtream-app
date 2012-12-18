define(["core/Application", "core/FlxState"], function(Application, FlxState) {

    var Pinboard = new Application("pinboard", "Candide Kemmler", "icon-bookmark");

    Pinboard.initialize = function () {
        _.bindAll(this);
        FlxState.router.route("app/pinboard", "", function() {
            Pinboard.render("");
        });
    };

    Pinboard.setup = function() {
        // TODO: implement this
    };

    Pinboard.parseState = function(state) {
        var obj = {};
        obj.state = state;
        return obj;
    };

    Pinboard.renderDefaultState = function() {
        Pinboard.navigateState("");
    };

    Pinboard.renderState = function(state) {
        // TODO: implement this
    };

    return Pinboard;
});