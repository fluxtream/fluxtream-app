define(["core/Application", "core/FlxState"], function(Application, FlxState) {

	var BodyTrack = new Application("bodytrack", "Anne R. Wright", "icon-random");
	
	BodyTrack.initialize = function () {
		_.bindAll(this);
		console.log("initializing bodytrack app");
		FlxState.router.route("app/bodytrack", "", function() { BodyTrack.render(""); });
	}
	
	BodyTrack.renderState = function(state) {
		FlxState.router.navigate("app/bodytrack");
	}
	
	return BodyTrack;
	
});