define(["core/Application"], function(Application) {
	
	var BodyTrack = {};
	_.extend(BodyTrack, Application);
	BodyTrack.name = "BodyTrack";
	BodyTrack.gotoDate = gotoDate;
	BodyTrack.initialize = initialize;
	return BodyTrack;
	
})