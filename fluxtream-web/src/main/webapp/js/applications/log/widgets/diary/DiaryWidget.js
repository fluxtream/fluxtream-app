define(["applications/log/widgets/diary/Status"], function(status) {
	
	function render(digest) {
		require(["text!applications/log/widgets/diary/diary.html"], function(template) {
			$("#widgets").append(template);
			status.handleComments();
		});
	}
	
	var diaryWidget = {};
	diaryWidget.render = render;
	return diaryWidget;
	
});
