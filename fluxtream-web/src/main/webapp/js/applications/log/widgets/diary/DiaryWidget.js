define(["applications/log/widgets/diary/Status"], function(status) {
	
	function render(digest) {
		status.handleComments();
	}
	
	var diaryWidget = {};
	diaryWidget.render = render;
	return diaryWidget;
	
});
