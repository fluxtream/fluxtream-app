define(["applications/log/widgets/Widget"], function(Widget) {
	
	function render(digest, timeUnit) {
		require(["text!applications/log/widgets/list/list.html"], function(template) {
			$("#widgets").append(template);
		});
	}
	
	var widget = new Widget("clock", "Candide Kemmler", "icon-list");
	widget.render = render;
	return widget;
	
});
