define(["core/Application"], function(Application) {

	var router = new Backbone.Router();
	
	function initialize() {
		router.navigate("/app/bodyTrack");
		if (!window.historyStarted) {
			Backbone.history.start({pushState: true});
			window.historyStarted = true;
		}
	}
	
	var BodyTrack = {};
	_.extend(BodyTrack, Application);
	BodyTrack.name = "BodyTrack";
	BodyTrack.initialize = initialize;
	return BodyTrack;
	
})