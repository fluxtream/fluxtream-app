define(["core/Application"], function(Application) {

	function initialize() {
	}
	
	var BodyTrack = {};
	_.extend(BodyTrack, Application);
	BodyTrack.name = "BodyTrack";
	BodyTrack.initialize = initialize;
	return BodyTrack;
	
})