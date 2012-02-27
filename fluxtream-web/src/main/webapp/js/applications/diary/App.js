define(["core/Application"], function(Application) {

	var router = new Backbone.Router();
	
	function initialize() {
		router.navigate("/diary");
		if (!window.historyStarted) {
			Backbone.history.start({pushState: true});
			window.historyStarted = true;
		}
	}
	
	var Diary = {};
	_.extend(Diary, Application);
	Diary.name = "Diary";
	Diary.initialize = initialize;
	return Diary;
	
})