define(["core/Application", "core/FlxState"], function(Application, FlxState) {
	
	var Diary = new Application("diary", "Candide Kemmler", "icon-pencil");
	
	Diary.initialize = function() {
		_.bindAll(this);
		FlxState.router.route("app/diary", "", function() { Diary.render(""); });
	}
	
	Diary.renderState = function(state) {
		FlxState.router.navigate("app/diary");
	}
	return Diary;
	
})