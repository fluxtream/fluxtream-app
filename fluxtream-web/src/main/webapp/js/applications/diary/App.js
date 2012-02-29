define(["core/Application", "core/FlxState"], function(Application, FlxState) {
	
	function initialize() {
		FlxState.router.navigate("/app/diary");
	}
	
	var Diary = {};
	_.extend(Diary, Application);
	Diary.name = "Diary";
	Diary.initialize = initialize;
	return Diary;
	
})