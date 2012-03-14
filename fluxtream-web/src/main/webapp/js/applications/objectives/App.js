define(["core/Application", "core/FlxState"], function(Application, FlxState) {

	var Objectives = new Application("objectives", "Candide Kemmler", "icon-screenshot");
	
	Objectives.initialize = function () {
		_.bindAll(this);
		FlxState.router.route("app/objectives", "", function() { Objectives.render(""); });
	}
	
	Objectives.renderState = function(state) {
		FlxState.router.navigate("app/objectives");
	}
	
	return Objectives;
	
});