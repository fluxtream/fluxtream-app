define(["core/Application"], function(Application) {

	function initialize() {
		
	}
	
	var Diary = {};
	_.extend(Diary, Application);
	Diary.name = "Diary";
	Diary.initialize = initialize;
	return Diary;
	
})