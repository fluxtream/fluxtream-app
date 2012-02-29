define(["core/Application", "core/FlxState"], function(Application, FlxState) {
	
	function initialize() {
		FlxState.router.navigate("/app/bodytrack");
	}
	
	var BodyTrack = {};
	_.extend(BodyTrack, Application);
	BodyTrack.name = "BodyTrack";
	BodyTrack.initialize = initialize;
	return BodyTrack;
	
});