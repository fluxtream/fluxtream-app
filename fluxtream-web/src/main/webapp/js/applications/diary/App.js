define(["core/Application"], function(Application) {
	
	var Diary = {};
	_.extend(Diary, Application);
	Diary.name = "Diary";
	Diary.gotoDate = gotoDate;
	Diary.initialize = initialize;
	return Diary;
	
})