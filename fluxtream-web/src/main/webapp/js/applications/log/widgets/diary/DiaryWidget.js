define(["applications/log/widgets/diary/Status"], function(status) {
	
	function render(digest, timeUnit) {
		require(["text!applications/log/widgets/diary/" + timeUnit.toLowerCase() + "Diary.html"], function(template) {
			$("#widgets").append(template);
			status.handleComments();
		});
	}
	
	var diaryWidget = {};
	diaryWidget.render = render;
	return diaryWidget;
	
});
