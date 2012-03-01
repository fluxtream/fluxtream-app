define(["core/Application", "core/FlxState"], function(Application, FlxState) {
	
	function initialize() {
		FlxState.router.route("app/diary", "", function() { console.log("hahahahaa"); App.renderApp("diary"); });
		console.log("initializing diary app");
	}
	
	function render() {
		FlxState.router.navigate("app/diary");
	}
	
	var Diary = new Application("diary", "Candide Kemmler", "icon-pencil");
	Diary.initialize = initialize;
	Diary.render = render;
	return Diary;
	
})