define(["core/Application", "core/FlxState"], function(Application, FlxState) {
	
	function initialize() {
		FlxState.router.route("app/bodytrack", "", function() { App.renderApp("bodytrack"); });
		console.log("initializing bodytrack app");
	}
	
	function render() {
		FlxState.router.navigate("app/bodytrack");
	}
	
	var BodyTrack = new Application("bodytrack", "Anne R. Wright", "icon-random");
	BodyTrack.initialize = initialize;
	BodyTrack.render = render;
	return BodyTrack;
	
});