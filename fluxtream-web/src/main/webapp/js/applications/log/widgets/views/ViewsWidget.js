define(["applications/log/widgets/Widget"], function(Widget) {
	
	function render(digest, timeUnit) {
		require(["text!applications/log/widgets/views/views.html"], function(template) {
			$("#widgets").append(template);
		});
	}
	
	var widget = new Widget("clock", "Candide Kemmler", "icon-eye-open");
	widget.render = render;
	return widget;
	
});
